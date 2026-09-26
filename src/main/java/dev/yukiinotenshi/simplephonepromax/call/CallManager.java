package dev.yukiinotenshi.simplephonepromax.call;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen;
import dev.yukiinotenshi.simplephonepromax.gui.IncomingCallScreen;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.network.VoiceCallTransport;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class CallManager {
   private static final long OUTGOING_COOLDOWN_MS = 3000L;
   private static final long BUSY_SCREEN_MS = 6000L;
   private CallState state;
   private String otherPlayerName;
   private UUID otherPlayerUuid;
   private long callStartTimeMs;
   private long ringingStartedAtMs;
   private UUID callGroupId;
   private boolean incomingDirection;
   private long lastOutgoingAttemptMs;

   public CallManager() {
      this.reset();
   }

   public synchronized void reset() {
      this.state = CallState.NONE;
      this.otherPlayerName = null;
      this.otherPlayerUuid = null;
      this.callStartTimeMs = 0L;
      this.ringingStartedAtMs = 0L;
      this.callGroupId = null;
      this.incomingDirection = false;
   }

   public CallState getState() {
      return this.state;
   }

   private static void closeCallScreen() {
      try {
         MinecraftClient c = MinecraftClient.getInstance();
         if (c != null && (c.currentScreen instanceof ActiveCallScreen || c.currentScreen instanceof IncomingCallScreen)) {
            c.setScreen(null);
         }
      } catch (Throwable var1) {
      }
   }

   public void tick() {
      long now = System.currentTimeMillis();
      if (this.state == CallState.OUTGOING_RINGING && this.ringingStartedAtMs > 0L && now - this.ringingStartedAtMs >= 45000L) {
         UUID other = this.otherPlayerUuid;
         String otherN = this.otherPlayerName;
         UUID me = null;

         try {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c != null && c.player != null) {
               me = c.player.getUuid();
            }
         } catch (Throwable var12) {
         }

         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var11) {
         }

         addHistory(otherN, other, "missed_out", 0);
         sendMsg("Нет ответа");
         if (other != null && me != null) {
            try {
               ModNetworking.sendCallHangup(me, other);
            } catch (Throwable var10) {
            }
         }

         this.reset();
         closeCallScreen();
      }

      if (this.state == CallState.INCOMING_RINGING && this.ringingStartedAtMs > 0L && now - this.ringingStartedAtMs >= 45000L) {
         UUID other = this.otherPlayerUuid;
         String otherN = this.otherPlayerName;
         UUID me = null;

         try {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c != null && c.player != null) {
               me = c.player.getUuid();
            }
         } catch (Throwable var9) {
         }

         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var8) {
         }

         addHistory(otherN, other, "missed_in", 0);
         sendMsg("Пропущенный вызов");
         if (other != null && me != null) {
            try {
               ModNetworking.sendCallDecline(me, other);
            } catch (Throwable var7) {
            }
         }

         this.reset();
         closeCallScreen();
      }

      if (this.state == CallState.BUSY && this.ringingStartedAtMs > 0L && now - this.ringingStartedAtMs >= BUSY_SCREEN_MS) {
         this.reset();
         closeCallScreen();
      }
   }

   public boolean isInCall() {
      return this.state == CallState.ACTIVE
         || this.state == CallState.INCOMING_RINGING
         || this.state == CallState.OUTGOING_RINGING
         || this.state == CallState.BUSY;
   }

   public boolean isInActiveCall() {
      return this.state == CallState.ACTIVE;
   }

   public boolean isInRingingCall() {
      return this.state == CallState.INCOMING_RINGING || this.state == CallState.OUTGOING_RINGING;
   }

   public String getOtherPlayerName() {
      return this.otherPlayerName;
   }

   public UUID getOtherPlayerUuid() {
      return this.otherPlayerUuid;
   }

   public UUID getCallGroupId() {
      return this.callGroupId;
   }

   public long getCallDurationMs() {
      return this.callStartTimeMs == 0L ? 0L : System.currentTimeMillis() - this.callStartTimeMs;
   }

   public long getRingingStartedAtMs() {
      return this.ringingStartedAtMs;
   }

   public synchronized boolean startOutgoingCall(String targetName, UUID targetUuid) {
      if(dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy()||dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.active())return false;
      long now = System.currentTimeMillis();
      if (now - this.lastOutgoingAttemptMs < OUTGOING_COOLDOWN_MS) {
         sendMsg("Повторите вызов позже");
         return false;
      }

      if (targetUuid == null) {
         sendMsg("Номер не найден");
         return false;
      }

      if (SimpleVoiceCallClient.config != null && (SimpleVoiceCallClient.config.isBlocked(targetUuid) || SimpleVoiceCallClient.config.isBlocked(targetName))) {
         sendMsg("Абонент заблокирован");
         return false;
      }

      if(dev.yukiinotenshi.simplephonepromax.network.CallControlService.rejectsCallForDoNotDisturb(targetUuid)) {
         this.lastOutgoingAttemptMs=now;
         addHistory(targetName,targetUuid,"dnd_remote",0);
         sendMsg("Абонент включил режим «Не беспокоить»");
         return false;
      }

      if (this.state != CallState.NONE) {
         sendMsg("Линия занята");
         return false;
      } else if (ModNetworking.isPlayerBusy(targetUuid) && !dev.yukiinotenshi.simplephonepromax.network.CallControlService.canWait(targetUuid)) {
         this.lastOutgoingAttemptMs = now;
         this.showBusy(targetName, targetUuid);
         return false;
      } else {
         this.lastOutgoingAttemptMs = now;
         this.state = CallState.OUTGOING_RINGING;
         this.otherPlayerName = targetName;
         this.otherPlayerUuid = targetUuid;
         this.callStartTimeMs = 0L;
         this.ringingStartedAtMs = System.currentTimeMillis();
         this.callGroupId = null;
         this.incomingDirection = false;

         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.playRing(true);
            }
         } catch (Throwable var4) {
         }

         PhoneClientActions.sendMeAction("начал(а) кому-то звонить");
         sendMsg("\ud83d\udcde Исходящий — " + (targetName != null ? targetName : "") + "");
         return true;
      }
   }

   private synchronized void showBusy(String targetName, UUID targetUuid) {
      try {
         if (SimpleVoiceCallClient.soundManager != null) {
            SimpleVoiceCallClient.soundManager.stopRing();
         }
      } catch (Throwable var3) {
      }

      VoiceCallTransport.leaveGroup();
      this.state = CallState.BUSY;
      this.otherPlayerName = targetName;
      this.otherPlayerUuid = targetUuid;
      this.callStartTimeMs = 0L;
      this.ringingStartedAtMs = System.currentTimeMillis();
      this.callGroupId = null;
      this.incomingDirection = false;
      addHistory(targetName, targetUuid, "busy_out", 0);
      sendMsg("Абонент занят");
   }

   public synchronized void startIncomingCall(String callerName, UUID callerUuid) {
      if(dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy())return;
      if (!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.allows(callerUuid,false)) { addHistory(callerName,callerUuid,"dnd",0); return; }
      if (SimpleVoiceCallClient.config != null && (SimpleVoiceCallClient.config.isBlocked(callerUuid) || SimpleVoiceCallClient.config.isBlocked(callerName))) {
         sendBlockedMessage(callerName);
         return;
      }

      if (this.state != CallState.ACTIVE && this.state != CallState.INCOMING_RINGING) {
         this.state = CallState.INCOMING_RINGING;
         this.otherPlayerName = callerName;
         this.otherPlayerUuid = callerUuid;
         this.callStartTimeMs = 0L;
         this.ringingStartedAtMs = System.currentTimeMillis();
         this.callGroupId = null;
         this.incomingDirection = true;

         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.playRing(true);
            }
         } catch (Throwable var4) {
         }

         sendMsg("\ud83d\udcde Входящий звонок от " + (callerName != null ? callerName : ""));
      }
   }

   public synchronized void acceptCall() {
      this.acceptCall(null, null, false);
   }

   public synchronized void joinGroupCall(String displayName, UUID anchorUuid, UUID groupId) {
      this.state = CallState.ACTIVE;
      this.otherPlayerName = displayName != null && !displayName.isBlank() ? displayName : "Групповой звонок";
      this.otherPlayerUuid = anchorUuid;
      this.callStartTimeMs = System.currentTimeMillis();
      this.ringingStartedAtMs = 0L;
      this.callGroupId = groupId;
      this.incomingDirection = false;

      try {
         if (SimpleVoiceCallClient.soundManager != null) {
            SimpleVoiceCallClient.soundManager.stopRing();
         }
      } catch (Throwable var4) {
      }

      sendMsg("Группа: " + this.otherPlayerName);
   }

   public synchronized void acceptCall(UUID callerUuid, UUID targetUuid, boolean fromNetwork) {
      if (this.state != CallState.INCOMING_RINGING && this.state != CallState.OUTGOING_RINGING && this.state != CallState.ACTIVE) {
         this.reset();
      } else if (this.state != CallState.ACTIVE) {
         CallState previousState = this.state;
         if (fromNetwork
            && this.state == CallState.OUTGOING_RINGING
            && callerUuid != null
            && (this.otherPlayerUuid == null || !this.otherPlayerUuid.equals(callerUuid))) {
            this.otherPlayerUuid = callerUuid;
         }

         this.state = CallState.ACTIVE;
         this.callStartTimeMs = System.currentTimeMillis();
         this.ringingStartedAtMs = 0L;
         this.callGroupId = VoicechatPluginImpl.getOwnGroupId();

         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var5) {
         }

         sendMsg("На связи: " + (this.otherPlayerName != null ? this.otherPlayerName : ""));
         if (previousState == CallState.INCOMING_RINGING) {
            PhoneClientActions.sendMeAction("принял вызов");
         }
      }
   }

   public synchronized void declineCall() {
      if (this.state == CallState.INCOMING_RINGING) {
         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var2) {
         }

         addHistory(this.otherPlayerName, this.otherPlayerUuid, "declined_in", 0);
         sendMsg("Вызов отклонён");
         PhoneClientActions.sendMeAction("сбросил вызов");
         this.reset();
         closeCallScreen();
      } else if (this.state == CallState.OUTGOING_RINGING) {
         addHistory(this.otherPlayerName, this.otherPlayerUuid, "declined_out", 0);
         sendMsg("Вызов отменён");
         this.reset();
         closeCallScreen();
      }
   }

   public synchronized void remoteDecline() {
      if (this.state == CallState.OUTGOING_RINGING) {
         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var2) {
         }

         addHistory(this.otherPlayerName, this.otherPlayerUuid, "declined_out", 0);
         sendMsg("Вызов отклонён");
         this.reset();
         closeCallScreen();
      }
   }

   public synchronized void remoteBlocked() {
      if (this.state == CallState.OUTGOING_RINGING) {
         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var2) {
         }

         addHistory(this.otherPlayerName, this.otherPlayerUuid, "declined_out", 0);
         sendMsg("Вызов недоступен");
         this.reset();
         closeCallScreen();
      }
   }

   public synchronized void remoteBusy() {
      if (this.state == CallState.OUTGOING_RINGING) {
         this.showBusy(this.otherPlayerName, this.otherPlayerUuid);
      }
   }

   public synchronized void sendBlockedMessage(String callerName) {
      sendMsg("Заблокированный вызов" + (callerName != null && !callerName.isBlank() ? ": " + callerName : ""));
   }

   public synchronized void remoteCancelIncoming() {
      if (this.state == CallState.INCOMING_RINGING) {
         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var2) {
         }

         sendMsg("\ud83d\udcf4 Звонящий отменил вызов");
         addHistory(this.otherPlayerName, this.otherPlayerUuid, "missed_in", 0);
         this.reset();
         closeCallScreen();
      }
   }

   public synchronized void connectionLost() {
      VoicechatPluginImpl.setMicrophoneMuted(false);
      if (this.isInCall()) {
         try {
            if (SimpleVoiceCallClient.soundManager != null) {
               SimpleVoiceCallClient.soundManager.stopRing();
            }
         } catch (Throwable var2) {
         }

         sendMsg("\ud83d\udcf4 Связь с Simple Voice Chat потеряна");
         this.reset();
         closeCallScreen();
      }
   }

   public synchronized void remoteHangup() {
      VoicechatPluginImpl.setMicrophoneMuted(false);
      if (this.state == CallState.ACTIVE) {
         long duration = this.callStartTimeMs > 0L ? (System.currentTimeMillis() - this.callStartTimeMs) / 1000L : 0L;
         addHistory(this.otherPlayerName, this.otherPlayerUuid, this.incomingDirection ? "completed_in" : "completed_out", (int)duration);
         sendMsg("\ud83d\udcf4 Собеседник завершил звонок");
         this.reset();
         closeCallScreen();
      }
   }

   public synchronized void updateTransferredPeer(String name, UUID uuid) {
      if (this.state == CallState.ACTIVE) {
         this.otherPlayerName = name;
         this.otherPlayerUuid = uuid;
      }
   }

   public synchronized void endCall() {
      VoicechatPluginImpl.setMicrophoneMuted(false);
      long dur = 0L;
      boolean wasActive = this.state == CallState.ACTIVE && this.callStartTimeMs > 0L;
      if (wasActive) {
         dur = (System.currentTimeMillis() - this.callStartTimeMs) / 1000L;
      }

      try {
         if (SimpleVoiceCallClient.soundManager != null) {
            SimpleVoiceCallClient.soundManager.stopRing();
         }
      } catch (Throwable var8) {
      }

      VoiceCallTransport.leaveGroup();
      if (wasActive) {
         long mm = dur / 60L;
         long ss = dur % 60L;
         addHistory(this.otherPlayerName, this.otherPlayerUuid, this.incomingDirection ? "completed_in" : "completed_out", (int)dur);
         sendMsg("\ud83d\udcf4 Разговор: " + mm + ":" + (ss < 10L ? "0" + ss : ss));
         PhoneClientActions.sendMeAction("закончил разговор");
      } else if (this.state == CallState.OUTGOING_RINGING) {
         addHistory(this.otherPlayerName, this.otherPlayerUuid, "canceled_out", 0);
         sendMsg("\ud83d\udcf4 Исходящий звонок отменён");
      } else if (this.state == CallState.INCOMING_RINGING) {
         addHistory(this.otherPlayerName, this.otherPlayerUuid, "canceled_in", 0);
         sendMsg("\ud83d\udcf4 Входящий звонок сброшен");
         PhoneClientActions.sendMeAction("сбросил вызов");
      } else if (this.state == CallState.BUSY) {
         sendMsg("Вызов завершён");
      }

      this.reset();
      closeCallScreen();
   }

   private static void sendMsg(String m) {
      try {
         MinecraftClient c = MinecraftClient.getInstance();
         if (c != null && c.player != null && m != null && !m.isBlank()) {
            dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages.show(m);
         }
      } catch (Throwable ignored) {
      }
   }

   public static void addHistory(String name, UUID uuid, String type, int durationSec) {
      try {
         ModConfig.CallHistoryEntry entry = new ModConfig.CallHistoryEntry();
         entry.server = dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current();
         entry.dialedNumber = type.endsWith("_out") ? dev.yukiinotenshi.simplephonepromax.phone.PhoneDialer.lastDialedFor(uuid) : null;
         entry.name = name;
         entry.uuid = uuid != null ? uuid.toString() : null;
         entry.number = uuid != null ? PhoneNumberManager.getDisplayNumberFor(uuid) : null;
         entry.type = type;
         entry.durationSec = durationSec;
         entry.timestampMs = System.currentTimeMillis();
         var history=SimpleVoiceCallClient.config.callHistory;
         if(type.startsWith("missed")&&!history.isEmpty()){
            var previous=history.get(0);
            if(java.util.Objects.equals(previous.uuid,entry.uuid)&&java.util.Objects.equals(previous.server,entry.server)&&java.util.Objects.equals(previous.type,type)&&entry.timestampMs-previous.timestampMs<300000){entry.count=Math.max(1,previous.count)+1;history.remove(0);}
         }
         history.add(0, entry);

         while (SimpleVoiceCallClient.config.callHistory.size() > 100) {
            SimpleVoiceCallClient.config.callHistory.remove(SimpleVoiceCallClient.config.callHistory.size() - 1);
         }

         SimpleVoiceCallClient.config.save();
      } catch (Throwable var5) {
      }
   }
}



