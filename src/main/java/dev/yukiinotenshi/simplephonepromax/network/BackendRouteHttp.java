package dev.yukiinotenshi.simplephonepromax.network;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.URLConnection;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SNIHostName;

/** Routes requests to the configured HTTPS endpoint while keeping the URL host as TLS SNI and HTTP Host. */
public final class BackendRouteHttp {
    public static final String DEFAULT_PRIMARY = "yukiinotenshi.dynv6.net:443";
    public static final String DEFAULT_BACKUP = "YukiiNoTenshi-40940.portmap.host:40940";
    public static final String TLS_SERVER_NAME = "yukiinotenshi.dynv6.net";
    private static final long PRIMARY_RETRY_MILLIS = 60_000L;
    private static final int MAX_BUFFERED_REQUEST_BYTES = 16 * 1024 * 1024;
    private static final int MAX_BUFFERED_RESPONSE_BYTES = 8 * 1024 * 1024;
    private static final ExecutorService IO = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "fox-mobile-backend-http");
        thread.setDaemon(true);
        return thread;
    });
    private static long nextPrimaryProbeAt;
    private static Route rememberedRoute = Route.PRIMARY;

    public enum Route { PRIMARY, BACKUP, DIRECT, LOCAL }

    public static final class Settings {
        public boolean autoFallback = true;
        public String forcedRoute = "AUTO";
        public String primaryEndpoint = DEFAULT_PRIMARY;
        public String fallbackEndpoint = DEFAULT_BACKUP;
        public String tlsServerName = TLS_SERVER_NAME;
        public int connectTimeoutSeconds = 5;
        public int requestTimeoutSeconds = 8;
    }

    public static final class Headers {
        private final Map<String, List<String>> values;
        Headers(Map<String, List<String>> source) {
            Map<String, List<String>> copy = new LinkedHashMap<>();
            if (source != null) source.forEach((key, value) -> {
                if (key != null && value != null) copy.put(key, List.copyOf(value));
            });
            values = Collections.unmodifiableMap(copy);
        }
        public Optional<String> firstValue(String name) {
            return values.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .flatMap(entry -> entry.getValue().stream()).findFirst();
        }
        public OptionalLongValue firstValueAsLong(String name) {
            Optional<String> value = firstValue(name);
            if (value.isEmpty()) return OptionalLongValue.empty();
            try { return OptionalLongValue.of(Long.parseLong(value.get())); }
            catch (NumberFormatException ignored) { return OptionalLongValue.empty(); }
        }
    }

    public record OptionalLongValue(boolean present, long value) {
        public static OptionalLongValue empty() { return new OptionalLongValue(false, 0); }
        public static OptionalLongValue of(long value) { return new OptionalLongValue(true, value); }
        public long orElse(long fallback) { return present ? value : fallback; }
    }

    public record TextResponse(int statusCode, Headers headers, String body, Route route) { }

    public static final class StreamResponse implements AutoCloseable {
        private final int statusCode;
        private final Headers headers;
        private final InputStream body;
        private final Route route;
        private StreamResponse(int statusCode, Headers headers, InputStream body, Route route) {
            this.statusCode = statusCode; this.headers = headers; this.body = body; this.route = route;
        }
        public int statusCode() { return statusCode; }
        public Headers headers() { return headers; }
        public InputStream body() { return body; }
        public Route route() { return route; }
        @Override public void close() throws IOException { body.close(); }
    }

    private BackendRouteHttp() { }

    public static CompletableFuture<TextResponse> sendAsync(
            HttpRequest request, Settings settings, SSLContext sslContext, boolean followRedirects) {
        return CompletableFuture.supplyAsync(() -> {
            try { return sendBuffered(request, settings, sslContext, followRedirects); }
            catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
        }, IO);
    }

    public static StreamResponse sendStreaming(
            HttpRequest request, Settings settings, SSLContext sslContext, boolean followRedirects) throws IOException {
        try {
            return execute(request, settings, sslContext, followRedirects, null, true);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTPS request interrupted", interrupted);
        }
    }

    private static TextResponse sendBuffered(
            HttpRequest request, Settings settings, SSLContext sslContext, boolean followRedirects) throws IOException {
        try {
            byte[] requestBody = collectBody(request, requestTimeoutMillis(request, settings));
            StreamResponse response = execute(request, settings, sslContext, followRedirects, requestBody, false);
            try (response) {
                byte[] bytes = response.body().readNBytes(MAX_BUFFERED_RESPONSE_BYTES + 1);
                if (bytes.length > MAX_BUFFERED_RESPONSE_BYTES) throw new IOException("HTTPS response exceeds 8 MiB limit");
                return new TextResponse(response.statusCode(), response.headers(), new String(bytes, StandardCharsets.UTF_8), response.route());
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTPS request interrupted", interrupted);
        }
    }

    private static StreamResponse execute(
            HttpRequest request, Settings settings, SSLContext sslContext, boolean followRedirects,
            byte[] bufferedBody, boolean streamPublisher) throws IOException, InterruptedException {
        if (request == null || request.uri() == null) throw new IOException("Missing HTTP request");
        Settings safe = settings == null ? new Settings() : settings;
        URI uri = request.uri();
        boolean routeEligible = eligible(uri, safe);
        boolean bodyPresent = request.bodyPublisher().isPresent();
        String method = request.method().toUpperCase(Locale.ROOT);
        boolean safeRead = method.equals("GET") && !bodyPresent;
        int timeoutMs = requestTimeoutMillis(request, safe);
        int connectTimeoutMs = Math.min(timeoutMs, clamp(safe.connectTimeoutSeconds, 1, 120) * 1000);
        Route route = routeEligible ? chooseRoute(safe) : Route.DIRECT;
        boolean triedBackup = route == Route.BACKUP;

        while (true) {
            HttpURLConnection connection = null;
            boolean responseReceived = false;
            boolean bodyStarted = false;
            boolean requestMayHaveBeenSent = false;
            int responseStatus = -1;
            try {
                URLConnection raw = uri.toURL().openConnection();
                if (!(raw instanceof HttpURLConnection http)) throw new IOException("Unsupported URL protocol");
                connection = http;
                connection.setConnectTimeout(connectTimeoutMs);
                connection.setReadTimeout(timeoutMs);
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(followRedirects);
                connection.setRequestMethod(method);
                connection.setRequestProperty("Connection", "close");
                for (Map.Entry<String, List<String>> header : request.headers().map().entrySet()) {
                    String name = header.getKey();
                    if (name.equalsIgnoreCase("content-length") || name.equalsIgnoreCase("connection")) continue;
                    for (String value : header.getValue()) connection.addRequestProperty(name, value);
                }

                if (connection instanceof HttpsURLConnection https) {
                    SSLContext trust = sslContext;
                    if (routeEligible) {
                        Endpoint physical = endpointFor(route, uri, safe);
                        SSLSocketFactory baseFactory = trust == null
                                ? (SSLSocketFactory) SSLSocketFactory.getDefault()
                                : trust.getSocketFactory();
                        https.setSSLSocketFactory(new RoutedSocketFactory(baseFactory, physical,
                                safe.tlsServerName == null ? TLS_SERVER_NAME : safe.tlsServerName,
                                connectTimeoutMs, timeoutMs));
                    } else if (trust != null) {
                        https.setSSLSocketFactory(trust.getSocketFactory());
                    }
                }

                long contentLength = request.bodyPublisher().map(HttpRequest.BodyPublisher::contentLength).orElse(0L);
                if (bodyPresent) {
                    connection.setDoOutput(true);
                    if (contentLength >= 0) connection.setFixedLengthStreamingMode(contentLength);
                    else connection.setChunkedStreamingMode(64 * 1024);
                }

                try {
                    connection.connect();
                } catch (IOException failure) {
                    if (canRetryOnBackup(route, triedBackup, routeEligible, safeRead, bodyStarted,
                            requestMayHaveBeenSent, safe, failure)) {
                        close(connection);
                        route = Route.BACKUP;
                        triedBackup = true;
                        rememberFailure(Route.PRIMARY, safe);
                        continue;
                    }
                    throw failure;
                }

                if (bodyPresent) {
                    OutputStream output;
                    requestMayHaveBeenSent = true;
                    try {
                        output = connection.getOutputStream();
                    } catch (IOException failureBeforeWrite) {
                        if (canRetryOnBackup(route, triedBackup, routeEligible, safeRead, false,
                                requestMayHaveBeenSent, safe, failureBeforeWrite)) {
                            close(connection);
                            route = Route.BACKUP;
                            triedBackup = true;
                            rememberFailure(Route.PRIMARY, safe);
                            continue;
                        }
                        throw failureBeforeWrite;
                    }
                    bodyStarted = true;
                    try (output) {
                        if (streamPublisher) writePublisher(request, output, timeoutMs);
                        else if (bufferedBody != null && bufferedBody.length > 0) output.write(bufferedBody);
                    }
                }

                requestMayHaveBeenSent = true;
                int status = connection.getResponseCode();
                responseStatus = status;
                responseReceived = true;
                rememberSuccess(route, safe);
                InputStream body = responseStream(connection, status);
                boolean canFailoverWhileReading = safeRead && status >= 200 && status < 300;
                InputStream tracked = new TrackedInputStream(body, connection, route, safe, canFailoverWhileReading);
                if (!streamPublisher && bufferedBody == null) {
                    byte[] bytes;
                    try (tracked) { bytes = tracked.readNBytes(MAX_BUFFERED_RESPONSE_BYTES + 1); }
                    if (bytes.length > MAX_BUFFERED_RESPONSE_BYTES) throw new IOException("HTTPS response exceeds 8 MiB limit");
                    return new StreamResponse(status, headers(connection), new java.io.ByteArrayInputStream(bytes), route);
                }
                return new StreamResponse(status, headers(connection), tracked, route);
            } catch (IOException failure) {
                boolean safeResponseReadFailure = safeRead && responseReceived && responseStatus >= 200 && responseStatus < 300;
                if (connection != null && canRetryOnBackup(route, triedBackup, routeEligible,
                        safeResponseReadFailure, bodyStarted, requestMayHaveBeenSent, safe, failure)) {
                    close(connection);
                    route = Route.BACKUP;
                    triedBackup = true;
                    rememberFailure(Route.PRIMARY, safe);
                    continue;
                }
                close(connection);
                throw failure;
            } finally {
                // A returned response owns the connection through its tracked body stream.
            }
        }
    }

    static byte[] collectBody(HttpRequest request, int timeoutMs) throws IOException, InterruptedException {
        Optional<HttpRequest.BodyPublisher> optional = request.bodyPublisher();
        if (optional.isEmpty()) return null;
        HttpRequest.BodyPublisher publisher = optional.get();
        long declared = publisher.contentLength();
        if (declared > MAX_BUFFERED_REQUEST_BYTES) throw new IOException("Buffered HTTP request exceeds 16 MiB limit");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(declared > 0 ? (int) declared : 256);
        CompletableFuture<byte[]> done = new CompletableFuture<>();
        AtomicReference<Flow.Subscription> activeSubscription = new AtomicReference<>();
        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            private int count;
            @Override public void onSubscribe(Flow.Subscription value) { subscription = value; activeSubscription.set(value); value.request(1); }
            @Override public void onNext(ByteBuffer item) {
                try {
                    int length = item.remaining();
                    if ((long) count + length > MAX_BUFFERED_REQUEST_BYTES) {
                        subscription.cancel();
                        done.completeExceptionally(new IOException("Buffered HTTP request exceeds 16 MiB limit"));
                        return;
                    }
                    byte[] chunk = new byte[length]; item.get(chunk); bytes.write(chunk); count += length;
                    subscription.request(1);
                } catch (Throwable failure) { subscription.cancel(); done.completeExceptionally(failure); }
            }
            @Override public void onError(Throwable failure) { done.completeExceptionally(failure); }
            @Override public void onComplete() { done.complete(bytes.toByteArray()); }
        });
        try { return done.get(Math.max(1000, timeoutMs), TimeUnit.MILLISECONDS); }
        catch (TimeoutException failure) {
            Flow.Subscription subscription = activeSubscription.get(); if (subscription != null) subscription.cancel();
            throw new SocketTimeoutException("Timed out preparing HTTP request body");
        }
        catch (InterruptedException interrupted) { Flow.Subscription subscription = activeSubscription.get(); if (subscription != null) subscription.cancel(); throw interrupted; }
        catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException("Unable to prepare HTTP request body", cause);
        }
    }

    private static void writePublisher(HttpRequest request, OutputStream output, int timeoutMs) throws IOException {
        HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElse(null);
        if (publisher == null) return;
        CompletableFuture<Void> done = new CompletableFuture<>();
        AtomicReference<Flow.Subscription> activeSubscription = new AtomicReference<>();
        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            @Override public void onSubscribe(Flow.Subscription value) { subscription = value; activeSubscription.set(value); value.request(1); }
            @Override public void onNext(ByteBuffer item) {
                try {
                    byte[] chunk = new byte[item.remaining()]; item.get(chunk); output.write(chunk); output.flush(); subscription.request(1);
                } catch (Throwable failure) { subscription.cancel(); done.completeExceptionally(failure); }
            }
            @Override public void onError(Throwable failure) { done.completeExceptionally(failure); }
            @Override public void onComplete() { done.complete(null); }
        });
        try { done.get(Math.max(1000, timeoutMs), TimeUnit.MILLISECONDS); }
        catch (InterruptedException interrupted) { Flow.Subscription subscription = activeSubscription.get(); if (subscription != null) subscription.cancel(); Thread.currentThread().interrupt(); throw new IOException("HTTP body write interrupted", interrupted); }
        catch (TimeoutException failure) { Flow.Subscription subscription = activeSubscription.get(); if (subscription != null) subscription.cancel(); throw new SocketTimeoutException("Timed out writing HTTP request body"); }
        catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException("Unable to write HTTP request body", cause);
        }
    }

    private static InputStream responseStream(HttpURLConnection connection, int status) throws IOException {
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return stream == null ? InputStream.nullInputStream() : stream;
    }

    private static Headers headers(HttpURLConnection connection) { return new Headers(connection.getHeaderFields()); }

    private static boolean eligible(URI uri, Settings settings) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
        String sni = settings.tlsServerName == null || settings.tlsServerName.isBlank() ? TLS_SERVER_NAME : settings.tlsServerName;
        return sni.equalsIgnoreCase(uri.getHost());
    }

    private static Route chooseRoute(Settings settings) {
        String forced = settings.forcedRoute == null ? "AUTO" : settings.forcedRoute.toUpperCase(Locale.ROOT);
        if (forced.equals("PRIMARY")) return Route.PRIMARY;
        if (forced.equals("BACKUP")) return Route.BACKUP;
        if (!settings.autoFallback) return Route.PRIMARY;
        synchronized (BackendRouteHttp.class) {
            if (rememberedRoute == Route.BACKUP && System.currentTimeMillis() < nextPrimaryProbeAt) return Route.BACKUP;
            rememberedRoute = Route.PRIMARY;
            return Route.PRIMARY;
        }
    }

    private static boolean canRetryOnBackup(Route route, boolean triedBackup, boolean eligible, boolean safeRead,
                                             boolean bodyStarted, boolean requestMayHaveBeenSent,
                                             Settings settings, IOException failure) {
        String forced = settings.forcedRoute == null ? "AUTO" : settings.forcedRoute.toUpperCase(Locale.ROOT);
        return route == Route.PRIMARY && !triedBackup && eligible && settings.autoFallback && forced.equals("AUTO")
                && !bodyStarted && isNetworkFailure(failure) && (safeRead || !requestMayHaveBeenSent)
                && endpointOrNull(settings.fallbackEndpoint) != null;
    }

    private static boolean isNetworkFailure(Throwable failure) {
        boolean network = false;
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof CertificateException || current instanceof SSLPeerUnverifiedException) return false;
            if (current instanceof SSLHandshakeException || current instanceof SSLException) {
                boolean transportFailure = false;
                for (Throwable nested = current; nested != null; nested = nested.getCause()) {
                    if (nested instanceof SocketTimeoutException || nested instanceof SocketException
                            || nested instanceof EOFException || nested instanceof UnknownHostException
                            || nested instanceof NoRouteToHostException) transportFailure = true;
                    if (nested instanceof CertificateException || nested instanceof SSLPeerUnverifiedException) return false;
                }
                if (!transportFailure) return false;
            }
            if (current instanceof UnknownHostException || current instanceof ConnectException
                    || current instanceof NoRouteToHostException || current instanceof SocketTimeoutException
                    || current instanceof SocketException || current instanceof EOFException) network = true;
        }
        return network;
    }

    private static Endpoint endpointFor(Route route, URI uri, Settings settings) throws IOException {
        String configured = route == Route.BACKUP ? settings.fallbackEndpoint : settings.primaryEndpoint;
        Endpoint endpoint = endpointOrNull(configured);
        if (endpoint != null) return endpoint;
        if (route == Route.BACKUP) throw new IOException("Invalid backup HTTPS endpoint");
        return new Endpoint(uri.getHost(), uri.getPort() < 0 ? 443 : uri.getPort());
    }

    public static boolean validEndpoint(String value) { return endpointOrNull(value) != null; }

    private static Endpoint endpointOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI parsed = URI.create("tcp://" + value.trim());
            String host = parsed.getHost();
            int port = parsed.getPort();
            if (host == null || port < 1 || port > 65535 || parsed.getRawUserInfo() != null
                    || (parsed.getRawPath() != null && !parsed.getRawPath().isEmpty())
                    || parsed.getRawQuery() != null || parsed.getRawFragment() != null) return null;
            if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length() - 1);
            return new Endpoint(host, port);
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private static int requestTimeoutMillis(HttpRequest request, Settings settings) {
        Duration configured = request == null ? null : request.timeout().orElse(null);
        long millis = configured == null || configured.toSeconds() <= 15
                ? clamp(settings == null ? 8 : settings.requestTimeoutSeconds, 1, 600) * 1000L
                : configured.toMillis();
        return (int) Math.max(1000, Math.min(Integer.MAX_VALUE, millis));
    }

    private static void rememberSuccess(Route route, Settings settings) {
        if (route == Route.DIRECT || !"AUTO".equalsIgnoreCase(settings.forcedRoute == null ? "AUTO" : settings.forcedRoute)) return;
        synchronized (BackendRouteHttp.class) {
            if (route == Route.PRIMARY) { rememberedRoute = Route.PRIMARY; nextPrimaryProbeAt = 0; }
            else if (settings.autoFallback) { rememberedRoute = Route.BACKUP; nextPrimaryProbeAt = System.currentTimeMillis() + PRIMARY_RETRY_MILLIS; }
        }
    }

    private static void rememberFailure(Route route, Settings settings) {
        if (route != Route.PRIMARY || !settings.autoFallback || !"AUTO".equalsIgnoreCase(settings.forcedRoute == null ? "AUTO" : settings.forcedRoute)) return;
        synchronized (BackendRouteHttp.class) {
            rememberedRoute = Route.BACKUP;
            nextPrimaryProbeAt = System.currentTimeMillis() + PRIMARY_RETRY_MILLIS;
        }
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static void close(HttpURLConnection connection) { if (connection != null) connection.disconnect(); }

    private record Endpoint(String host, int port) { }

    private static final class TrackedInputStream extends FilterInputStream {
        private final HttpURLConnection connection;
        private final Route route;
        private final Settings settings;
        private final boolean canFailover;
        TrackedInputStream(InputStream input, HttpURLConnection connection, Route route, Settings settings, boolean canFailover) {
            super(input); this.connection = connection; this.route = route; this.settings = settings; this.canFailover = canFailover;
        }
        @Override public int read() throws IOException {
            try { return super.read(); }
            catch (IOException failure) { if (canFailover && isNetworkFailure(failure)) rememberFailure(route, settings); throw failure; }
        }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            try { return super.read(bytes, offset, length); }
            catch (IOException failure) { if (canFailover && isNetworkFailure(failure)) rememberFailure(route, settings); throw failure; }
        }
        @Override public void close() throws IOException { try { super.close(); } finally { connection.disconnect(); } }
    }

    private static final class RoutedSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        private final Endpoint target;
        private final String tlsServerName;
        private final int connectTimeoutMs, readTimeoutMs;
        RoutedSocketFactory(SSLSocketFactory delegate, Endpoint target, String tlsServerName, int connectTimeoutMs, int readTimeoutMs) {
            this.delegate = delegate; this.target = target; this.tlsServerName = tlsServerName;
            this.connectTimeoutMs = connectTimeoutMs; this.readTimeoutMs = readTimeoutMs;
        }
        private Socket create(String logicalHost, int logicalPort, InetAddress localAddress, int localPort) throws IOException {
            boolean routed = logicalHost.equalsIgnoreCase(tlsServerName) && logicalPort == 443;
            String physicalHost = routed ? target.host : logicalHost;
            int physicalPort = routed ? target.port : logicalPort;
            Socket raw = new Socket();
            try {
                if (localAddress != null) raw.bind(new InetSocketAddress(localAddress, localPort));
                raw.connect(new InetSocketAddress(physicalHost, physicalPort), connectTimeoutMs);
                raw.setSoTimeout(readTimeoutMs);
                SSLSocket ssl = (SSLSocket) delegate.createSocket(raw, logicalHost, logicalPort, true);
                ssl.setSoTimeout(readTimeoutMs);
                SSLParameters parameters = ssl.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                if (logicalHost.equalsIgnoreCase(tlsServerName) && !isIpAddress(logicalHost)) {
                    parameters.setServerNames(List.of(new SNIHostName(logicalHost)));
                }
                ssl.setSSLParameters(parameters);
                return ssl;
            } catch (IOException | RuntimeException failure) {
                try { raw.close(); } catch (IOException ignored) { }
                throw failure;
            }
        }
        private static boolean isIpAddress(String host) {
            return host.indexOf(':') >= 0 || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
        }
        @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
        @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }
        @Override public Socket createSocket(String host, int port) throws IOException { return create(host, port, null, 0); }
        @Override public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException { return create(host, port, localHost, localPort); }
        @Override public Socket createSocket(InetAddress host, int port) throws IOException { return create(host.getHostName(), port, null, 0); }
        @Override public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException { return create(address.getHostName(), port, localAddress, localPort); }
        @Override public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            if (!socket.isConnected()) {
                Endpoint physical = host.equalsIgnoreCase(tlsServerName) && port == 443 ? target : new Endpoint(host, port);
                socket.connect(new InetSocketAddress(physical.host, physical.port), connectTimeoutMs);
            } else if (host.equalsIgnoreCase(tlsServerName) && port == 443
                    && (!samePhysical(socket, target))) {
                throw new IOException("Refusing to wrap a TCP socket connected to a different route");
            }
            SSLSocket ssl = (SSLSocket) delegate.createSocket(socket, host, port, autoClose);
            ssl.setSoTimeout(readTimeoutMs);
            SSLParameters parameters = ssl.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            if (host.equalsIgnoreCase(tlsServerName) && !isIpAddress(host)) parameters.setServerNames(List.of(new SNIHostName(host)));
            ssl.setSSLParameters(parameters);
            return ssl;
        }
        private static boolean samePhysical(Socket socket, Endpoint endpoint) throws IOException {
            if (socket instanceof DeferredRouteSocket routed) return routed.connectedTo(endpoint);
            if (socket.getPort() != endpoint.port) return false;
            for (InetAddress address : InetAddress.getAllByName(endpoint.host)) {
                if (address.equals(socket.getInetAddress())) return true;
            }
            return false;
        }
        @Override public Socket createSocket(Socket socket, InputStream consumed, boolean autoClose) throws IOException {
            return delegate.createSocket(socket, consumed, autoClose);
        }
        /**
         * HttpsURLConnection first asks its socket factory for an unconnected socket, then connects
         * that socket to the URL host, and only afterwards asks the factory to layer TLS over it.
         * Remap that connect operation while retaining the URL host for the layered TLS socket.
         */
        @Override public Socket createSocket() { return new DeferredRouteSocket(target, tlsServerName, connectTimeoutMs); }

        private static final class DeferredRouteSocket extends Socket {
            private final Socket socket = new Socket();
            private final Endpoint target;
            private final String tlsServerName;
            private final int defaultConnectTimeoutMs;
            private String connectedTargetHost;
            private int connectedTargetPort;

            private DeferredRouteSocket(Endpoint target, String tlsServerName, int defaultConnectTimeoutMs) {
                this.target = target;
                this.tlsServerName = tlsServerName;
                this.defaultConnectTimeoutMs = defaultConnectTimeoutMs;
            }

            @Override public void connect(java.net.SocketAddress endpoint) throws IOException {
                connect(endpoint, defaultConnectTimeoutMs);
            }

            @Override public void connect(java.net.SocketAddress endpoint, int timeout) throws IOException {
                if (!(endpoint instanceof InetSocketAddress logical)) throw new IOException("HTTPS route requires an internet socket address");
                String logicalHost = logical.getHostString();
                boolean routed = logical.getPort() == 443 && logicalHost.equalsIgnoreCase(tlsServerName);
                java.net.SocketAddress physical = routed
                        ? new InetSocketAddress(target.host, target.port)
                        : logical;
                socket.connect(physical, timeout > 0 ? timeout : defaultConnectTimeoutMs);
                connectedTargetHost = routed ? target.host : logicalHost;
                connectedTargetPort = routed ? target.port : logical.getPort();
            }

            private boolean connectedTo(Endpoint endpoint) {
                return socket.isConnected() && connectedTargetHost != null
                        && connectedTargetPort == endpoint.port
                        && connectedTargetHost.equalsIgnoreCase(endpoint.host);
            }

            @Override public void bind(java.net.SocketAddress bindpoint) throws IOException { socket.bind(bindpoint); }
            @Override public java.net.InetAddress getInetAddress() { return socket.getInetAddress(); }
            @Override public java.net.InetAddress getLocalAddress() { return socket.getLocalAddress(); }
            @Override public int getPort() { return socket.getPort(); }
            @Override public int getLocalPort() { return socket.getLocalPort(); }
            @Override public java.net.SocketAddress getRemoteSocketAddress() { return socket.getRemoteSocketAddress(); }
            @Override public java.net.SocketAddress getLocalSocketAddress() { return socket.getLocalSocketAddress(); }
            @Override public java.nio.channels.SocketChannel getChannel() { return socket.getChannel(); }
            @Override public java.io.InputStream getInputStream() throws IOException { return socket.getInputStream(); }
            @Override public java.io.OutputStream getOutputStream() throws IOException { return socket.getOutputStream(); }
            @Override public void setTcpNoDelay(boolean on) throws SocketException { socket.setTcpNoDelay(on); }
            @Override public boolean getTcpNoDelay() throws SocketException { return socket.getTcpNoDelay(); }
            @Override public void setSoLinger(boolean on, int linger) throws SocketException { socket.setSoLinger(on, linger); }
            @Override public int getSoLinger() throws SocketException { return socket.getSoLinger(); }
            @Override public void sendUrgentData(int data) throws IOException { socket.sendUrgentData(data); }
            @Override public void setOOBInline(boolean on) throws SocketException { socket.setOOBInline(on); }
            @Override public boolean getOOBInline() throws SocketException { return socket.getOOBInline(); }
            @Override public synchronized void setSoTimeout(int timeout) throws SocketException { socket.setSoTimeout(timeout); }
            @Override public synchronized int getSoTimeout() throws SocketException { return socket.getSoTimeout(); }
            @Override public void setSendBufferSize(int size) throws SocketException { socket.setSendBufferSize(size); }
            @Override public int getSendBufferSize() throws SocketException { return socket.getSendBufferSize(); }
            @Override public void setReceiveBufferSize(int size) throws SocketException { socket.setReceiveBufferSize(size); }
            @Override public int getReceiveBufferSize() throws SocketException { return socket.getReceiveBufferSize(); }
            @Override public void setKeepAlive(boolean on) throws SocketException { socket.setKeepAlive(on); }
            @Override public boolean getKeepAlive() throws SocketException { return socket.getKeepAlive(); }
            @Override public void setTrafficClass(int tc) throws SocketException { socket.setTrafficClass(tc); }
            @Override public int getTrafficClass() throws SocketException { return socket.getTrafficClass(); }
            @Override public void setReuseAddress(boolean on) throws SocketException { socket.setReuseAddress(on); }
            @Override public boolean getReuseAddress() throws SocketException { return socket.getReuseAddress(); }
            @Override public synchronized void close() throws IOException { socket.close(); }
            @Override public void shutdownInput() throws IOException { socket.shutdownInput(); }
            @Override public void shutdownOutput() throws IOException { socket.shutdownOutput(); }
            @Override public String toString() { return "DeferredRouteSocket[" + socket + "]"; }
            @Override public boolean isConnected() { return socket.isConnected(); }
            @Override public boolean isBound() { return socket.isBound(); }
            @Override public boolean isClosed() { return socket.isClosed(); }
            @Override public boolean isInputShutdown() { return socket.isInputShutdown(); }
            @Override public boolean isOutputShutdown() { return socket.isOutputShutdown(); }
            @SuppressWarnings("removal")
            @Override public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
                socket.setPerformancePreferences(connectionTime, latency, bandwidth);
            }
        }
    }
}
