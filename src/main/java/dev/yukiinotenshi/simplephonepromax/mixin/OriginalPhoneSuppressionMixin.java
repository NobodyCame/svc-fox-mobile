package dev.yukiinotenshi.simplephonepromax.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The original interaction mixin stays installed but can no longer open its UI. */
@Pseudo
@Mixin(targets = "com.yogurt278990.simplevoicecall.phone.PhoneDetector", remap = false)
public abstract class OriginalPhoneSuppressionMixin {
   @Inject(method = "isPhone", at = @At("HEAD"), cancellable = true, remap = false)
   private static void ignoreOriginalPhone(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(false); }
}

