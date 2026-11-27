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
    private int footerY;

    public BackendRouteSettingsScreen(Screen parent) {
        super(Component.literal("Маршруты подключения"));
        this.parent = parent;
    }

    @Override protected void init() {
        super.init();
        PhoneGuiTextures.Frame frame = frame();
        boolean compact = frame.height() < 150;
        int x = frame.x() + 12, w = frame.width() - 24;
        int fieldH = compact ? 12 : Math.min(20, Math.max(14, frame.px(11)));
        int labelGap = fieldH + font.lineHeight + (compact ? 1 : 5);
        int firstY = frame.y() + 23;

        int half = (w - 4) / 2;
        primaryField = field(x, firstY, compact ? half : w, fieldH, "Основной адрес", SimpleVoiceCallClient.config.backendPrimaryEndpoint, 128);
        fallbackField = field(compact ? x + half + 4 : x, compact ? firstY : firstY + labelGap,
                compact ? w - half - 4 : w, fieldH, "Резервный адрес", SimpleVoiceCallClient.config.backendFallbackEndpoint, 128);
        int timeoutY = firstY + (compact ? 1 : 2) * labelGap;
        connectTimeoutField = field(x, timeoutY, half, fieldH, "Подключение, сек.", Integer.toString(SimpleVoiceCallClient.config.backendConnectTimeoutSeconds), 3);
        requestTimeoutField = field(x + half + 4, timeoutY, w - half - 4, fieldH, "Ответ, сек.", Integer.toString(SimpleVoiceCallClient.config.backendRequestTimeoutSeconds), 3);

        int buttonY = timeoutY + fieldH + (compact ? 2 : 3);
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
        }).bounds(x + buttonW + 4, buttonY, w - buttonW - 4, fieldH).build());

        int footerH = compact ? 13 : 20;
        footerY = frame.bottom() - footerH - 4;
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> save(true))
                .bounds(x, footerY, half, footerH).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), button -> save(true))
                .bounds(x + half + 4, footerY, w - half - 4, footerH).build());
    }

    private PhoneGuiTextures.Frame frame() {
        return PhoneGuiTextures.dialogLayout(width, height, 164);
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
            validationError = "Подключение: 1–120 с. Ответ: 1–600 с.";
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
        PhoneGuiTextures.Frame frame = frame();
        PhoneGuiTextures.drawUniversalBackground(graphics, frame, mouseX, mouseY);
        PhoneGuiTextures.drawCenteredTrimmedText(graphics, font, title, frame.centerX(), frame.y() + 3, frame.width()-24, -1);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Основной физический адрес", primaryField.getX(), primaryField.getY() - font.lineHeight - 2, primaryField.getWidth(), 0xFFFFE070, true);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Резервный Portmap", fallbackField.getX(), fallbackField.getY() - font.lineHeight - 2, fallbackField.getWidth(), 0xFFFFE070, true);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Подключение", connectTimeoutField.getX(), connectTimeoutField.getY() - font.lineHeight - 2, connectTimeoutField.getWidth(), 0xFFFFE070, true);
        PhoneGuiTextures.drawTrimmedText(graphics, font, "Ответ", requestTimeoutField.getX(), requestTimeoutField.getY() - font.lineHeight - 2, requestTimeoutField.getWidth(), 0xFFFFE070, true);
        int noteY = modeButton.getY() + modeButton.getHeight() + 2;
        int lines = Math.max(0, (footerY - noteY) / font.lineHeight);
        if (validationError.isBlank()) {
            PhoneGuiTextures.drawWrappedLines(graphics, font, Component.literal("TLS/Host: " + BackendRouteHttp.TLS_SERVER_NAME),
                    frame.x()+12, noteY, frame.width()-24, lines, 0xFF65D879);
        } else {
            PhoneGuiTextures.drawWrappedLines(graphics, font, Component.literal(validationError),
                    frame.x()+12, noteY, frame.width()-24, lines, 0xFFFF7777);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        PhoneGuiTextures.widgets(this, graphics, mouseX, mouseY);
    }

    @Override public void onClose() { save(true); }
    @Override public boolean isPauseScreen() { return false; }
}
