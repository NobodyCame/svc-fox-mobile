package dev.yukiinotenshi.simplephonepromax.sound;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
public final class CallAnnouncement {
 private static SimpleSoundInstance sound;private static long endAt;
 public static void stop(){if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);sound=null;endAt=0;}
 public static void play(String name){stop();var c=SimpleVoiceCallClient.config;float volume=switch(name){case "offline"->c.offlineVolume;case "ended"->c.endedVolume;case "voicemail"->c.voicemailVolume;case "invalid_number"->c.invalidNumberVolume;case "dialing"->c.dialingVolume;default->c.unavailableVolume;};
  long duration=switch(name){case "offline"->10581;case "ended"->9142;case "voicemail"->5804;case "invalid_number"->11331;case "dialing"->2000;default->12000;};
  endAt=System.currentTimeMillis()+duration+250;
  if(volume>0){sound=SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SimpleVoiceCallClient.RESOURCE_ID,name)),1f,volume);Minecraft.getInstance().getSoundManager().play(sound);}
 }
 public static boolean finished(){return System.currentTimeMillis()>=endAt&&(sound==null||!Minecraft.getInstance().getSoundManager().isActive(sound));}
}

