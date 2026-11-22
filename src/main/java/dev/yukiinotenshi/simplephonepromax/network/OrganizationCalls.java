package dev.yukiinotenshi.simplephonepromax.network;

import com.google.gson.JsonObject;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDialer;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

public final class OrganizationCalls {
    private static boolean queued, busy;
    private static boolean shiftRequestPending;
    private static long nextShiftChangeAt;
    private static final ArrayDeque<Long> shiftRequests = new ArrayDeque<>();
    private static String status = "", context = "", number = "";
    private static UUID currentTarget;
    private static UUID blockedTarget;
    private static IncomingLine incomingLine;
    private static long incomingLookup;

    private record IncomingLine(UUID caller, String server, UUID group, String number, String name) {}

    public static Component incomingLineLabel() {
        var line = incomingLine;
        if (line == null || !line.server.equals(ServerProfiles.current())
                || !line.caller.equals(SimpleVoiceCallClient.callManager.getOtherPlayerUuid())
                || !line.group.equals(VoiceCallTransport.getIncomingGroup())) return null;
        return Component.translatable("phone.organization.incoming_line", line.number, line.name);
    }

    static void findIncomingLine(UUID caller, UUID group) {
        long lookup = ++incomingLookup;
        incomingLine = null;
        if (group == null || !CallControlService.available()) return;
        String server = ServerProfiles.current();
        JsonObject body = new JsonObject();
        body.addProperty("caller", caller.toString());
        var client = Minecraft.getInstance();
        CallControlService.extension("org-incoming", body).whenComplete((value, error) -> client.execute(() -> {
            if (error != null || lookup != incomingLookup || !server.equals(ServerProfiles.current())
                    || !group.equals(VoiceCallTransport.getIncomingGroup())
                    || !caller.equals(SimpleVoiceCallClient.callManager.getOtherPlayerUuid())
                    || !value.has("number") || !value.has("name")) return;
            incomingLine = new IncomingLine(caller, server, group, value.get("number").getAsString(),
                    value.get("name").getAsString());
        }));
    }
    private static long next, generation;

    public static String status() { return status; }
    public static boolean queued() { return queued; }

    public static CompletableFuture<Boolean> tryNumber(String number) {
        if (!CallControlService.available() || !number.matches("[0-9]{1,3}") ||
                dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()) {
            return CompletableFuture.completedFuture(false);
        }
        JsonObject body = new JsonObject();
        body.addProperty("number", number);
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        long epoch = generation;
        CallControlService.extension("org-join", body).whenComplete((value, error) ->
                Minecraft.getInstance().execute(() -> {
                    if (epoch != generation) { result.complete(true); return; }
                    if (error != null) { result.completeExceptionally(error); return; }
                    boolean handled = value.has("handled") && value.get("handled").getAsBoolean();
                    if (handled) {
                        queued = true;
                        OrganizationCalls.number = number;
                        context = dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current();
                        apply(value);
                    }
                    result.complete(handled);
                }));
        return result;
    }

    private static void apply(com.google.gson.JsonObject value) {
        if (!value.has("handled") || !value.get("handled").getAsBoolean()) {
            queued = false;
            busy = false;
            blockedTarget = null;
            status = "Связь с очередью потеряна";
            return;
        }
        status = "Очередь " + value.get("name").getAsString() + " · место " +
                (value.has("position") ? value.get("position").getAsInt() : 1);
        if (value.has("target") && !value.get("target").isJsonNull()) {
            UUID target = UUID.fromString(value.get("target").getAsString());
            if (value.has("accepted") && value.get("accepted").getAsBoolean()) {
                status = "Оператор ответил";
                queued = false;
                currentTarget = null;
                blockedTarget = null;
                number = "";
                generation++;
                if (Minecraft.getInstance().player != null && CallControlService.available())
                    CallControlService.extension("org-cancel", new JsonObject());
                return;
            }
            if (target.equals(currentTarget)) return;
            var client = Minecraft.getInstance();
            if (currentTarget != null && SimpleVoiceCallClient.callManager.getOtherPlayerUuid() != null &&
                    currentTarget.equals(SimpleVoiceCallClient.callManager.getOtherPlayerUuid())) {
                UUID previous = currentTarget;
                SimpleVoiceCallClient.callManager.endCall();
                if (client.player != null) ModNetworking.sendCallHangup(client.player.getUUID(), previous);
            }
            currentTarget = null;
            var state = VoiceCallTransport.findState(target);
            var info = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(target);
            String voiceName = state == null ? null : state.getName();
            String playerName = info == null ? null : info.getProfile().name();
            var config = SimpleVoiceCallClient.config;
            if (config.isBlocked(target) || config.isBlocked(voiceName) || config.isBlocked(playerName)) {
                blockedTarget = target;
                status = "Пропускаем заблокированного оператора";
                return;
            }
            blockedTarget = null;
            currentTarget = target;
            String callerLabel = voiceName != null && !voiceName.isBlank() ? voiceName : playerName;
            if (PhoneDialer.call(target, callerLabel == null ? target.toString() : callerLabel)) {
                status = "Соединяем с оператором · " + value.get("name").getAsString();
                Minecraft.getInstance().setScreenAndShow(new dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen());
            } else {
                status = "Вызываем оператора · если он не ответит, вызов перейдёт дальше";
            }
        } else {
            if (currentTarget != null && SimpleVoiceCallClient.callManager.getState() !=
                    dev.yukiinotenshi.simplephonepromax.call.CallState.ACTIVE && currentTarget.equals(
                    SimpleVoiceCallClient.callManager.getOtherPlayerUuid())) {
                UUID previous = currentTarget;
                SimpleVoiceCallClient.callManager.endCall();
                var client = Minecraft.getInstance();
                if (client.player != null) ModNetworking.sendCallHangup(client.player.getUUID(), previous);
            }
            currentTarget = null;
            blockedTarget = null;
        }
    }

    public static void tick() {
        if (!queued) return;
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        var callState = SimpleVoiceCallClient.callManager.getState();
        UUID other = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
        boolean ourCall = currentTarget != null && currentTarget.equals(other) &&
                callState != dev.yukiinotenshi.simplephonepromax.call.CallState.NONE;
        if (client.player == null || !context.equals(dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current()) ||
                (SimpleVoiceCallClient.callManager.isInCall() && !ourCall) || EncryptedCalls.active()) {
            cancel();
            return;
        }
        if (!busy && now >= next) {
            busy = true;
            next = now + 1000;
            long epoch = generation;
            JsonObject body = new JsonObject();
            if (blockedTarget != null) body.addProperty("skip_target", blockedTarget.toString());
            CallControlService.extension("org-status", body).whenComplete((value, error) ->
                    client.execute(() -> {
                        if (epoch != generation) return;
                        busy = false;
                        if (error != null) {
                            status = "Связь прервана · повтор через 5 сек";
                            next = System.currentTimeMillis() + 5000;
                        } else {
                            apply(value);
                        }
                    }));
        }
    }

    public static void cancel() {
        queued = false;
        busy = false;
        generation++;
        number = "";
        currentTarget = null;
        blockedTarget = null;
        if (Minecraft.getInstance().player != null && CallControlService.available()) {
            CallControlService.extension("org-cancel", new JsonObject());
        }
    }

    public static boolean shiftChangePending() { return shiftRequestPending; }
    public static boolean canChangeShift() {
        long now = System.currentTimeMillis();
        pruneShiftRequests(now);
        return !shiftRequestPending && now >= nextShiftChangeAt && shiftRequests.size() < 10 && CallControlService.available();
    }

    public static void setShift(String lineNumber, boolean onShift,
                                java.util.function.BiConsumer<Boolean, String> result) {
        long now = System.currentTimeMillis();
        if (shiftRequestPending) {
            result.accept(false, "Изменение смены уже отправляется");
            return;
        }
        if (now < nextShiftChangeAt) {
            result.accept(false, "Подождите перед следующим изменением смены");
            return;
        }
        pruneShiftRequests(now);
        if (shiftRequests.size() >= 10) {
            result.accept(false, "Достигнут лимит смены; повторите через минуту");
            return;
        }
        if (!CallControlService.available()) {
            result.accept(false, "Нет связи с оператором");
            return;
        }
        shiftRequestPending = true;
        nextShiftChangeAt = now + 3000;
        shiftRequests.addLast(now);
        JsonObject body = new JsonObject();
        body.addProperty("number", lineNumber);
        body.addProperty("on_shift", onShift);
        CallControlService.extension("org-shift", body).whenComplete((value, error) ->
                Minecraft.getInstance().execute(() -> {
                    shiftRequestPending = false;
                    boolean success = error == null && value.has("on_shift") && value.get("on_shift").getAsBoolean() == onShift;
                    result.accept(success, success ? "" : error == null ? "Сервер не подтвердил изменение смены" : errorMessage(error));
                }));
    }

    private static void pruneShiftRequests(long now) {
        while (!shiftRequests.isEmpty() && now - shiftRequests.peekFirst() >= 60_000) shiftRequests.removeFirst();
    }

    private static String errorMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) return "Ошибка запроса смены";
        return message.length() > 120 ? message.substring(0, 120) : message;
    }
}
