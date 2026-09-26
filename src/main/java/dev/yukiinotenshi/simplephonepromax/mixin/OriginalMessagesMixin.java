package dev.yukiinotenshi.simplephonepromax.mixin;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.yogurt278990.simplevoicecall.call.CallManager",remap=false)
public abstract class OriginalMessagesMixin {
 @Inject(method="sendMsg",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private static void concise(String message,CallbackInfo ci){ci.cancel();PhoneMessages.show(message);}
}

