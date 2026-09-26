package dev.yukiinotenshi.simplephonepromax.mixin;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.yogurt278990.simplevoicecall.sound.SoundManager",remap=false)
public abstract class OriginalSoundSuppressionMixin {
   @Inject(method="playRing",at=@At("HEAD"),cancellable=true,remap=false)
   private void ring(boolean loop,CallbackInfo ci){ci.cancel();if(SimpleVoiceCallClient.soundManager!=null)SimpleVoiceCallClient.soundManager.playRing(loop);}
   @Inject(method="stopRing",at=@At("HEAD"),cancellable=true,remap=false)
   private void stop(CallbackInfo ci){ci.cancel();if(SimpleVoiceCallClient.soundManager!=null)SimpleVoiceCallClient.soundManager.stopRing();}
   @Inject(method="tick",at=@At("HEAD"),cancellable=true,remap=false)
   private void tick(CallbackInfo ci){ci.cancel();}
}

