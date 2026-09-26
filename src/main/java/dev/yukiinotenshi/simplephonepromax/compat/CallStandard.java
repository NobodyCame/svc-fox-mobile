package dev.yukiinotenshi.simplephonepromax.compat;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import net.fabricmc.loader.api.FabricLoader;

/** Only an idle phone can switch engines; a live call is never split across transports. */
public final class CallStandard {
   private static boolean legacy;
   public static void initialize() {
      legacy = isLegacyValue(SimpleVoiceCallClient.config.callStandard);
      if (legacy) LegacyCallBridge.initialize();
   }
   public static boolean canSwitch() {
      return !dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy() && !dev.yukiinotenshi.simplephonepromax.network.VoicemailClient.busy() && !dev.yukiinotenshi.simplephonepromax.gui.DialAttemptScreen.active() && !dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.queued() && !dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.active() && (SimpleVoiceCallClient.callManager==null||!SimpleVoiceCallClient.callManager.isInCall())
         &&dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl.getOwnGroupId()==null
         &&!dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasHeld()
         &&dev.yukiinotenshi.simplephonepromax.network.CallControlService.incoming()==null;
   }
   public static boolean select(String standard) {
      boolean next=isLegacyValue(standard);
      if(next==legacy)return true;
      if(!canSwitch()||next&&!originalInstalled())return false;
      if(next&&!LegacyCallBridge.ready())LegacyCallBridge.initialize();
      if(next&&!LegacyCallBridge.ready())return false;
      dev.yukiinotenshi.simplephonepromax.network.CallControlService.deactivate();
      dev.yukiinotenshi.simplephonepromax.network.VoiceCallTransport.onDisconnected();
      if(legacy){LegacyCallBridge.disconnect();LegacyCallBridge.manager("reset");}
      if(SimpleVoiceCallClient.soundManager!=null)SimpleVoiceCallClient.soundManager.stopRing();
      legacy=next;
      SimpleVoiceCallClient.callManager=next?new LegacyCallManager():new dev.yukiinotenshi.simplephonepromax.call.CallManager();
      SimpleVoiceCallClient.config.callStandard=next?"legacy":"fox";SimpleVoiceCallClient.config.save();
      return true;
   }
   public static boolean isLegacy() { return legacy; }
   public static boolean originalInstalled() { return FabricLoader.getInstance().isModLoaded("simple-voice-call"); }
   public static String label(String standard) { return isLegacyValue(standard) ? "Simple Voice Call (Legacy)" : "Fox Mobile"; }
   public static boolean restartNeeded() { return legacy != isLegacyValue(SimpleVoiceCallClient.config.callStandard); }
   private static boolean isLegacyValue(String value) { return "legacy".equals(value) || "yoghurt".equals(value); }
}

