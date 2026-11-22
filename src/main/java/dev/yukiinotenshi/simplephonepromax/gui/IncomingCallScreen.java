package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class IncomingCallScreen extends Screen {
   private long lastBlinkTime = System.currentTimeMillis();
   private boolean blinkVisible = true;
   private final List<Button> texturedButtons = new ArrayList<>();

   public IncomingCallScreen() {
      super(Component.translatable("phone.incoming.title"));
   }

   private void addTexturedButton(Button button) {
      this.texturedButtons.add(button);
      this.addRenderableWidget(button);
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
      Button acceptButton = Button.builder(Component.translatable("phone.incoming.answer"), b -> {
         UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         UUID playerUuid = null;
         if (this.minecraft != null && this.minecraft.player != null) {
            playerUuid = this.minecraft.player.getUUID();
         }

         if (otherUuid != null && playerUuid != null) {
            ModNetworking.sendCallAccept(playerUuid, otherUuid);
         }
      }).size(buttonWidth, buttonHeight).pos(buttonX, buttonY).build();
      this.addTexturedButton(acceptButton);
      Button declineButton = Button.builder(Component.translatable("phone.incoming.decline"), b -> {
         UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         UUID playerUuid = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
         if (otherUuid != null && playerUuid != null) {
            ModNetworking.sendCallDecline(playerUuid, otherUuid);
         }

         SimpleVoiceCallClient.callManager.declineCall();
         this.onClose();
      }).size(buttonWidth, buttonHeight).pos(buttonX, buttonY + buttonHeight + gap).build();
      this.addTexturedButton(declineButton);
      Button blockButton = Button.builder(Component.translatable("phone.call.block"), b -> {
         UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         if (otherUuid == null) return;
         SimpleVoiceCallClient.config.addBlocked(otherUuid, null);
         ModNetworking.sendCallBlocked(otherUuid);

         SimpleVoiceCallClient.callManager.declineCall();
         this.onClose();
      }).size(buttonWidth, buttonHeight).pos(buttonX, buttonY + 2 * (buttonHeight + gap)).build();
      blockButton.active = SimpleVoiceCallClient.callManager.getOtherPlayerUuid() != null;
      this.addTexturedButton(blockButton);
   }

   public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTick) {
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
            Component callTitle = Component.translatable("phone.incoming.title");

            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.font, callTitle, centerX, frame.y() + frame.px(12), frame.px(170), 16776960
               );
            } catch (Throwable var18) {
            }
         }

         Component line = dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.incomingLineLabel();
         if (line != null) {
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext, this.font, line, centerX,
               frame.y() + frame.px(51), frame.px(160), 0xFFB8E4FF);
         }
         String callerName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
         if (callerName != null) {
            Component nameComponent = Component.literal(callerName);
            String trimmedName = this.font.plainSubstrByWidth(nameComponent.getString(), frame.px(92));
            int nameY = frame.y() + frame.px(38);
            int nameX = centerX - this.font.width(trimmedName) / 2;

            try {
               PhoneGuiTextures.drawText(drawContext, this.font, trimmedName, nameX + 1, nameY, 0xFF000000, false);
            } catch (Throwable var17) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.font, trimmedName, nameX - 1, nameY, 0xFF000000, false);
            } catch (Throwable var16) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.font, trimmedName, nameX, nameY + 1, 0xFF000000, false);
            } catch (Throwable var15) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.font, trimmedName, nameX, nameY - 1, 0xFF000000, false);
            } catch (Throwable var14) {
            }

            try {
               PhoneGuiTextures.drawText(drawContext, this.font, trimmedName, nameX, nameY, 0xFFFF5555, false);
            } catch (Throwable var13) {
            }
         }

         super.extractRenderState(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
         try {
            for (Button button : this.texturedButtons) {
               PhoneGuiTextures.drawButton(drawContext, this.font, button, mouseX, mouseY);
            }
         } catch (Throwable ignored) {
         }
      } catch (Throwable var19) {
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      PhoneClientActions.putAwayPhone();
      super.onClose();
   }
}



