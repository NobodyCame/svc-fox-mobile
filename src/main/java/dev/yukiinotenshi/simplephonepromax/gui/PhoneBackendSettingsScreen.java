package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class PhoneBackendSettingsScreen extends Screen {
   private final Screen parent;
   private TextFieldWidget urlField;
   private TextFieldWidget deviceField;
   private TextFieldWidget tokenField;

   public PhoneBackendSettingsScreen(Screen parent) {
      super(Text.translatable("phone.settings.backend.title"));
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

      this.urlField = new TextFieldWidget(this.textRenderer, x, field1Y, w, h, Text.translatable("phone.settings.backend.url"));
      this.urlField.setMaxLength(256);
      this.urlField.setText(SimpleVoiceCallClient.config.backendBaseUrl);
      this.addDrawableChild(this.urlField);

      this.deviceField = new TextFieldWidget(this.textRenderer, x, field2Y, w, h, Text.translatable("phone.settings.backend.device"));
      this.deviceField.setMaxLength(128);
      this.deviceField.setText(SimpleVoiceCallClient.config.backendDeviceId);
      this.addDrawableChild(this.deviceField);

      this.tokenField = new TextFieldWidget(this.textRenderer, x, field3Y, w, h, Text.translatable("phone.settings.backend.token"));
      this.tokenField.setMaxLength(256);
      this.tokenField.setText(SimpleVoiceCallClient.config.backendAdminToken);
      this.addDrawableChild(this.tokenField);

      int buttonW = (w - buttonGap) / 2;
      int buttonH = Math.max(12, frame.px(14));
      int leftX = frame.x() + frame.px(18);
      int rightX = leftX + buttonW + buttonGap;
      int rowY = frame.y() + frame.px(92);

      this.addDrawableChild(ButtonWidget.builder(Text.translatable(SimpleVoiceCallClient.config.backendNumbersEnabled ? "phone.settings.backend.enabled" : "phone.settings.backend.disabled"), b -> {
         SimpleVoiceCallClient.config.backendNumbersEnabled = !SimpleVoiceCallClient.config.backendNumbersEnabled;
         this.saveFields(false);
         this.rebuild();
      }).size(buttonW, buttonH).position(leftX, rowY).build());

      this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.standard.title"), b -> {
         this.saveFields(false);this.client.setScreen(new PhoneCallStandardScreen(this));
      }).size(buttonW, buttonH).position(rightX, rowY).build());

      this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.settings.backend.refresh"), b -> {
         this.saveFields(false);
         if (this.client != null) {
            BackendNumberService.registerSelfAsync(this.client);
         }
      }).size(buttonW, buttonH).position(leftX, rowY + buttonH + buttonGap).build());

      this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.settings.save"), b -> {
         this.saveFields(true);
      }).size(buttonW, buttonH).position(rightX, rowY + buttonH + buttonGap).build());

      this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.settings.back"), b -> {
         this.saveFields(true);
      }).size(frame.px(92), buttonH).position(frame.centerX() - frame.px(46), Math.min(this.height - buttonH - 4, frame.bottom() + frame.px(3))).build());
   }

   private void rebuild() {
      if (this.client != null) {
         this.client.setScreen(new PhoneBackendSettingsScreen(this.parent));
      }
   }

   private void saveFields(boolean close) {
      SimpleVoiceCallClient.config.backendBaseUrl = this.urlField != null ? dev.yukiinotenshi.simplephonepromax.network.OperatorHttp.upgrade(this.urlField.getText().trim()) : "";
      SimpleVoiceCallClient.config.backendDeviceId = this.deviceField != null ? this.deviceField.getText().trim() : "";
      SimpleVoiceCallClient.config.backendAdminToken = this.tokenField != null ? this.tokenField.getText().trim() : "";
      SimpleVoiceCallClient.config.save();
      if (close && this.client != null) {
         this.client.setScreen(this.parent);
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      WallpaperEngine.render(context, this.width, this.height);
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      PhoneGuiTextures.drawUniversalBackground(context, frame, mouseX, mouseY);
      PhoneGuiTextures.drawCenteredTrimmedText(context, this.textRenderer, this.title, frame.centerX(), frame.y() + frame.px(6), frame.px(160), -1);
      this.drawFieldLabel(context, this.urlField, "phone.settings.backend.url");
      this.drawFieldLabel(context, this.deviceField, "phone.settings.backend.device");
      this.drawFieldLabel(context, this.tokenField, "phone.settings.backend.token");

      super.render(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
   }

   public boolean shouldPause() {
      return false;
   }

   private void drawFieldLabel(DrawContext context, TextFieldWidget field, String key) {
      PhoneGuiTextures.drawTrimmedText(context, this.textRenderer, Text.translatable(key).getString(),
         field.getX(), field.getY() - this.textRenderer.fontHeight - 2, field.getWidth(), 0xFFFFE070, true);
   }

   public void close() {
      this.saveFields(true);
      PhoneClientActions.putAwayPhone();
   }
}

