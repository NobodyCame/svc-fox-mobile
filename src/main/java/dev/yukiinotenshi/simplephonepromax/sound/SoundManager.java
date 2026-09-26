package dev.yukiinotenshi.simplephonepromax.sound;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.Clip;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry.Reference;

public class SoundManager {
   private final MinecraftClient client = MinecraftClient.getInstance();
   private int ringtoneTickCounter = 0;
   private boolean ringtonePlaying = false;
   private boolean ringtoneLoop = false;
   private int ringtoneRemainingShots = 0;
   private long lastRingtoneAt = 0L;
   private boolean busyBeep = false;
   private long lastBusyBeepAt = 0L;
   private PositionedSoundInstance builtinPlaying;
   private Clip customClip;
   private String selectedRingtone;
   private float selectedVolume;
   private long decodeGeneration;
   private boolean decoding;
   private static final java.util.concurrent.ExecutorService DECODER = new java.util.concurrent.ThreadPoolExecutor(1,1,0,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"phone-ringtone-decoder");t.setDaemon(true);return t;},new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

   public void playRing(boolean loop) {
      this.stopRingtone();
      this.selectedRingtone=null;
      this.selectedVolume=SimpleVoiceCallClient.config.ringtoneVolume;
      if(SimpleVoiceCallClient.callManager!=null && SimpleVoiceCallClient.callManager.getState()==dev.yukiinotenshi.simplephonepromax.call.CallState.INCOMING_RINGING){
         var id=SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         if(!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.allows(id,false))return;
         if(id!=null){var preference=SimpleVoiceCallClient.config.contactRingtones.get(dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.contactKey(id));if(preference!=null){this.selectedRingtone=preference.ringtone;this.selectedVolume=preference.volume;}}
      }
      if (SimpleVoiceCallClient.callManager != null && SimpleVoiceCallClient.callManager.getState() == dev.yukiinotenshi.simplephonepromax.call.CallState.OUTGOING_RINGING) return;
      if (this.selectedVolume <= 0) return;
      this.ringtonePlaying = true;
      this.ringtoneLoop = loop;
      this.ringtoneTickCounter = 0;
      if (loop) {
         this.ringtoneRemainingShots = Integer.MAX_VALUE;
      } else if (SimpleVoiceCallClient.config.useCustomRingtone) {
         this.ringtoneRemainingShots = 1;
      } else {
         String rt = this.selectedRingtone != null ? this.selectedRingtone : SimpleVoiceCallClient.config.ringtone;
         if (!SimpleVoiceCallClient.config.useCustomRingtone && "silent".equalsIgnoreCase(rt)) {
            this.ringtoneRemainingShots = 0;
         } else if ("classic".equalsIgnoreCase(rt)) {
            this.ringtoneRemainingShots = 2;
         } else if ("digital".equalsIgnoreCase(rt)) {
            this.ringtoneRemainingShots = 3;
         } else {
            this.ringtoneRemainingShots = 2;
         }
      }

      if (this.ringtoneRemainingShots > 0) {
         this.startVoiceRingtone();
         this.playOneShotNow();
      }
   }

   public void playBusyBeepStart() {
      this.busyBeep = true;
      this.lastBusyBeepAt = 0L;
   }

   public void playBusyBeepStop() {
      this.busyBeep = false;
   }

   public void playRingtone() {
      this.playRing(true);
   }

   public void stopRing() {
      this.stopRingtone();
   }

   public void stopRingtone() {
      RingtoneVoiceInjector.stop();
      if(this.builtinPlaying!=null){this.client.getSoundManager().stop(this.builtinPlaying);this.builtinPlaying=null;}
      this.ringtonePlaying = false;
      this.ringtoneLoop = false;
      this.ringtoneTickCounter = 0;
      this.lastRingtoneAt = 0L;
      this.ringtoneRemainingShots = 0;
      this.busyBeep = false;
      this.closeCustomClip();
   }

   private void startVoiceRingtone() {
      if (!SimpleVoiceCallClient.config.transmitRingtoneInCall || VoicechatPluginImpl.getOwnGroupId() != null) return;
      String ringtone = this.selectedRingtone != null ? this.selectedRingtone : SimpleVoiceCallClient.config.ringtone;
      Path custom = null;
      if (ringtone != null && ringtone.startsWith("file:")) {
         custom = FabricLoader.getInstance().getConfigDir().resolve(SimpleVoiceCallClient.MOD_ID).resolve("ringtones")
               .resolve(Path.of(ringtone.substring(5)).getFileName().toString());
         ringtone = null;
      } else if (this.selectedRingtone == null && SimpleVoiceCallClient.config.useCustomRingtone) {
         String file = SimpleVoiceCallClient.config.customRingtone;
         if (file != null && !file.isBlank()) custom = FabricLoader.getInstance().getConfigDir().resolve(SimpleVoiceCallClient.MOD_ID)
               .resolve("ringtones").resolve(Path.of(file).getFileName().toString());
      }
      if (ringtone == null && custom == null) return;
      RingtoneVoiceInjector.start(ringtone, custom, this.selectedVolume);
   }

   public void tick() {
      if (this.ringtonePlaying) {
         this.ringtoneTickCounter++;
         boolean customClipActive = this.decoding || (this.builtinPlaying!=null&&this.client.getSoundManager().isPlaying(this.builtinPlaying));
         if (this.customClip != null) {
            customClipActive = this.ringtoneLoop || this.customClip.isRunning();
            if (!customClipActive) {
               this.closeCustomClip();
            }
         }

         if (!customClipActive) {
            long now = System.currentTimeMillis();
            long gap = 1600L;
            String rt = this.selectedRingtone != null ? this.selectedRingtone : SimpleVoiceCallClient.config.ringtone;
            if ("digital".equalsIgnoreCase(rt)) {
               gap = 1100L;
            }

            if ("classic".equalsIgnoreCase(rt)) {
               gap = 1900L;
            }

            if (!SimpleVoiceCallClient.config.useCustomRingtone && "silent".equalsIgnoreCase(rt)) {
               this.ringtonePlaying = false;
            } else if (!this.ringtoneLoop && this.ringtoneRemainingShots <= 0) {
               this.ringtonePlaying = false;
            } else if (now - this.lastRingtoneAt >= gap) {
               this.playOneShotNow();
            }
         }
      }

      if (this.busyBeep) {
         long now = System.currentTimeMillis();
         if (now - this.lastBusyBeepAt >= 500L) {
            this.lastBusyBeepAt = now;

            try {
               SoundEvent beep = pickBusyBeep();
               if (beep != null) {
                  MinecraftClient cl = MinecraftClient.getInstance();
                  if (cl != null && cl.player != null) {
                     PositionedSoundInstance inst = PositionedSoundInstance.ui(beep, 1.0F, 0.7F);
                     this.builtinPlaying=inst;cl.getSoundManager().play(inst);
                  }
               }
            } catch (Throwable var7) {
            }
         }
      }
   }

   public boolean isRingtonePlaying() {
      return this.ringtonePlaying;
   }

   private void playOneShotNow() {
      try {
         MinecraftClient cl = MinecraftClient.getInstance();
         if (cl == null || cl.player == null) {
            return;
         }

         if (!this.ringtoneLoop) {
            if (this.ringtoneRemainingShots <= 0) {
               return;
            }

            this.ringtoneRemainingShots--;
         }

         if (this.playCustomRingtone()) {
            this.lastRingtoneAt = System.currentTimeMillis();
            return;
         }

         SoundEvent se = pickSoundEvent();
         if (se == null) {
            return;
         }

         Reference<SoundEvent> ref = (Reference<SoundEvent>)Registries.SOUND_EVENT.getEntry(Registries.SOUND_EVENT.getId(se)).orElse(null);
         if (ref == null) {
            try {
               PositionedSoundInstance inst = PositionedSoundInstance.ui(se, 1.0F, this.selectedVolume);
               this.builtinPlaying=inst;cl.getSoundManager().play(inst);
            } catch (Throwable var5) {
            }
         } else {
            PositionedSoundInstance inst = PositionedSoundInstance.ui(ref.value(), 1.0F, this.selectedVolume);
            this.builtinPlaying=inst;cl.getSoundManager().play(inst);
         }

         this.lastRingtoneAt = System.currentTimeMillis();
      } catch (Throwable t) {
         this.lastRingtoneAt = System.currentTimeMillis();
      }
   }

   private boolean playCustomRingtone() {
      if (this.selectedRingtone != null ? !this.selectedRingtone.startsWith("file:") : !SimpleVoiceCallClient.config.useCustomRingtone) {
         return false;
      }

      String file = this.selectedRingtone!=null?this.selectedRingtone.substring(5):SimpleVoiceCallClient.config.customRingtone;
      if (file != null && !file.isBlank()) {
         try {
            this.closeCustomClip();
            Path path = FabricLoader.getInstance()
               .getConfigDir()
               .resolve(SimpleVoiceCallClient.MOD_ID)
               .resolve("ringtones")
               .resolve(Path.of(file).getFileName().toString());
            if (!Files.isRegularFile(path)) {
               return false;
            }

            final long generation = this.decodeGeneration;
            final float volume = this.selectedVolume;
            this.decoding = true;
            DECODER.execute(() -> {
               Clip decoded = null; String error = null;
               try { decoded = ExternalSoundPlayer.prepare(path,volume); } catch (Exception e) { error = e.getMessage(); }
               final Clip result=decoded;final String failure=error;
               this.client.execute(() -> {
                  if(generation!=this.decodeGeneration){if(result!=null)result.close();return;}
                  this.decoding=false;
                  if(result==null){this.ringtonePlaying=false;if(this.client.player!=null)dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages.show("Ошибка рингтона: "+failure);return;}
                  this.customClip=result;
                  if(this.ringtoneLoop)result.loop(Clip.LOOP_CONTINUOUSLY);else result.start();
               });
            });

            return true;
         } catch (Throwable ignored) {
            this.closeCustomClip();
            return false;
         }
      } else {
         return false;
      }
   }

   private void closeCustomClip() {
      this.decodeGeneration++;
      this.decoding=false;
      if (this.customClip != null) {
         try {
            this.customClip.stop();
            this.customClip.close();
         } catch (Throwable var2) {
         }

         this.customClip = null;
      }
   }

   private SoundEvent pickSoundEvent() {
      try {
         String rt = this.selectedRingtone != null ? this.selectedRingtone : SimpleVoiceCallClient.config.ringtone;
         if (rt == null) {
            rt = "default";
         }

         if("silent".equalsIgnoreCase(rt))return null;
         if(!java.util.Set.of("nokia","office","iphone","blackberry","atomic","samsung").contains(rt))rt="nokia";
         return SoundEvent.of(net.minecraft.util.Identifier.of(SimpleVoiceCallClient.RESOURCE_ID,"ring_"+rt));
      } catch (Throwable ignored) {
         return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
      }
   }

   private static SoundEvent pickBusyBeep() {
      try {
         SoundEvent t = builtin("block_note_block_hat");
         if (t != null) {
            return t;
         }
      } catch (Throwable var2) {
      }

      try {
         return builtin("block_note_block_pling");
      } catch (Throwable var1) {
         return null;
      }
   }

   private static SoundEvent builtin(String key) {
      try {
         try {
            Object f = SoundEvents.class.getField(key).get(null);
            if (f instanceof SoundEvent) {
               return (SoundEvent)f;
            }
         } catch (Throwable var10) {
         }

         try {
            Method m = SoundEvents.class.getMethod(key);
            Object r = m.invoke(null);
            if (r instanceof SoundEvent) {
               return (SoundEvent)r;
            }
         } catch (Throwable var9) {
         }
      } catch (Throwable var11) {
      }

      try {
         String[] tries = new String[]{
            "BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_BLOCK_HARP", "BLOCK_NOTE_BLOCK_BIT", "BLOCK_NOTE_BLOCK_HAT", "ENTITY_EXPERIENCE_ORB_PICKUP"
         };

         for (String t : tries) {
            try {
               Field f = SoundEvents.class.getField(t);
               Object v = f.get(null);
               if (v instanceof SoundEvent) {
                  return (SoundEvent)v;
               }
            } catch (Throwable var8) {
            }
         }
      } catch (Throwable var12) {
      }

      return SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
   }
}



