package dev.yukiinotenshi.simplephonepromax;

import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.Group.Type;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.net.ClientServerNetManager;
import de.maxhenkel.voicechat.net.CreateGroupPacket;
import de.maxhenkel.voicechat.net.JoinGroupPacket;
import de.maxhenkel.voicechat.net.LeaveGroupPacket;
import de.maxhenkel.voicechat.plugins.impl.ClientGroupImpl;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.MicThread;
import de.maxhenkel.voicechat.voice.common.ClientGroup;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoicechatPluginImpl implements VoicechatPlugin {
   public static VoicechatApi INSTANCE;
   private static final Logger LOGGER = LoggerFactory.getLogger("Fox Mobile");

   public String getPluginId() {
      return "svc-fox-mobile";
   }

   public void initialize(VoicechatApi api) {
      INSTANCE = api;
      LOGGER.info("Voicechat API initialized");
   }

   public void registerEvents(EventRegistration registration) {
      registration.registerEvent(ClientVoicechatConnectionEvent.class, this::onConnection);
      registration.registerEvent(de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent.class,e->dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.initialize(e.getVoicechat()));
      registration.registerEvent(de.maxhenkel.voicechat.api.events.ClientSoundEvent.class,event->{dev.yukiinotenshi.simplephonepromax.network.VoicemailClient.capture(event);dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.capture(event);});
   }

   private void onConnection(ClientVoicechatConnectionEvent event) {
      dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.initialize(event.getVoicechat());
      LOGGER.info("Voicechat connection state: " + event.isConnected());
      if(event.isConnected()&&!dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.active()&&!SimpleVoiceCallClient.callManager.isInCall())setMicrophoneMuted(false);
      if (!event.isConnected()) {
         ModNetworking.onVoicechatDisconnected();
         MinecraftClient.getInstance().execute(SimpleVoiceCallClient.callManager::connectionLost);
      }
   }

   public static boolean createGroup(String name) {
      return createGroup(name, null);
   }

   public static boolean createGroup(String name, String password) {
      try {
         ClientServerNetManager.sendToServer(new CreateGroupPacket(name, password, Type.OPEN));
         return true;
      } catch (Throwable e) {
         LOGGER.error("Could not create voice call group", e);
         return false;
      }
   }

   public static boolean createPrivateGroup(String name,String password) {
      // NORMAL keeps the conversation private from nearby outsiders while allowing
      // group members to hear nearby non-members (for the SVC ambient-audio behavior).
      try { ClientServerNetManager.sendToServer(new CreateGroupPacket(name,svcPassword(password),Type.NORMAL));return true; }catch(Throwable e){LOGGER.warn("Could not create private voice group (SVC group name/password limit is 24 characters)",e);return false;}
   }

   /** SVC 2.6.x encodes both group names and passwords with a 24-character cap. */
   private static String svcPassword(String password) {
      if(password==null||password.length()<=24)return password;
      return password.substring(0,24);
   }

   public static boolean joinGroup(UUID groupId) {
      return joinGroup(groupId, null);
   }

   public static boolean joinGroup(UUID groupId, String password) {
      if (groupId == null) {
         return false;
      }

      try {
         ClientServerNetManager.sendToServer(new JoinGroupPacket(groupId, svcPassword(password)));
         return true;
      } catch (Throwable e) {
         LOGGER.error("Could not join voice call group", e);
         return false;
      }
   }

   public static void leaveGroup() {
      try {
         ClientServerNetManager.sendToServer(new LeaveGroupPacket());
      } catch (Throwable e) {
         LOGGER.error("Could not leave voice call group", e);
      }
   }

   public static List<Group> getGroups() {
      List<Group> result = new ArrayList<>();

      try {
         for (ClientGroup group : ClientManager.getGroupManager().getGroups()) {
            result.add(new ClientGroupImpl(group));
         }
      } catch (Throwable e) {
         LOGGER.debug("Voice groups are not ready", e);
      }

      return result;
   }

   public static UUID getOwnGroupId() {
      try {
         return ClientManager.getPlayerStateManager().getGroupID();
      } catch (Throwable ignored) {
         return null;
      }
   }

   public static List<PlayerState> getPlayerStates() {
      try {
         return ClientManager.getPlayerStateManager().getPlayerStates(false);
      } catch (Throwable ignored) {
         return new ArrayList<>();
      }
   }

   public static boolean isReady() {
      try {
         return INSTANCE != null && !ClientManager.getPlayerStateManager().isDisconnected();
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean setMicrophoneMuted(boolean muted) {
      if(SimpleVoiceCallClient.config!=null){SimpleVoiceCallClient.config.muted=muted;SimpleVoiceCallClient.config.save();}
      try {
         MicThread mic = ClientManager.getClient().getMicThread();
         if (mic == null) {
            return false;
         }

         mic.setMicrophoneLocked(muted);
         SimpleVoiceCallClient.config.muted = muted;
         SimpleVoiceCallClient.config.save();
         return true;
      } catch (Throwable e) {
         LOGGER.error("Could not change microphone state", e);
         return false;
      }
   }

   public static boolean isMicrophoneMuted() {
      return SimpleVoiceCallClient.config.muted;
   }
}



