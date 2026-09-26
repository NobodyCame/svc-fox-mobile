package dev.yukiinotenshi.simplephonepromax.network;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.call.CallManager;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDetector;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;

public final class VoiceCallTransport {
   private static final String CALL_PREFIX = "C";
   private static final String INVITE_PREFIX = "I";
   private static final String DECLINE_PREFIX = "D";
   private static final String BLOCK_PREFIX = "B";
   private static final String BUSY_PREFIX = "U";
   private static final Set<UUID> handledSignals = new HashSet<>();
   private static UUID outgoingTarget;
   private static UUID incomingGroup;
   private static String incomingGroupName;
   private static boolean incomingGroupHasPassword;
   private static UUID activeGroup;
   private static UUID createdGroup;
   private static boolean acceptingIncoming;
   private static UUID pendingJoinGroup;
   private static UUID pendingJoinTarget;
   private static String pendingJoinTargetName;
   private static int pendingJoinTicks;
   private static UUID pendingInviteReturnGroup;
   private static String pendingInviteReturnPassword;
   private static int pendingInviteReturnTicks;
   private static UUID pendingInviteCaller;
   private static String pendingInviteCallerName;
   private static int pendingInviteCallerTicks;
   private static int signalLeaveTicks;

   private VoiceCallTransport() {
   }

   public static boolean startCall(UUID target) {
      if (target != null && VoicechatPluginImpl.isReady()) {
         leaveGroup();
         outgoingTarget = target;
         incomingGroup = null;
         incomingGroupName = null;
         incomingGroupHasPassword = false;
         activeGroup = null;
         signalLeaveTicks = 0;
         String groupName = "C" + encodeUuid(target);
         SimpleVoiceCallClient.LOGGER.info("Phone transport target={} signal={}", target, groupName);
         return VoicechatPluginImpl.createGroup(groupName, groupPassword(groupName));
      } else {
         return false;
      }
   }

   public static boolean isPlayerBusy(UUID target) {
      PlayerState targetState = findState(target);
      if (targetState == null || targetState.getGroup() == null) {
         return false;
      }

      UUID ownGroup = VoicechatPluginImpl.getOwnGroupId();
      return ownGroup == null || !ownGroup.equals(targetState.getGroup());
   }

   public static boolean isPlayerInPhoneCall(UUID target) {
      PlayerState targetState = findState(target);
      if (targetState == null || targetState.getGroup() == null) {
         return false;
      }

      Group group = findGroup(targetState.getGroup());
      return group != null && isPhoneCallGroup(group.getName());
   }

   public static boolean isPlayerInActiveCallGroup(UUID target) {
      if (target == null) {
         return false;
      }

      UUID callGroup = activeGroup != null ? activeGroup : VoicechatPluginImpl.getOwnGroupId();
      if (callGroup == null) {
         return false;
      }

      PlayerState targetState = findState(target);
      return targetState != null && callGroup.equals(targetState.getGroup());
   }

   public static boolean joinPlayerCall(UUID target, String targetName) {
      // Joining someone else's call requires explicit consent signalling.
      return false;
   }

   public static boolean inviteToActiveCall(UUID target, String targetName) {
      return CallControlService.invite(target);
   }

   public static boolean playerInGroup(UUID player, UUID group) {
      PlayerState state = findState(player);
      return state != null && group != null && group.equals(state.getGroup());
   }

   public static void restoreCreatedGroup(UUID group) { createdGroup = group; }

   public static UUID getCreatedGroup() {
      return createdGroup != null && createdGroup.equals(VoicechatPluginImpl.getOwnGroupId()) ? createdGroup : null;
   }

   public static boolean isIncomingGroupFor(Group group, UUID target) {
      return group != null && (CALL_PREFIX + encodeUuid(target)).equals(group.getName());
   }

   public static boolean joinConsentedCall(UUID groupId, UUID peer, String name, boolean replaceCurrent) {
      Group group = findGroup(groupId);
      if (group == null || !playerInGroup(peer, groupId)) return false;
      if (SimpleVoiceCallClient.callManager.isInActiveCall() && !replaceCurrent) return false;
      if (replaceCurrent && SimpleVoiceCallClient.callManager.isInActiveCall()) {
         SimpleVoiceCallClient.callManager.endCall();
      }
      if (SimpleVoiceCallClient.callManager.isInCall()) return false;
      incomingGroup = groupId;
      incomingGroupName = group.getName();
      incomingGroupHasPassword = group.hasPassword();
      handledSignals.add(groupId);
      SimpleVoiceCallClient.callManager.startIncomingCall(name, peer);
      return acceptIncoming();
   }

   public static boolean acceptIncoming() {
      if (incomingGroup == null) {
         return false;
      }

      String password = incomingGroupHasPassword ? incomingGroupName : null;
      acceptingIncoming = VoicechatPluginImpl.joinGroup(incomingGroup, password);
      return acceptingIncoming;
   }

   public static void decline(UUID caller) {
      incomingGroup = null;
      incomingGroupName = null;
      incomingGroupHasPassword = false;
      activeGroup = null;
      acceptingIncoming = false;
      if (caller != null && VoicechatPluginImpl.isReady()) {
         String groupName = "D" + encodeUuid(caller);
         VoicechatPluginImpl.createGroup(groupName);
         signalLeaveTicks = 20;
      }
   }

   public static void block(UUID caller) {
      incomingGroup = null;
      incomingGroupName = null;
      incomingGroupHasPassword = false;
      activeGroup = null;
      acceptingIncoming = false;
      if (caller != null && VoicechatPluginImpl.isReady()) {
         String groupName = "B" + encodeUuid(caller);
         VoicechatPluginImpl.createGroup(groupName);
         signalLeaveTicks = 20;
      }
   }

   public static void busy(UUID caller) {
      incomingGroup = null;
      incomingGroupName = null;
      incomingGroupHasPassword = false;
      acceptingIncoming = false;
      if (caller != null && VoicechatPluginImpl.isReady()) {
         String groupName = BUSY_PREFIX + encodeUuid(caller);
         VoicechatPluginImpl.createGroup(groupName);
         signalLeaveTicks = 20;
      }
   }

   public static void leaveGroup() {
      createdGroup = null;
      if (VoicechatPluginImpl.getOwnGroupId() != null) {
         VoicechatPluginImpl.leaveGroup();
      }

      outgoingTarget = null;
      incomingGroup = null;
      incomingGroupName = null;
      incomingGroupHasPassword = false;
      activeGroup = null;
      acceptingIncoming = false;
      pendingJoinGroup = null;
      pendingJoinTarget = null;
      pendingJoinTargetName = null;
      pendingJoinTicks = 0;
      pendingInviteReturnGroup = null;
      pendingInviteReturnPassword = null;
      pendingInviteReturnTicks = 0;
      pendingInviteCaller = null;
      pendingInviteCallerName = null;
      pendingInviteCallerTicks = 0;
   }

   public static void onDisconnected() {
      createdGroup = null;
      outgoingTarget = null;
      incomingGroup = null;
      incomingGroupName = null;
      incomingGroupHasPassword = false;
      activeGroup = null;
      acceptingIncoming = false;
      pendingJoinGroup = null;
      pendingJoinTarget = null;
      pendingJoinTargetName = null;
      pendingJoinTicks = 0;
      pendingInviteReturnGroup = null;
      pendingInviteReturnPassword = null;
      pendingInviteReturnTicks = 0;
      pendingInviteCaller = null;
      pendingInviteCallerName = null;
      pendingInviteCallerTicks = 0;
      signalLeaveTicks = 0;
      handledSignals.clear();
   }

   public static void tick() {
      if(PrivateCalls.busy())return;
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && VoicechatPluginImpl.isReady()) {
         if (signalLeaveTicks > 0 && --signalLeaveTicks == 0) {
            UUID ownGroup = VoicechatPluginImpl.getOwnGroupId();
            if (isSignalGroup(ownGroup)) {
               VoicechatPluginImpl.leaveGroup();
            }
         }

         if (pendingJoinTicks > 0 && --pendingJoinTicks == 0) {
            pendingJoinGroup = null;
            pendingJoinTarget = null;
            pendingJoinTargetName = null;
         }

         if (pendingInviteCallerTicks > 0 && --pendingInviteCallerTicks == 0) {
            pendingInviteCaller = null;
            pendingInviteCallerName = null;
         }

         if (pendingInviteReturnTicks > 0 && --pendingInviteReturnTicks == 0) {
            UUID groupToReturn = pendingInviteReturnGroup;
            String password = pendingInviteReturnPassword;
            pendingInviteReturnGroup = null;
            pendingInviteReturnPassword = null;
            if (groupToReturn != null && !groupToReturn.equals(VoicechatPluginImpl.getOwnGroupId()) && groupExists(groupToReturn)) {
               VoicechatPluginImpl.joinGroup(groupToReturn, password);
            }
         }

         UUID self = client.player.getUuid();
         String incomingName = "C" + encodeUuid(self);
         String inviteName = INVITE_PREFIX + encodeUuid(self);
         String declineName = "D" + encodeUuid(self);
         String blockName = "B" + encodeUuid(self);
         String busyName = BUSY_PREFIX + encodeUuid(self);

         for (Group group : VoicechatPluginImpl.getGroups()) {
            if (!handledSignals.contains(group.getId())) {
               if (incomingName.equals(group.getName())) {
                  PlayerState caller = findPlayerInGroup(group.getId(), self);
                  if (caller != null) {
                     handledSignals.add(group.getId());
                     if (!SimpleVoiceCallClient.callManager.isInCall()) {
                        incomingGroup = group.getId();
                        incomingGroupName = group.getName();
                        incomingGroupHasPassword = group.hasPassword();
                        client.execute(() -> receiveIncoming(caller));
                     } else if (SimpleVoiceCallClient.callManager.isInActiveCall() && !CallControlService.supports(caller.getUuid(), "call_waiting_v1")) {
                        CallControlService.receiveLegacyWaiting(caller.getUuid(),caller.getName(),group.getId());
                     }
                  }
               } else if (inviteName.equals(group.getName())) {
                  PlayerState caller = findPlayerInGroup(group.getId(), self);
                  if (caller != null) {
                     handledSignals.add(group.getId());
                     if (!SimpleVoiceCallClient.callManager.isInCall()) {
                        pendingInviteCaller = caller.getUuid();
                        pendingInviteCallerName = caller.getName();
                        pendingInviteCallerTicks = 100;
                     } else if (SimpleVoiceCallClient.callManager.getState() == CallState.INCOMING_RINGING
                        || SimpleVoiceCallClient.callManager.getState() == CallState.BUSY) {
                        CallManager.addHistory(caller.getName(), caller.getUuid(), "busy_in", 0);
                     }
                  }
               } else if (declineName.equals(group.getName())) {
                  PlayerState decliner = findPlayerInGroup(group.getId(), self);
                  if (decliner != null && outgoingTarget != null && outgoingTarget.equals(decliner.getUuid())) {
                     handledSignals.add(group.getId());
                     leaveGroup();
                     client.execute(SimpleVoiceCallClient.callManager::remoteDecline);
                  }
               } else if (blockName.equals(group.getName())) {
                  PlayerState blocker = findPlayerInGroup(group.getId(), self);
                  if (blocker != null && outgoingTarget != null && outgoingTarget.equals(blocker.getUuid())) {
                     handledSignals.add(group.getId());
                     leaveGroup();
                     client.execute(SimpleVoiceCallClient.callManager::remoteBlocked);
                  }
               } else if (busyName.equals(group.getName())) {
                  PlayerState busyPlayer = findPlayerInGroup(group.getId(), self);
                  if (busyPlayer != null && outgoingTarget != null && outgoingTarget.equals(busyPlayer.getUuid())) {
                     handledSignals.add(group.getId());
                     leaveGroup();
                     client.execute(SimpleVoiceCallClient.callManager::remoteBusy);
                  }
               }
            }
         }

         if (pendingInviteCaller != null) {
            if (SimpleVoiceCallClient.callManager.isInCall()) {
               pendingInviteCaller = null;
               pendingInviteCallerName = null;
               pendingInviteCallerTicks = 0;
            } else {
               PlayerState caller = findState(pendingInviteCaller);
               if (caller != null && caller.getGroup() != null) {
                  Group invitedGroup = findGroup(caller.getGroup());
                  if (invitedGroup != null && isPhoneCallGroup(invitedGroup.getName())) {
                     incomingGroup = invitedGroup.getId();
                     incomingGroupName = invitedGroup.getName();
                     incomingGroupHasPassword = invitedGroup.hasPassword();
                     String callerName = pendingInviteCallerName != null ? pendingInviteCallerName : caller.getName();
                     UUID callerUuid = pendingInviteCaller;
                     pendingInviteCaller = null;
                     pendingInviteCallerName = null;
                     pendingInviteCallerTicks = 0;
                     client.execute(() -> receiveIncoming(callerName, callerUuid));
                  }
               }
            }
         }

         UUID ownGroup = VoicechatPluginImpl.getOwnGroupId();
         if (acceptingIncoming && incomingGroup != null && incomingGroup.equals(ownGroup)) {
            activeGroup = incomingGroup;
            acceptingIncoming = false;
            client.execute(() -> {
               SimpleVoiceCallClient.callManager.acceptCall();
               if (!(client.currentScreen instanceof ActiveCallScreen)) {
                  client.setScreen(new ActiveCallScreen());
               }
            });
         }

         if (pendingJoinGroup != null && pendingJoinGroup.equals(ownGroup)) {
            activeGroup = pendingJoinGroup;
            UUID joinedGroup = pendingJoinGroup;
            UUID joinedTarget = pendingJoinTarget;
            String joinedName = pendingJoinTargetName;
            pendingJoinGroup = null;
            pendingJoinTarget = null;
            pendingJoinTargetName = null;
            pendingJoinTicks = 0;
            client.execute(() -> {
               SimpleVoiceCallClient.callManager.joinGroupCall("Групповой звонок: " + (joinedName != null ? joinedName : ""), joinedTarget, joinedGroup);
               if (!(client.currentScreen instanceof ActiveCallScreen)) {
                  client.setScreen(new ActiveCallScreen());
               }
            });
         }

         if (outgoingTarget != null && ownGroup != null) {
            if (isIncomingGroupFor(findGroup(ownGroup), outgoingTarget)) createdGroup = ownGroup;
            PlayerState target = findState(outgoingTarget);
            if (target != null && ownGroup.equals(target.getGroup())) {
               activeGroup = ownGroup;
               UUID acceptedBy = outgoingTarget;
               outgoingTarget = null;
               client.execute(() -> {
                  SimpleVoiceCallClient.callManager.acceptCall(acceptedBy, self, true);
                  if (!(client.currentScreen instanceof ActiveCallScreen)) {
                     client.setScreen(new ActiveCallScreen());
                  }
               });
            } else if (target != null && target.getGroup() != null && !ownGroup.equals(target.getGroup())
               && CallControlService.canWait(outgoingTarget)) {
               CallControlService.ensureWaiting(outgoingTarget, ownGroup);
            } else if (target != null && target.getGroup() != null && !ownGroup.equals(target.getGroup())) {
               leaveGroup();
               client.execute(SimpleVoiceCallClient.callManager::remoteBusy);
            }
         }

         if (SimpleVoiceCallClient.callManager.isInActiveCall() && activeGroup != null) {
            UUID other = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
            if (other != null && !playerInGroup(other, activeGroup)) {
               PlayerState remaining = findPlayerInGroup(activeGroup, self);
               if (remaining != null) SimpleVoiceCallClient.callManager.updateTransferredPeer(remaining.getName(), remaining.getUuid());
            }
            if (!groupExists(activeGroup) || (!groupHasOther(activeGroup, self) && !CallControlService.keepsGroup(activeGroup))) {
               leaveGroup();
               client.execute(SimpleVoiceCallClient.callManager::remoteHangup);
            }
         }

         if (SimpleVoiceCallClient.callManager.getState() == CallState.INCOMING_RINGING && incomingGroup != null && !groupExists(incomingGroup)) {
            incomingGroup = null;
            client.execute(SimpleVoiceCallClient.callManager::remoteCancelIncoming);
         }
      }
   }

   private static void receiveIncoming(PlayerState caller) {
      if (caller != null) {
         receiveIncoming(caller.getName(), caller.getUuid());
      }
   }

   private static void receiveIncoming(String callerName, UUID callerUuid) {
      if(!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.allows(callerUuid,false)||!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.admit(callerUuid,"ordinary:"+incomingGroup)) {
         CallManager.addHistory(callerName,callerUuid,"dnd",0);decline(callerUuid);return;
      }
      if (callerUuid == null) {
         return;
      }

      String name = callerName != null && !callerName.isBlank() ? callerName : callerUuid.toString();
      if (SimpleVoiceCallClient.callManager.isInCall()) {
         CallState state = SimpleVoiceCallClient.callManager.getState();
         if (state == CallState.INCOMING_RINGING || state == CallState.BUSY) {
            CallManager.addHistory(name, callerUuid, "busy_in", 0);
            busy(callerUuid);
         }

         return;
      }

      if (!SimpleVoiceCallClient.callManager.isInCall()) {
         if (!PhoneDetector.hasPhone(MinecraftClient.getInstance())) {
            CallManager.addHistory(name, callerUuid, "missed_in", 0);
            decline(callerUuid);
            return;
         }

         if (SimpleVoiceCallClient.config != null
            && (SimpleVoiceCallClient.config.isBlocked(callerUuid) || SimpleVoiceCallClient.config.isBlocked(name))) {
            CallManager.addHistory(name, callerUuid, "blocked_in", 0);
            SimpleVoiceCallClient.callManager.sendBlockedMessage(name);
            block(callerUuid);
            return;
         }

         SimpleVoiceCallClient.callManager.startIncomingCall(name, callerUuid);
      }
   }

   private static PlayerState findPlayerInGroup(UUID group, UUID excluded) {
      for (PlayerState state : VoicechatPluginImpl.getPlayerStates()) {
         if (!excluded.equals(state.getUuid()) && group.equals(state.getGroup())) {
            return state;
         }
      }

      return null;
   }

   public static PlayerState findState(UUID uuid) {
      if (uuid == null) {
         return null;
      }

      for (PlayerState state : VoicechatPluginImpl.getPlayerStates()) {
         if (uuid.equals(state.getUuid())) {
            return state;
         }
      }

      return null;
   }

   public static Group findGroup(UUID id) {
      if (id == null) {
         return null;
      }

      for (Group group : VoicechatPluginImpl.getGroups()) {
         if (id.equals(group.getId())) {
            return group;
         }
      }

      return null;
   }

   private static boolean groupExists(UUID id) {
      for (Group group : VoicechatPluginImpl.getGroups()) {
         if (id.equals(group.getId())) {
            return true;
         }
      }

      return false;
   }

   private static boolean groupHasOther(UUID id, UUID self) {
      if (id == null || self == null) {
         return false;
      }

      for (PlayerState state : VoicechatPluginImpl.getPlayerStates()) {
         if (!self.equals(state.getUuid()) && id.equals(state.getGroup())) {
            return true;
         }
      }

      return false;
   }

   private static boolean isSignalGroup(UUID id) {
      if (id == null) {
         return false;
      }

      for (Group group : VoicechatPluginImpl.getGroups()) {
         if (id.equals(group.getId())) {
            String name = group.getName();
            return name != null
               && (name.startsWith("D") || name.startsWith("B") || name.startsWith(BUSY_PREFIX) || name.startsWith(INVITE_PREFIX));
         }
      }

      return false;
   }

   private static boolean isPhoneCallGroup(String name) {
      return name != null && name.length() == 23 && name.startsWith(CALL_PREFIX);
   }

   private static String inviteSignalName(UUID target) {
      return INVITE_PREFIX + encodeUuid(target);
   }

   private static String encodeUuid(UUID uuid) {
      ByteBuffer buffer = ByteBuffer.allocate(16);
      buffer.putLong(uuid.getMostSignificantBits());
      buffer.putLong(uuid.getLeastSignificantBits());
      return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
   }

   private static String groupPassword(String groupName) {
      return !SimpleVoiceCallClient.config.passwordProtectedCalls ? null : groupName;
   }
}



