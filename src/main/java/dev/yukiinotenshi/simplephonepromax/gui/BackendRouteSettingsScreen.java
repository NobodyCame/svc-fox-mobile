package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.BackendRouteHttp;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Route controls for the phone backend. The service URL remains the TLS/Host identity. */
public final class BackendRouteSettingsScreen extends Screen {
    private final Screen parent;
    private EditBox primaryField, fallbackField, connectTimeoutField, requestTimeoutField;
    private Button fallbackButton, modeButton;
    private String validationError = "";

    public BackendRouteSettingsScreen(Screen parent) {
        super(Component.literal("Маршруты подключения"));
        this.parent = parent;
    }

    @Override protected void init() {
        super.init();
        PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(width, height);
        int x = frame.x() + frame.px(18);
        int w = frame.px(160);
        int fieldH = Math.max(12, frame.px(11));
        int labelGap = frame.px(18);
        int firstY = frame.y() + frame.px(23);

        primaryField = field(x, firstY, w, fieldH, "Основной адрес", SimpleVoiceCallClient.config.backendPrimaryEndpoint, 128);
        fallbackField = field(x, firstY + labelGap, w, fieldH, "Резервный адрес", SimpleVoiceCallClient.config.backendFallbackEndpoint, 128);
        int half = (w - frame.px(4)) / 2;
        connectTimeoutField = field(x, firstY + 2 * labelGap, half, fieldH, "Подключение, сек.", Integer.toString(SimpleVoiceCallClient.config.backendConnectTimeoutSeconds), 3);
        requestTimeoutField = field(x + half + frame.px(4), firstY + 2 * labelGap, half, fieldH, "Ответ, сек.", Integer.toString(SimpleVoiceCallClient.config.backendRequestTimeoutSeconds), 3);

        int buttonY = firstY + 3 * labelGap + frame.px(1);
        int buttonW = half;
        fallbackButton = addRenderableWidget(Button.builder(fallbackLabel(), button -> {
            SimpleVoiceCallClient.config.backendAutoFallback = !SimpleVoiceCallClient.config.backendAutoFallback;
            fallbackButton.setMessage(fallbackLabel());
        }).bounds(x, buttonY, buttonW, fieldH).build());
        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> {
            String current = SimpleVoiceCallClient.config.backendRouteMode;
            SimpleVoiceCallClient.config.backendRouteMode = switch (current == null ? "AUTO" : current) {
                case "AUTO" -> "PRIMARY";
                case "PRIMARY" -> "BACKUP";
                default -> "AUTO";
            };
            modeButton.setMessage(modeLabel());
        }).bounds(x + buttonW + frame.px(4), buttonY, buttonW, fieldH).build());

        int footerY = Math.min(height - frame.px(14) - 4, frame.bottom() + frame.px(3));
        int footerW = (w - frame.px(4)) / 2;
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> save(true))
                .bounds(x, footerY, footerW, Math.max(12, frame.px(13))).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), button -> save(true))
                .bounds(x + footerW + frame.px(4), footerY, footerW, Math.max(12, frame.px(13))).build());
    }

    private EditBox field(int x, int y, int w, int h, String label, String value, int maxLength) {
        EditBox edit = new EditBox(font, x, y, w, h, Component.literal(label));
        edit.setMaxLength(maxLength);
        edit.setValue(value == null ? "" : value);
        addRenderableWidget(edit);
        return edit;
    }

    private Component fallbackLabel() {
        return Component.literal("Авто: " + (SimpleVoiceCallClient.config.backendAutoFallback ? "ВКЛ" : "ВЫКЛ"));
    }

    private Component modeLabel() {
        String mode = SimpleVoiceCallClient.config.backendRouteMode;
        String label = switch (mode == null ? "AUTO" : mode) {
            case "PRIMARY" -> "Основной";
            case "BACKUP" -> "Резервный";
            default -> "Авто";
        };
        return Component.literal("Маршрут: " + label + " ›");
    }

    private void save(boolean close) {
        String primary = primaryField == null ? "" : primaryField.getValue().trim();
        String fallback = fallbackField == null ? "" : fallbackField.getValue().trim();
        int connectTimeout, requestTimeout;
        try {
            connectTimeout = Integer.parseInt(connectTimeoutField.getValue().trim());
            requestTimeout = Integer.parseInt(requestTimeoutField.getValue().trim());
        } catch (RuntimeException invalid) {
            validationError = "Укажи тайм-аут числом в секундах";
            return;
        }
        if (!BackendRouteHttp.validEndpoint(primary) || !BackendRouteHttp.validEndpoint(fallback)) {
            validationError = "Адрес укажи как имя-хоста:порт";
            return;
        }
        if (connectTimeout < 1 || connectTimeout > 120 || requestTimeout < 1 || requestTimeout > 600) {
            validationError = "Тайм-аут: подключение 1–120, ответ 1–600 сек.";
            return;
        }
        validationError = "";
        SimpleVoiceCallClient.config.backendPrimaryEndpoint = primary;
        SimpleVoiceCallClient.config.backendFallbackEndpoint = fallback;
        SimpleVoiceCallClient.config.backendConnectTimeoutSeconds = connectTimeout;
        SimpleVoiceCallClient.config.backendRequestTimeoutSeconds = requestTimeout;
        SimpleVoiceCallClient.config.save();
        if (close && minecraft != null) minecraft.setScreenAndShow(parent);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        WallpaperEngine.render(graphics, width, height);
        PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(width, height);
        PhoneGuiTextures.drawUniversalBackground(graphics, frame, mouseX, mouseY);
        PhoneGuiTextures.drawCenteredTrimmedText(graphics, font, title, frame.centerX(), frame.y() + frame.px(6), frame.px(160), -1);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Основной физический адрес", primaryField.getX(), primaryField.getY() - font.lineHeight - 2, primaryField.getWidth(), 0xFFFFE070, true);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Резервный Portmap", fallbackField.getX(), fallbackField.getY() - font.lineHeight - 2, fallbackField.getWidth(), 0xFFFFE070, true);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Подключение", connectTimeoutField.getX(), connectTimeoutField.getY() - font.lineHeight - 2, connectTimeoutField.getWidth(), 0xFFFFE070, true);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Ответ", requestTimeoutField.getX(), requestTimeoutField.getY() - font.lineHeight - 2, requestTimeoutField.getWidth(), 0xFFFFE070, true);
        graphics.text(font, "TLS/Host: " + BackendRouteHttp.TLS_SERVER_NAME, frame.x() + frame.px(18), frame.bottom() - frame.px(7), 0xFF65D879, false);
        if (!validationError.isBlank()) graphics.text(font, validationError, frame.centerX() - font.width(validationError) / 2, frame.bottom() - frame.px(18), 0xFFFF7777, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        PhoneGuiTextures.widgets(this, graphics, mouseX, mouseY);
    }

    @Override public void onClose() { save(true); }
    @Override public boolean isPauseScreen() { return false; }
}
