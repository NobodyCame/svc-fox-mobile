package dev.yukiinotenshi.simplephonepromax.mixin;

import dev.yukiinotenshi.simplephonepromax.sound.RingtoneVoiceInjector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "de.maxhenkel.voicechat.voice.client.MicrophoneProcessor", remap = false)
public abstract class MicrophoneProcessorRingtoneMixin {
    @Shadow private boolean activating;
    @Unique private boolean foxmobile$ringtoneMixed;

    @Inject(method = "process([SZ)V", at = @At(value = "INVOKE", target = "Lde/maxhenkel/voicechat/voice/client/MicrophoneProcessor;processInternal([SZ)Z", shift = At.Shift.BEFORE), remap = false)
    private void foxmobile$mixRingtone(short[] rawAudio, boolean testing, CallbackInfo ci) {
        foxmobile$ringtoneMixed = RingtoneVoiceInjector.mixInto(rawAudio);
    }

    @Inject(method = "process([SZ)V", at = @At("RETURN"), remap = false)
    private void foxmobile$transmitRingtoneWithoutPtt(short[] rawAudio, boolean testing, CallbackInfo ci) {
        if (foxmobile$ringtoneMixed) {
            activating = true;
            foxmobile$ringtoneMixed = false;
        }
    }
}
