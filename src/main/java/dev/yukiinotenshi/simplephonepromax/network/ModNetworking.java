package dev.yukiinotenshi.simplephonepromax.network;

import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import dev.yukiinotenshi.simplephonepromax.compat.LegacyCallBridge;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModNetworking {
   public static final Set<UUID> playersWithMod = ConcurrentHashMap.newKeySet();
   public static final ConcurrentHashMap<UUID, String> playerPhoneNumbers = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<String, UUID> phoneNumberToUuid = new ConcurrentHashMap<>();
   private static int tickCounter;

   public static void register() {
   }

   public static void tick() {
      if (CallStandard.isLegacy()) return; // Original ticks are driven by LegacyCallManager.
      if (++tickCounter % 20 == 0) {
         broadcastIdentify();
      }

      CallControlService.tick();
      VoiceCallTransport.tick();
   }

   public static void broadcastIdentify() {
      playersWithMod.clear();

      for (PlayerState state : VoicechatPluginImpl.getPlayerStates()) {
         playersWithMod.add(state.getUuid());
         PhoneNumberManager.registerPlayer(state.getUuid(), null);
      }
   }

   public static boolean hasMod(UUID uuid) {
      return playersWithMod.contains(uuid);
   }

   public static boolean isPlayerBusy(UUID uuid) {
      return VoiceCallTransport.isPlayerBusy(uuid);
   }

   public static boolean isPlayerInPhoneCall(UUID uuid) {
      return VoiceCallTransport.isPlayerInPhoneCall(uuid);
   }

   public static boolean isPlayerInActiveCallGroup(UUID uuid) {
      return VoiceCallTransport.isPlayerInActiveCallGroup(uuid);
   }

   public static boolean joinPlayerCall(UUID uuid, String name) {
      return VoiceCallTransport.joinPlayerCall(uuid, name);
   }

   public static boolean inviteToActiveCall(UUID uuid, String name) {
      return VoiceCallTransport.inviteToActiveCall(uuid, name);
   }

   public static void requestPlayerPresence(UUID uuid) {
      if (uuid != null) {
         playersWithMod.add(uuid);
      }
   }

   public static void registerPhoneNumber(UUID uuid, String number) {
      if (uuid != null && number != null) {
         String digits = PhoneNumberManager.onlyDigits(number);
         if (PhoneNumberManager.isValidKnownNumber(digits)) {
            String old = playerPhoneNumbers.put(uuid, digits);
            if (old != null && !old.equals(digits)) {
               phoneNumberToUuid.remove(old, uuid);
            }

            phoneNumberToUuid.put(digits, uuid);
         }
      }
   }

   public static UUID findUuidByPhoneNumber(String number) {
      return number == null ? null : phoneNumberToUuid.get(PhoneNumberManager.onlyDigits(number));
   }

   public static void sendCallRequest(UUID targetUuid, String targetName) {
      if (CallStandard.isLegacy()) { LegacyCallBridge.original("network.ModNetworking", "sendCallRequest", targetUuid, targetName); return; }
      if (!VoiceCallTransport.startCall(targetUuid)) {
         dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient.callManager.endCall();
         dev.yukiinotenshi.simplephonepromax.sound.OperatorSounds.unavailable();
      }
   }

   public static void sendCallAccept(UUID callerUuid, UUID targetUuid) {
      if (CallStandard.isLegacy()) { LegacyCallBridge.original("network.ModNetworking", "sendCallAccept", callerUuid, targetUuid); return; }
      VoiceCallTransport.acceptIncoming();
   }

   public static void sendCallDecline(UUID callerUuid, UUID targetUuid) {
      if (CallStandard.isLegacy()) { LegacyCallBridge.original("network.ModNetworking", "sendCallDecline", callerUuid, targetUuid); return; }
      VoiceCallTransport.decline(targetUuid);
   }

   public static void sendCallBlocked(UUID callerUuid) {
      if (CallStandard.isLegacy()) { LegacyCallBridge.original("network.ModNetworking", "sendCallDecline", callerUuid, callerUuid); return; }
      VoiceCallTransport.block(callerUuid);
   }

   public static void sendCallHangup(UUID callerUuid, UUID targetUuid) {
      if (CallStandard.isLegacy()) { LegacyCallBridge.original("network.ModNetworking", "sendCallHangup", callerUuid, targetUuid); return; }
      VoiceCallTransport.leaveGroup();
   }

   public static void sendGroupInvite(UUID targetUuid, String groupId) {
      if (CallStandard.isLegacy()) { LegacyCallBridge.original("network.ModNetworking", "sendGroupInvite", targetUuid, groupId); return; }
   }

   public static void onVoicechatDisconnected() {
      if (CallStandard.isLegacy()) { net.minecraft.client.MinecraftClient.getInstance().execute(LegacyCallBridge::disconnect); return; }
      net.minecraft.client.MinecraftClient.getInstance().execute(CallControlService::reset);
      VoiceCallTransport.onDisconnected();
   }
}



