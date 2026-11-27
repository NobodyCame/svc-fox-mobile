package dev.yukiinotenshi.simplephonepromax.compat;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;

/** Selects the number directory. Calls always use Fox Mobile's own transport. */
public final class CallStandard {
   private static boolean legacy;

   private CallStandard() {}

   public static void initialize() {
      legacy = isLegacyValue(SimpleVoiceCallClient.config.callStandard);
   }

   public static boolean canSwitch() {
      return !dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy()
         && !dev.yukiinotenshi.simplephonepromax.network.VoicemailClient.busy()
         && !dev.yukiinotenshi.simplephonepromax.gui.DialAttemptScreen.active()
         && !dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.queued()
         && !dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.active()
         && (SimpleVoiceCallClient.callManager == null || !SimpleVoiceCallClient.callManager.isInCall())
         && dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl.getOwnGroupId() == null
         && !dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasHeld()
         && dev.yukiinotenshi.simplephonepromax.network.CallControlService.incoming() == null;
   }

   public static boolean select(String standard) {
      boolean next = isLegacyValue(standard);
      if (next == legacy) return true;
      if (!canSwitch()) return false;
      legacy = next;
      SimpleVoiceCallClient.config.callStandard = next ? "legacy" : "fox";
      SimpleVoiceCallClient.config.save();
      return true;
   }

   public static boolean isLegacy() { return legacy; }
   public static String label(String standard) {
      return isLegacyValue(standard) ? "Legacy-номера" : "Fox Mobile";
   }
   public static boolean restartNeeded() { return legacy != isLegacyValue(SimpleVoiceCallClient.config.callStandard); }
   private static boolean isLegacyValue(String value) { return "legacy".equals(value) || "yoghurt".equals(value); }
}
