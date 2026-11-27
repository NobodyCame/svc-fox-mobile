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
        int h = buttonHeight(), step = h + 4;
        int w = Math.min(320, width - 20), x = (width - w) / 2;
        int y = compact() ? 39 : Math.max(56, height / 2 - 42);

        var cycle = addRenderableWidget(Button.builder(Component.literal(name + (profiles.size() > 1 ? "  ›" : "")),
                b -> { selection++; rebuildWidgets(); }).bounds(x, y, w, h).build());
        cycle.active = profiles.size() > 1;
        var copy = addRenderableWidget(Button.builder(Component.literal("Скопировать в текущий профиль"), b -> {
            if (!destination.equals(ServerProfiles.current())) {
                status = "Сервер изменился. Откройте перенос заново.";
                return;
            }
            int count = SettingsBackup.copyContacts(backup, source, destination);
            status = "Скопировано: " + count;
        }).bounds(x, y + step, w, h).build());
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
        }).bounds(x, y + 2*step, w, h).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x, backY(), w, h).build());
    }

    private boolean compact() { return height < 170; }
    private int buttonHeight() { return compact() ? 14 : 20; }
    private int backY() { return height - buttonHeight() - 4; }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        PhoneGuiTextures.fullBackground(graphics, width, height, mx, my);
        graphics.centeredText(font, title, width / 2, compact()?4:8, 0xFFFFFFFF);
        int textWidth = Math.max(20, width - 24);
        PhoneGuiTextures.drawCenteredTrimmedText(graphics,font,Component.literal("Куда: " + destination),width/2,compact()?15:22,textWidth,0xFFFFFFFF);
        PhoneGuiTextures.drawCenteredTrimmedText(graphics,font,Component.literal("Проверьте номера после переноса."),width/2,compact()?26:36,textWidth,0xFFE5C580);
        PhoneGuiTextures.drawCenteredTrimmedText(graphics,font,Component.literal(status),width/2,backY()-font.lineHeight-4,textWidth,0xFF80FF80);
        super.extractRenderState(graphics, mx, my, delta);
        PhoneGuiTextures.widgets(this, graphics, mx, my);
    }

    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
