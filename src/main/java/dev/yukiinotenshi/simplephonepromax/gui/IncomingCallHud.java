package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class IncomingCallHud {
   private IncomingCallHud() {
   }

   public static void render(DrawContext context) {
      try {
         PhoneNotification.render(context);
         var offer = dev.yukiinotenshi.simplephonepromax.network.CallControlService.incoming();
         if (offer != null) {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.player != null && PhoneDetector.hasPhone(c)) {
               PhoneGuiTextures.drawIncomingToast(context, c.textRenderer, offer.sourceName(), dev.yukiinotenshi.simplephonepromax.network.CallControlService.incomingAt());
            }
            return;
         }
         if (SimpleVoiceCallClient.callManager == null || SimpleVoiceCallClient.callManager.getState() != CallState.INCOMING_RINGING) {
            return;
         }

         MinecraftClient client = MinecraftClient.getInstance();
         if (client == null || client.textRenderer == null) {
            return;
         }

         if (!PhoneDetector.hasPhone(client)) {
            return;
         }

         PhoneGuiTextures.drawIncomingToast(
            context,
            client.textRenderer,
            SimpleVoiceCallClient.callManager.getOtherPlayerName(),
            SimpleVoiceCallClient.callManager.getRingingStartedAtMs()
         );
      } catch (Throwable ignored) {
      }
   }
}

