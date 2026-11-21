package dev.yukiinotenshi.simplephonepromax.sound;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Local cues only: never sent through the voice channel. */
public final class OperatorSounds {
    private static SimpleSoundInstance playing;
    private static String cue = "";
    private static long terminalUntil;
    private static CallState previous = CallState.NONE;
    private OperatorSounds() {}

    public static void unavailable() { stop(); terminalUntil = System.currentTimeMillis() + 12000; }
    public static void stop() {
        if (playing != null) Minecraft.getInstance().getSoundManager().stop(playing);
        playing = null; cue = "";
    }
    public static void tick() {
        var client = Minecraft.getInstance();
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
        if (playing == null || (repeat && !client.getSoundManager().isActive(playing))) {
            playing=SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SimpleVoiceCallClient.RESOURCE_ID,wanted)),1f,volume);
            client.getSoundManager().play(playing);
        }
    }
}

