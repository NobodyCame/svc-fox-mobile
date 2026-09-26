package dev.yukiinotenshi.simplephonepromax.network;

import com.google.gson.JsonObject;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDialer;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;

public final class OrganizationCalls {
    private static boolean queued, busy;
    private static boolean shiftRequestPending;
    private static long nextShiftChangeAt;
    private static final ArrayDeque<Long> shiftRequests = new ArrayDeque<>();
    private static String status = "", context = "", number = "";
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
                MinecraftClient.getInstance().execute(() -> {
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
            status = "Связь с очередью потеряна";
            return;
        }
        status = "Очередь " + value.get("name").getAsString() + " · место " +
                (value.has("position") ? value.get("position").getAsInt() : 1);
        if (value.has("target") && !value.get("target").isJsonNull()) {
            UUID target = UUID.fromString(value.get("target").getAsString());
            String name = value.get("name").getAsString();
            cancel();
            if (PhoneDialer.call(target, name)) {
                status = "Соединяем с оператором";
                MinecraftClient.getInstance().setScreen(new dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen());
            } else {
                status = "Оператор недоступен · попробуйте позже";
            }
        }
    }

    public static void tick() {
        if (!queued) return;
        long now = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !context.equals(dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current()) ||
                SimpleVoiceCallClient.callManager.isInCall() || EncryptedCalls.active()) {
            cancel();
            return;
        }
        if (!busy && now >= next) {
            busy = true;
            next = now + 1000;
            long epoch = generation;
            CallControlService.extension("org-status", new JsonObject()).whenComplete((value, error) ->
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
        if (MinecraftClient.getInstance().player != null && CallControlService.available()) {
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
                MinecraftClient.getInstance().execute(() -> {
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
