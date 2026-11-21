package dev.yukiinotenshi.simplephonepromax.phone;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen;
import dev.yukiinotenshi.simplephonepromax.gui.PhoneMainScreen;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class PhoneClientActions {
   private static final long SAME_ME_ACTION_COOLDOWN_MS = 1_000L;
   private static final long ANY_ME_ACTION_COOLDOWN_MS = 1_000L;
   private static final Map<String, Long> LAST_ME_ACTIONS = new HashMap<>();
   private static long lastAnyMeActionAt;

   private PhoneClientActions() {
   }

   public static void openPhone(Minecraft client, boolean announce) {
      if (client == null) {
         return;
      }

      if (announce) {
         sendMeAction("достал телефон");
      }

      if(dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy()){client.setScreenAndShow(new dev.yukiinotenshi.simplephonepromax.gui.PrivateCallScreen());return;}
      if(dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.active()||dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.hasIncoming()){client.setScreenAndShow(new dev.yukiinotenshi.simplephonepromax.gui.EncryptedCallScreen());return;}
      if (!dev.yukiinotenshi.simplephonepromax.network.CallbackRequests.inbox().isEmpty()) {
         client.setScreenAndShow(new dev.yukiinotenshi.simplephonepromax.gui.CallbackScreen());
         return;
      }
      if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.incoming() != null) {
         client.setScreenAndShow(new dev.yukiinotenshi.simplephonepromax.gui.CallOfferScreen());
         return;
      }
      CallState state = SimpleVoiceCallClient.callManager != null ? SimpleVoiceCallClient.callManager.getState() : CallState.NONE;
      if (state == CallState.INCOMING_RINGING || state == CallState.OUTGOING_RINGING || state == CallState.ACTIVE || state == CallState.BUSY) {
         client.setScreenAndShow(new ActiveCallScreen());
      } else if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasHeld()) {
         client.setScreenAndShow(new dev.yukiinotenshi.simplephonepromax.gui.HeldCallScreen());
      } else {
         client.setScreenAndShow(new PhoneMainScreen());
      }
   }

   public static synchronized void sendMeAction(String action) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null || client.getConnection() == null || action == null || action.isBlank()) {
            return;
         }

         String cleanAction = action.trim();
         long now = System.currentTimeMillis();
         Long lastSentAt = LAST_ME_ACTIONS.get(cleanAction);
         if (lastSentAt != null && now - lastSentAt < SAME_ME_ACTION_COOLDOWN_MS) {
            return;
         }

         if (now - lastAnyMeActionAt < ANY_ME_ACTION_COOLDOWN_MS) {
            return;
         }

         LAST_ME_ACTIONS.put(cleanAction, now);
         lastAnyMeActionAt = now;
         client.getConnection().sendCommand("me " + cleanAction);
      } catch (Throwable ignored) {
      }
   }

   public static void putAwayPhone() {
      dev.yukiinotenshi.simplephonepromax.sound.CallAnnouncement.stop();
      sendMeAction("убрал телефон");
   }
}

