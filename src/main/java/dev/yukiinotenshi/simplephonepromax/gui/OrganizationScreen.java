package dev.yukiinotenshi.simplephonepromax.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class OrganizationScreen extends Screen {
    private JsonArray lines = new JsonArray();
    private String error = "";
    private boolean loaded, loading, rebuildAfterLoad;
    private int page, ticks;

    public OrganizationScreen() { super(Text.literal("Организации")); }

    private void loadLines(boolean rebuild) {
        if (loading || !CallControlService.available()) {
            rebuildAfterLoad |= rebuild;
            return;
        }
        loading = true;
        rebuildAfterLoad |= rebuild;
        CallControlService.extension("org-list", new JsonObject()).whenComplete((value, failure) ->
                client.execute(() -> {
                    loading = false;
                    if (client.currentScreen != this) return;
                    boolean changed = false;
                    if (failure != null) {
                        error = "Сервис временно недоступен";
                    } else {
                        JsonArray fresh = value.has("lines") ? value.getAsJsonArray("lines") : new JsonArray();
                        changed = !lines.equals(fresh);
                        lines = fresh.deepCopy();
                        if (error.equals("Сервис временно недоступен")) error = "";
                    }
                    loaded = true;
                    boolean shouldRebuild = rebuildAfterLoad || changed;
                    rebuildAfterLoad = false;
                    if (shouldRebuild) clearAndInit();
                }));
    }

    @Override
    protected void init() {
        int width = Math.min(460, this.width - 24);
        int x = (this.width - width) / 2;
        int y = 62;
        if (!loaded) loadLines(false);

        if (OrganizationCalls.queued()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Отменить ожидание"), button -> {
                OrganizationCalls.cancel();
                clearAndInit();
            }).dimensions(x, y, width, 20).build());
        } else {
            int rowHeight = 46;
            int count = Math.max(1, (height - 150) / rowHeight);
            page = Math.min(page, Math.max(0, (lines.size() - 1) / count));
            int end = Math.min(lines.size(), (page + 1) * count);
            for (int i = page * count; i < end; i++) {
                JsonObject line = lines.get(i).getAsJsonObject();
                String number = line.get("number").getAsString();
                String name = line.get("name").getAsString();
                boolean assigned = line.has("is_agent") && line.get("is_agent").getAsBoolean();
                int rowY = y + (i - page * count) * rowHeight;
                int shiftWidth = assigned ? 96 : 0;
                int callWidth = width - (assigned ? shiftWidth + 4 : 0);
                addDrawableChild(ButtonWidget.builder(Text.literal(name + " · +" + number), button ->
                        OrganizationCalls.tryNumber(number).whenComplete((handled, failure) -> client.execute(() -> {
                            if (failure != null) error = "Не удалось встать в очередь";
                            else if (Boolean.TRUE.equals(handled)) clearAndInit();
                        }))).dimensions(x, rowY, callWidth, 20).build());
                if (assigned) {
                    boolean onShift = line.has("on_shift") && line.get("on_shift").getAsBoolean();
                    ButtonWidget shift = addDrawableChild(ButtonWidget.builder(
                            Text.literal(onShift ? "Смена: да" : "Смена: нет"), button -> {
                                OrganizationCalls.setShift(number, !onShift, (success, reason) -> {
                                    error = success ? "" : "Не удалось изменить смену: " + reason;
                                    if (success) loadLines(true);
                                    else clearAndInit();
                                });
                                clearAndInit();
                            }).dimensions(x + callWidth + 4, rowY, shiftWidth, 20).build());
                    shift.active = OrganizationCalls.canChangeShift();
                }
            }
            if (lines.isEmpty()) {
                ButtonWidget empty = addDrawableChild(ButtonWidget.builder(
                        Text.literal("Линии создаёт оператор Fox Mobile"), button -> {}).dimensions(x, y, width, 20).build());
                empty.active = false;
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("←"), button -> {
                page = Math.max(0, page - 1);
                clearAndInit();
            }).dimensions(x, height - 34, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("→"), button -> {
                if ((page + 1) * count < lines.size()) page++;
                clearAndInit();
            }).dimensions(x + 44, height - 34, 40, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> close())
                .dimensions(x + width - 100, height - 34, 100, 20).build());
    }

    @Override
    public void tick() {
        if (++ticks % 100 == 0) loadLines(false);
        if (!OrganizationCalls.shiftChangePending()) {
            for (var child : children()) if (child instanceof ButtonWidget button &&
                    button.getMessage().getString().startsWith("Смена:")) button.active = OrganizationCalls.canChangeShift();
        }
        if (OrganizationCalls.queued() && children().stream().noneMatch(child -> child instanceof ButtonWidget button &&
                button.getMessage().getString().equals("Отменить ожидание"))) clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        PhoneGuiTextures.fullBackground(context, width, height, mouseX, mouseY);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);
        String status = OrganizationCalls.queued() ? OrganizationCalls.status() : !error.isEmpty() ? error :
                "Свободные операторы примут звонок · очередь ждёт смену";
        context.drawWrappedTextWithShadow(textRenderer, Text.literal(status), 12, 36, width - 24, 0xFFFFFFFF);
        if (!OrganizationCalls.queued() && lines.size() > 0) {
            int rowHeight = 46;
            int count = Math.max(1, (height - 150) / rowHeight);
            int y = 85;
            for (int i = page * count; i < Math.min(lines.size(), (page + 1) * count); i++) {
                JsonObject line = lines.get(i).getAsJsonObject();
                int free = line.has("available_agents") ? line.get("available_agents").getAsInt() : 0;
                int active = line.has("active_agents") ? line.get("active_agents").getAsInt() : 0;
                int queue = line.has("queue_size") ? line.get("queue_size").getAsInt() : 0;
                context.drawTextWithShadow(textRenderer,
                        Text.literal("На смене: " + active + " · свободно: " + free + " · ждут: " + queue),
                        (width - Math.min(460, width - 24)) / 2 + 4, y + (i - page * count) * rowHeight + 24,
                        0xFFB8C8CF);
            }
        }
        super.render(context, mouseX, mouseY, delta);
        PhoneGuiTextures.widgets(this, context, mouseX, mouseY);
    }

    @Override public void close() { client.setScreen(new PhoneMainScreen()); }
    @Override public boolean shouldPause() { return false; }
}
