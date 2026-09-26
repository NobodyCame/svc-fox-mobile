package dev.yukiinotenshi.simplephonepromax.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.yogurt278990.simplevoicecall.VoicechatPluginImpl", remap = false)
public abstract class OriginalVoicechatSuppressionMixin {
   @Inject(method = "registerEvents", at = @At("HEAD"), cancellable = true, remap = false)
   private void suppressOriginalPlugin(CallbackInfo ci) { ci.cancel(); }
}

