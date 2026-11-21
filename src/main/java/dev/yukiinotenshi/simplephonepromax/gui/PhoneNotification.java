package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
public final class PhoneNotification {
 private static String heading="",sender="";private static long at;
 public static void show(String title,String name){show(title,name,null);}
 public static void show(String title,String name,String forcedSound){heading=title;sender=name;at=System.currentTimeMillis();var config=SimpleVoiceCallClient.config;if(config.notificationVolume>0){String sound=forcedSound!=null?forcedSound:java.util.Set.of("chaos","alpha","beep_once","cosmic-radio").contains(config.notificationSound)?config.notificationSound:"chaos";Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SimpleVoiceCallClient.RESOURCE_ID,"notify_"+sound)),1f,config.notificationVolume));}}
 private static SimpleSoundInstance preview;
 public static void stopPreview(){if(preview!=null){Minecraft.getInstance().getSoundManager().stop(preview);preview=null;}}
 public static void preview(){
  stopPreview();var config=SimpleVoiceCallClient.config;if(config.notificationVolume<=0)return;
  String sound=java.util.Set.of("chaos","alpha","beep_once","cosmic-radio").contains(config.notificationSound)?config.notificationSound:"chaos";
  preview=SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SimpleVoiceCallClient.RESOURCE_ID,"notify_"+sound)),1f,config.notificationVolume);
  Minecraft.getInstance().getSoundManager().play(preview);
 }
 public static void render(GuiGraphicsExtractor context){if(at>0&&System.currentTimeMillis()-at<8000)PhoneGuiTextures.drawToast(context,Minecraft.getInstance().font,heading,sender,at);}
}

