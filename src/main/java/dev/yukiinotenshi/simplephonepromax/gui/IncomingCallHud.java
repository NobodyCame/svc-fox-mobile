package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class IncomingCallHud {
   private IncomingCallHud() {
   }

   public static void render(GuiGraphicsExtractor context) {
      try {
         PhoneNotification.render(context);
         var offer = dev.yukiinotenshi.simplephonepromax.network.CallControlService.incoming();
         if (offer != null) {
            Minecraft c = Minecraft.getInstance();
            if (c.player != null && PhoneDetector.hasPhone(c)) {
               PhoneGuiTextures.drawIncomingToast(context, c.font, offer.sourceName(), dev.yukiinotenshi.simplephonepromax.network.CallControlService.incomingAt());
            }
            return;
         }
         if (SimpleVoiceCallClient.callManager == null || SimpleVoiceCallClient.callManager.getState() != CallState.INCOMING_RINGING) {
            return;
         }

         Minecraft client = Minecraft.getInstance();
         if (client == null || client.font == null) {
            return;
         }

         if (!PhoneDetector.hasPhone(client)) {
            return;
         }

         PhoneGuiTextures.drawIncomingToast(
            context,
            client.font,
            SimpleVoiceCallClient.callManager.getOtherPlayerName(),
            SimpleVoiceCallClient.callManager.getRingingStartedAtMs()
         );
      } catch (Throwable ignored) {
      }
   }
}

