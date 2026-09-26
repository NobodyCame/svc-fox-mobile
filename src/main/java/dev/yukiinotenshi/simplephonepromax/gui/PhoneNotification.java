package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
public final class PhoneNotification {
 private static String heading="",sender="";private static long at;
 public static void show(String title,String name){show(title,name,null);}
 public static void show(String title,String name,String forcedSound){heading=title;sender=name;at=System.currentTimeMillis();var config=SimpleVoiceCallClient.config;if(config.notificationVolume>0){String sound=forcedSound!=null?forcedSound:java.util.Set.of("chaos","alpha","beep_once","cosmic-radio").contains(config.notificationSound)?config.notificationSound:"chaos";MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvent.of(Identifier.of(SimpleVoiceCallClient.RESOURCE_ID,"notify_"+sound)),1f,config.notificationVolume));}}
 private static PositionedSoundInstance preview;
 public static void stopPreview(){if(preview!=null){MinecraftClient.getInstance().getSoundManager().stop(preview);preview=null;}}
 public static void preview(){
  stopPreview();var config=SimpleVoiceCallClient.config;if(config.notificationVolume<=0)return;
  String sound=java.util.Set.of("chaos","alpha","beep_once","cosmic-radio").contains(config.notificationSound)?config.notificationSound:"chaos";
  preview=PositionedSoundInstance.ui(SoundEvent.of(Identifier.of(SimpleVoiceCallClient.RESOURCE_ID,"notify_"+sound)),1f,config.notificationVolume);
  MinecraftClient.getInstance().getSoundManager().play(preview);
 }
 public static void render(DrawContext context){if(at>0&&System.currentTimeMillis()-at<8000)PhoneGuiTextures.drawToast(context,MinecraftClient.getInstance().textRenderer,heading,sender,at);}
}

