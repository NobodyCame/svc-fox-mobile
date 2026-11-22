package dev.yukiinotenshi.foxoperator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class LocalOperator implements AutoCloseable {
    private static final Gson JSON = new Gson();
    private static final String[] MODULES = {"operator_stdio", "simple_phone_backend", "call_signalling",
            "callback_requests", "organization_lines", "private_calls", "secure_relay", "voicemail"};
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64), task -> {
                Thread thread = new Thread(task, "Fox Mobile operator");
                thread.setDaemon(true);
                return thread;
            });
    private final Map<Object, OperatorWire.Incoming> incoming = new HashMap<>();
    private final Map<Object, Long> pending = new HashMap<>();
    private final Path directory;
    private final Path runtimeCache;
    private final String python;
    private final Consumer<String> reportError;
    private volatile Process process;
    private BufferedReader stdout;
    private BufferedWriter stdin;
    private volatile boolean closed;
    private boolean ready;

    public LocalOperator(Path directory, Path runtimeCache, String python, Consumer<String> reportError) {
        this.directory = directory.toAbsolutePath();
        this.runtimeCache = runtimeCache.toAbsolutePath();
        this.python = python;
        this.reportError = reportError;
        worker.execute(() -> {
            try {
                start();
            } catch (Exception e) {
                reportError.accept("Fox Mobile operator could not start: " + e.getMessage());
                stopProcess();
            }
        });
    }

    private void start() throws IOException {
        Files.createDirectories(directory);
        Path code = Files.createDirectories(directory.resolve("runtime"));
        for (String module : MODULES) {
            try (var resource = LocalOperator.class.getResourceAsStream("/fox-operator/" + module + ".py")) {
                if (resource == null) throw new IOException("missing module " + module);
                Files.copy(resource, code.resolve(module + ".py"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        String executable = OperatorRuntime.executable(runtimeCache, python);
        Process child;
        synchronized (this) {
            if (closed) return;
            var builder = new ProcessBuilder(executable, "-E", "-s", "-X", "utf8", "-u", code.resolve("operator_stdio.py").toString())
                    .directory(directory.toFile())
                    .redirectError(ProcessBuilder.Redirect.appendTo(directory.resolve("operator-errors.log").toFile()));
            builder.environment().keySet().removeIf(key -> key.startsWith("SIMPLE_PHONE_"));
            child = builder.start();
            process = child;
        }
        stdout = new BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8));
        stdin = new BufferedWriter(new OutputStreamWriter(child.getOutputStream(), StandardCharsets.UTF_8));
        if (!"FOX_OPERATOR_V1".equals(stdout.readLine())) throw new IOException("see operator-errors.log");
        ready = true;
    }

    // Called on the server thread. The callback also runs on that thread.
    public void receive(Object connection, UUID player, String name, byte[] packet,
                        Consumer<Runnable> schedule, Consumer<byte[]> reply) {
        var frame = OperatorWire.Frame.read(packet, OperatorWire.MAX_REQUEST);
        if (closed || frame == null || pending.containsKey(connection)) return;
        if (frame.offset() == 0) incoming.put(connection, new OperatorWire.Incoming(frame));
        var message = incoming.get(connection);
        if (message == null || !message.append(frame)) {
            incoming.remove(connection);
            return;
        }
        byte[] bytes = message.complete();
        if (bytes == null) return;
        incoming.remove(connection);
        pending.put(connection, frame.id());
        try {
            worker.execute(() -> {
                byte[] response = process(player, name, bytes);
                if (!closed) schedule.accept(() -> {
                    if (pending.remove(connection, frame.id())) OperatorWire.send(frame.id(), response, reply);
                });
            });
        } catch (RejectedExecutionException e) {
            pending.remove(connection);
            OperatorWire.send(frame.id(), error(503, "local operator busy"), reply);
        }
    }

    private byte[] process(UUID player, String name, byte[] bytes) {
        JsonObject request;
        try {
            request = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            request.addProperty("player", player.toString());
            request.addProperty("name", name);
        } catch (RuntimeException e) {
            return error(400, "invalid operator request");
        }
        if (!ready || process == null || !process.isAlive()) return error(503, "local operator unavailable");
        try {
            stdin.write(JSON.toJson(request));
            stdin.newLine();
            stdin.flush();
            String line = stdout.readLine();
            if (line == null) throw new IOException("backend exited");
            byte[] response = line.getBytes(StandardCharsets.UTF_8);
            return response.length <= OperatorWire.MAX_RESPONSE ? response : error(503, "operator response too large");
        } catch (IOException e) {
            stopProcess();
            return error(503, "local operator unavailable");
        }
    }

    private static byte[] error(int status, String message) {
        JsonObject body = new JsonObject();
        body.addProperty("error", message);
        JsonObject response = new JsonObject();
        response.addProperty("status", status);
        response.add("body", body);
        return JSON.toJson(response).getBytes(StandardCharsets.UTF_8);
    }

    public void disconnect(Object connection) {
        incoming.remove(connection);
        pending.remove(connection);
    }

    private synchronized void stopProcess() {
        if (process != null) process.destroyForcibly();
    }

    @Override public synchronized void close() {
        closed = true;
        worker.shutdownNow();
        stopProcess();
        incoming.clear();
        pending.clear();
    }
}
