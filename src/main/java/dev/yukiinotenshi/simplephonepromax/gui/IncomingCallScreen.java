package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;

public class IncomingCallScreen extends Screen {
   private long lastBlinkTime = System.currentTimeMillis();
   private boolean blinkVisible = true;
   private final List<ButtonWidget> texturedButtons = new ArrayList<>();

   public IncomingCallScreen() {
      super(Text.translatable("phone.incoming.title"));
   }

   private void addTexturedButton(ButtonWidget button) {
      this.texturedButtons.add(button);
      this.addDrawableChild(button);
   }

   protected void init() {
      try {
         super.init();
      } catch (Throwable var7) {
      }

      this.texturedButtons.clear();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int gap = Math.max(2, frame.px(2));
      int buttonWidth = Math.max(50, frame.px(68));
      int buttonHeight = Math.max(11, frame.px(12));
      int buttonY = frame.y() + frame.px(62);
      int buttonX = frame.x() + frame.px(110);
      ButtonWidget acceptButton = ButtonWidget.builder(Text.translatable("phone.incoming.answer"), b -> {
         UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         UUID playerUuid = null;
         if (this.client != null && this.client.player != null) {
            playerUuid = this.client.player.getUuid();
         }

         if (otherUuid != null && playerUuid != null) {
            ModNetworking.sendCallAccept(playerUuid, otherUuid);
         }
      }).size(buttonWidth, buttonHeight).position(buttonX, buttonY).build();
      this.addTexturedButton(acceptButton);
      ButtonWidget declineButton = ButtonWidget.builder(Text.translatable("phone.incoming.decline"), b -> {
         UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         UUID playerUuid = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
         if (otherUuid != null && playerUuid != null) {
            ModNetworking.sendCallDecline(playerUuid, otherUuid);
         }

         SimpleVoiceCallClient.callManager.declineCall();
         this.close();
      }).size(buttonWidth, buttonHeight).position(buttonX, buttonY + buttonHeight + gap).build();
      this.addTexturedButton(declineButton);
      ButtonWidget blockButton = ButtonWidget.builder(Text.translatable("phone.call.block"), b -> {
         UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         String otherName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
         if (otherUuid != null) {
            SimpleVoiceCallClient.config.addBlocked(otherUuid, otherName);
            ModNetworking.sendCallBlocked(otherUuid);
         } else if (otherName != null) {
            SimpleVoiceCallClient.config.addBlocked(otherName);
         }

         SimpleVoiceCallClient.callManager.declineCall();
         this.close();
      }).size(buttonWidth, buttonHeight).position(buttonX, buttonY + 2 * (buttonHeight + gap)).build();
      this.addTexturedButton(blockButton);
   }

   public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTick) {
      try {
         long now = System.currentTimeMillis();
         if (now - this.lastBlinkTime >= 500L) {
            this.blinkVisible = !this.blinkVisible;
            this.lastBlinkTime = now;
         }

         WallpaperEngine.render(drawContext, this.width, this.height);
         PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
         int centerX = frame.centerX();
         try {
            PhoneGuiTextures.drawPhone(drawContext, frame, mouseX, mouseY);
         } catch (Throwable var20) {
         }
         try {
            PhoneGuiTextures.drawIncomingPopup(drawContext, frame, 2);
         } catch (Throwable ignored) {
         }

         if (this.blinkVisible) {
            Text callTitle = Text.translatable("phone.incoming.title");

            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.textRenderer, callTitle, centerX, frame.y() + frame.px(12), frame.px(170), 16776960
               );
            } catch (Throwable var18) {
            }
         }

         String callerName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
         if (callerName != null) {
            Text nameComponent = Text.literal(callerName);
            String trimmedName = this.textRenderer.trimToWidth(nameComponent.getString(), frame.px(92));
            int nameY = frame.y() + frame.px(38);
            int nameX = centerX - this.textRenderer.getWidth(trimmedName) / 2;

            try {
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, trimmedName, nameX + 1, nameY, 0xFF000000, false);
            } catch (Throwable var17) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, trimmedName, nameX - 1, nameY, 0xFF000000, false);
            } catch (Throwable var16) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, trimmedName, nameX, nameY + 1, 0xFF000000, false);
            } catch (Throwable var15) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, trimmedName, nameX, nameY - 1, 0xFF000000, false);
            } catch (Throwable var14) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, trimmedName, nameX, nameY, 0xFFFF5555, false);
            } catch (Throwable var13) {
            }
         }

         super.render(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
         try {
            for (ButtonWidget button : this.texturedButtons) {
               PhoneGuiTextures.drawButton(drawContext, this.textRenderer, button, mouseX, mouseY);
            }
         } catch (Throwable ignored) {
         }
      } catch (Throwable var19) {
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      PhoneClientActions.putAwayPhone();
      super.close();
   }
}



