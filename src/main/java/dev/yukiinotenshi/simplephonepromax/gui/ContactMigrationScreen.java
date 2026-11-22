package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles;
import dev.yukiinotenshi.simplephonepromax.phone.SettingsBackup;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ContactMigrationScreen extends Screen {
    private final Screen parent;
    private final String destination = ServerProfiles.current();
    private SettingsBackup.Data backup;
    private int selection;
    private String status = "";

    public ContactMigrationScreen(Screen parent) {
        super(Component.literal("Перенос контактов"));
        this.parent = parent;
    }

    @Override protected void init() {
        var config = SimpleVoiceCallClient.config;
        var contacts = backup == null ? config.contacts : backup.contacts;
        List<String> profiles = contacts.stream().filter(Objects::nonNull)
                .map(contact -> Objects.toString(contact.server, ""))
                .filter(profile -> backup != null || !destination.equals(profile))
                .distinct().sorted().toList();
        String source = profiles.isEmpty() ? null : profiles.get(Math.floorMod(selection, profiles.size()));
        String name = source == null ? "Нет профилей для переноса" : source.isBlank() ? "Без сервера" : source;
        int w = Math.min(320, width - 20), x = (width - w) / 2, y = Math.max(58, height / 2 - 40);

        var cycle = addRenderableWidget(Button.builder(Component.literal(name + (profiles.size() > 1 ? "  ›" : "")),
                b -> { selection++; rebuildWidgets(); }).bounds(x, y, w, 20).build());
        cycle.active = profiles.size() > 1;
        var copy = addRenderableWidget(Button.builder(Component.literal("Скопировать в текущий профиль"), b -> {
            if (!destination.equals(ServerProfiles.current())) {
                status = "Сервер изменился. Откройте перенос заново.";
                return;
            }
            int count = SettingsBackup.copyContacts(backup, source, destination);
            status = "Скопировано: " + count;
        }).bounds(x, y + 24, w, 20).build());
        copy.active = source != null;

        addRenderableWidget(Button.builder(Component.literal(backup == null ? "Выбрать профиль из резервной копии" : "Выбрать из текущих контактов"), b -> {
            try {
                backup = backup == null ? SettingsBackup.read() : null;
                selection = 0;
                status = "";
                rebuildWidgets();
            } catch (Exception e) {
                status = "Не удалось прочитать резервную копию";
            }
        }).bounds(x, y + 48, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x, height - 24, w, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        PhoneGuiTextures.fullBackground(graphics, width, height, mx, my);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        int textWidth = Math.max(20, width - 24);
        graphics.centeredText(font, font.plainSubstrByWidth("Куда: " + destination, textWidth), width / 2, 22, 0xFFFFFFFF);
        graphics.textWithWordWrap(font, Component.literal("Проверьте номера после переноса."), 12, 36, textWidth, 0xFFE5C580);
        graphics.centeredText(font, status, width / 2, height - 39, 0xFF80FF80);
        super.extractRenderState(graphics, mx, my, delta);
        PhoneGuiTextures.widgets(this, graphics, mx, my);
    }

    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
