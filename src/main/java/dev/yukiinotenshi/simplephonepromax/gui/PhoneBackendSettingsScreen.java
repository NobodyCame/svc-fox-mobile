package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PhoneBackendSettingsScreen extends Screen {
   private final Screen parent;
   private EditBox urlField;
   private EditBox deviceField;
   private EditBox tokenField;

   public PhoneBackendSettingsScreen(Screen parent) {
      super(Component.translatable("phone.settings.backend.title"));
      this.parent = parent;
   }

   protected void init() {
      super.init();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int x = frame.x() + frame.px(18);
      int w = frame.px(160);
      int h = Math.max(12, frame.px(13));
      int field1Y = frame.y() + frame.px(27);
      int field2Y = frame.y() + frame.px(49);
      int field3Y = frame.y() + frame.px(71);
      int buttonGap = Math.max(2, frame.px(3));

      this.urlField = new EditBox(this.font, x, field1Y, w, h, Component.translatable("phone.settings.backend.url"));
      this.urlField.setMaxLength(256);
      this.urlField.setValue(SimpleVoiceCallClient.config.backendBaseUrl);
      this.addRenderableWidget(this.urlField);

      this.deviceField = new EditBox(this.font, x, field2Y, w, h, Component.translatable("phone.settings.backend.device"));
      this.deviceField.setMaxLength(128);
      this.deviceField.setValue(SimpleVoiceCallClient.config.backendDeviceId);
      this.addRenderableWidget(this.deviceField);

      this.tokenField = new EditBox(this.font, x, field3Y, w, h, Component.translatable("phone.settings.backend.token"));
      this.tokenField.setMaxLength(256);
      this.tokenField.setValue(SimpleVoiceCallClient.config.backendAdminToken);
      this.addRenderableWidget(this.tokenField);

      int buttonW = (w - buttonGap) / 2;
      int buttonH = Math.max(12, frame.px(14));
      int leftX = frame.x() + frame.px(18);
      int rightX = leftX + buttonW + buttonGap;
      int rowY = frame.y() + frame.px(92);

      this.addRenderableWidget(Button.builder(Component.translatable(SimpleVoiceCallClient.config.backendNumbersEnabled ? "phone.settings.backend.enabled" : "phone.settings.backend.disabled"), b -> {
         SimpleVoiceCallClient.config.backendNumbersEnabled = !SimpleVoiceCallClient.config.backendNumbersEnabled;
         this.saveFields(false);
         this.rebuild();
      }).size(buttonW, buttonH).pos(leftX, rowY).build());

      this.addRenderableWidget(Button.builder(Component.translatable("phone.standard.title"), b -> {
         this.saveFields(false);this.minecraft.setScreenAndShow(new PhoneCallStandardScreen(this));
      }).size(buttonW, buttonH).pos(rightX, rowY).build());

      this.addRenderableWidget(Button.builder(Component.translatable("phone.settings.backend.refresh"), b -> {
         this.saveFields(false);
         if (this.minecraft != null) {
            BackendNumberService.registerSelfAsync(this.minecraft);
         }
      }).size(buttonW, buttonH).pos(leftX, rowY + buttonH + buttonGap).build());

      this.addRenderableWidget(Button.builder(Component.translatable("phone.settings.save"), b -> {
         this.saveFields(true);
      }).size(buttonW, buttonH).pos(rightX, rowY + buttonH + buttonGap).build());

      int routeButtonHeight = Math.max(10, frame.px(8));
      int routeButtonY = Math.min(rowY + 2 * (buttonH + buttonGap), this.height - buttonH - 4 - routeButtonHeight - 2);
      this.addRenderableWidget(Button.builder(Component.literal("Маршруты подключения"), b -> {
         this.saveFields(false);
         if (this.minecraft != null) this.minecraft.setScreenAndShow(new BackendRouteSettingsScreen(this));
      }).size(w, routeButtonHeight).pos(x, routeButtonY).build());

      this.addRenderableWidget(Button.builder(Component.translatable("phone.settings.back"), b -> {
         this.saveFields(true);
      }).size(frame.px(92), buttonH).pos(frame.centerX() - frame.px(46), Math.min(this.height - buttonH - 4, frame.bottom() + frame.px(3))).build());
   }

   private void rebuild() {
      if (this.minecraft != null) {
         this.minecraft.setScreenAndShow(new PhoneBackendSettingsScreen(this.parent));
      }
   }

   private void saveFields(boolean close) {
      SimpleVoiceCallClient.config.backendBaseUrl = this.urlField != null ? dev.yukiinotenshi.simplephonepromax.network.OperatorHttp.upgrade(this.urlField.getValue().trim()) : "";
      SimpleVoiceCallClient.config.backendDeviceId = this.deviceField != null ? this.deviceField.getValue().trim() : "";
      SimpleVoiceCallClient.config.backendAdminToken = this.tokenField != null ? this.tokenField.getValue().trim() : "";
      SimpleVoiceCallClient.config.save();
      if (close && this.minecraft != null) {
         this.minecraft.setScreenAndShow(this.parent);
      }
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      WallpaperEngine.render(context, this.width, this.height);
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      PhoneGuiTextures.drawUniversalBackground(context, frame, mouseX, mouseY);
      PhoneGuiTextures.drawCenteredTrimmedText(context, this.font, this.title, frame.centerX(), frame.y() + frame.px(6), frame.px(160), -1);
      this.drawFieldLabel(context, this.urlField, "phone.settings.backend.url");
      this.drawFieldLabel(context, this.deviceField, "phone.settings.backend.device");
      this.drawFieldLabel(context, this.tokenField, "phone.settings.backend.token");

      super.extractRenderState(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void drawFieldLabel(GuiGraphicsExtractor context, EditBox field, String key) {
      PhoneGuiTextures.drawTrimmedText(context, this.font, Component.translatable(key).getString(),
         field.getX(), field.getY() - this.font.lineHeight - 2, field.getWidth(), 0xFFFFE070, true);
   }

   public void onClose() {
      this.saveFields(true);
      PhoneClientActions.putAwayPhone();
   }
}

