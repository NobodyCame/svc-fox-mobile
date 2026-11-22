package dev.yukiinotenshi.simplephonepromax.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.yukiinotenshi.foxoperator.OperatorWire;
import dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class LocalOperatorClient {
    private static CompletableFuture<Boolean> route;
    private static ClientPacketListener connection;
    private static CompletableFuture<?> tail = CompletableFuture.completedFuture(null);
    private static Pending pending;
    private static long nextId;

    private LocalOperatorClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OperatorPayload.TYPE, (payload, context) -> receive(payload.data()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> select(handler, client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (handler == connection) select(null, client);
        });
    }

    private static void select(ClientPacketListener handler, Minecraft client) {
        var oldPending = pending;
        var oldRoute = route;
        pending = null;
        tail = CompletableFuture.completedFuture(null);
        connection = handler;
        var choice = new CompletableFuture<Boolean>();
        route = choice;
        var disconnected = new IllegalStateException("operator connection changed");
        if (oldRoute != null) oldRoute.completeExceptionally(disconnected);
        if (oldPending != null) oldPending.result.completeExceptionally(disconnected);
        if (handler == null) finishChoice(choice, false);
        else if (ClientPlayNetworking.canSend(OperatorPayload.TYPE)) finishChoice(choice, true);
        else CompletableFuture.delayedExecutor(1200, TimeUnit.MILLISECONDS).execute(() -> client.execute(() -> {
            if (connection == handler && route == choice) finishChoice(choice, ClientPlayNetworking.canSend(OperatorPayload.TYPE));
        }));
    }

    private static void finishChoice(CompletableFuture<Boolean> choice, boolean local) {
        BackendNumberService.useLocalOperator(local);
        choice.complete(local);
    }

    public static CompletableFuture<BackendRouteHttp.TextResponse> send(HttpRequest request,
            Supplier<CompletableFuture<BackendRouteHttp.TextResponse>> external) {
        var result = new CompletableFuture<BackendRouteHttp.TextResponse>();
        var client = Minecraft.getInstance();
        client.execute(() -> {
            var handler = client.getConnection();
            if (connection != handler || route == null) select(handler, client);
            route.thenCompose(local -> local ? queue(request, handler, client) : external.get())
                    .whenComplete((value, failure) -> client.execute(() -> {
                        if (connection != handler) result.completeExceptionally(new IllegalStateException("operator connection changed"));
                        else if (failure != null) result.completeExceptionally(failure);
                        else result.complete(value);
                    }));
        });
        return result;
    }

    private static CompletableFuture<BackendRouteHttp.TextResponse> queue(HttpRequest request,
            ClientPacketListener handler, Minecraft client) {
        var result = tail.handle((value, failure) -> null)
                .thenComposeAsync(ignored -> sendNow(request, handler), client);
        tail = result;
        return result;
    }

    private static CompletableFuture<BackendRouteHttp.TextResponse> sendNow(HttpRequest request,
            ClientPacketListener handler) {
        if (connection != handler || !ClientPlayNetworking.canSend(OperatorPayload.TYPE)) {
            return CompletableFuture.failedFuture(new IllegalStateException("local operator disconnected"));
        }
        try {
            String path = request.uri().getPath();
            var match = java.util.regex.Pattern.compile("(/(?:health|public-numbers|register|resolve/[0-9]{1,15}|calls/[a-z0-9-]{1,48}))$").matcher(path);
            if (!match.find()) throw new IllegalArgumentException("unsupported operator path");
            var message = new JsonObject();
            message.addProperty("path", match.group(1));
            message.addProperty("method", request.method());
            if (request.bodyPublisher().isPresent()) {
                byte[] bytes = BackendRouteHttp.collectBody(request, 1000);
                var body = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : new String[]{"secret", "device_id", "minecraft_uuid", "last_known_name", "server_address", "player", "name", "server"}) body.remove(key);
                message.add("body", body);
            }
            byte[] bytes = message.toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > OperatorWire.MAX_REQUEST) throw new IllegalArgumentException("operator request too large");
            long id = ++nextId;
            var result = new CompletableFuture<BackendRouteHttp.TextResponse>().orTimeout(8, TimeUnit.SECONDS);
            pending = new Pending(id, result);
            OperatorWire.send(id, bytes, data -> ClientPlayNetworking.send(new OperatorPayload(data)));
            return result;
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static void receive(byte[] bytes) {
        var current = pending;
        var frame = OperatorWire.Frame.read(bytes, OperatorWire.MAX_RESPONSE);
        if (current == null || frame == null || frame.id() != current.id) return;
        if (frame.offset() == 0) current.incoming = new OperatorWire.Incoming(frame);
        if (current.incoming == null || !current.incoming.append(frame)) return;
        byte[] response = current.incoming.complete();
        if (response == null) return;
        pending = null;
        try {
            var json = JsonParser.parseString(new String(response, StandardCharsets.UTF_8)).getAsJsonObject();
            current.result.complete(new BackendRouteHttp.TextResponse(json.get("status").getAsInt(),
                    new BackendRouteHttp.Headers(java.util.Map.of()), json.get("body").toString(), BackendRouteHttp.Route.LOCAL));
        } catch (RuntimeException e) {
            current.result.completeExceptionally(e);
        }
    }

    private static final class Pending {
        private final long id;
        private final CompletableFuture<BackendRouteHttp.TextResponse> result;
        private OperatorWire.Incoming incoming;

        private Pending(long id, CompletableFuture<BackendRouteHttp.TextResponse> result) {
            this.id = id;
            this.result = result;
        }
    }
}
