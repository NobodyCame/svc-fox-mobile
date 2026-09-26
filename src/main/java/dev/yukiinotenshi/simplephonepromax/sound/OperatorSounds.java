package dev.yukiinotenshi.simplephonepromax.sound;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Local cues only: never sent through the voice channel. */
public final class OperatorSounds {
    private static PositionedSoundInstance playing;
    private static String cue = "";
    private static long terminalUntil;
    private static CallState previous = CallState.NONE;
    private OperatorSounds() {}

    public static void unavailable() { stop(); terminalUntil = System.currentTimeMillis() + 12000; }
    public static void stop() {
        if (playing != null) MinecraftClient.getInstance().getSoundManager().stop(playing);
        playing = null; cue = "";
    }
    public static void tick() {
        var client = MinecraftClient.getInstance();
        if (client.player == null || SimpleVoiceCallClient.callManager == null) {
            stop(); terminalUntil = 0; previous = CallState.NONE; return;
        }
        var state = SimpleVoiceCallClient.callManager.getState();
        if (state != previous) { stop(); if (state != CallState.NONE) terminalUntil = 0; previous = state; }
        var config = SimpleVoiceCallClient.config;
        String wanted = ""; float volume = 0;
        boolean repeat = false;
        if (dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.outgoingRinging()) { wanted="dialing"; volume=config.dialingVolume; repeat=true; }
        else if (state == CallState.OUTGOING_RINGING) { wanted="dialing"; volume=config.dialingVolume; repeat=true; }
        else if (state == CallState.BUSY) { wanted="busy"; volume=config.busyVolume; }
        else if (state == CallState.ACTIVE && CallControlService.incoming()!=null) { wanted="waiting"; volume=config.waitingVolume; repeat=true; }
        else if (System.currentTimeMillis() < terminalUntil) { wanted="unavailable"; volume=config.unavailableVolume; }
        if (volume <= 0 || wanted.isEmpty()) { stop(); return; }
        if (!wanted.equals(cue)) { stop(); cue = wanted; }
        if (playing == null || (repeat && !client.getSoundManager().isPlaying(playing))) {
            playing=PositionedSoundInstance.ui(SoundEvent.of(Identifier.of(SimpleVoiceCallClient.RESOURCE_ID,wanted)),1f,volume);
            client.getSoundManager().play(playing);
        }
    }
}

