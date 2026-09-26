package dev.yukiinotenshi.simplephonepromax.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Simple Voice Call mod: prevent its config, sounds, ticks and networking startup. */
@Pseudo
@Mixin(targets = "com.yogurt278990.simplevoicecall.SimpleVoiceCallClient", remap = false)
public abstract class OriginalClientSuppressionMixin {
   @Inject(method = "onInitializeClient", at = @At("HEAD"), cancellable = true, remap = false)
   private void proMaxOwnsPhone(CallbackInfo ci) {
      org.slf4j.LoggerFactory.getLogger("Fox Mobile").info("Simple Voice Call client is suppressed while Fox Mobile manages the phone interface");
      ci.cancel();
   }
}

