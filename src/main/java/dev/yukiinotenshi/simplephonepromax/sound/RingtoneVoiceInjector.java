package dev.yukiinotenshi.simplephonepromax.sound;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.OggAudioStream;
import net.minecraft.util.Identifier;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.*;

/** Mixes an opted-in ringtone into SVC's proximity microphone stream while outside a group. */
public final class RingtoneVoiceInjector {
    private static final int RATE = 48_000;
    private static final int MAX_PCM_BYTES = 32 * 1024 * 1024;
    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "fox-mobile-ringtone-voice"); t.setDaemon(true); return t;
    });
    private static volatile short[] pcm;
    private static volatile boolean active;
    private static long generation;
    private static int cursor;

    private RingtoneVoiceInjector() {}

    public static synchronized void start(String ringtone, Path customFile, float gain) {
        stop();
        if (SimpleVoiceCallClient.config == null || !SimpleVoiceCallClient.config.transmitRingtoneInCall
                || VoicechatPluginImpl.getOwnGroupId() != null) return;
        long token = ++generation;
        active = true;
        DECODER.execute(() -> {
            try {
                Decoded decoded = customFile != null ? decodeFile(customFile) : decodeResource(ringtone);
                short[] converted = toMono48k(decoded, gain);
                MinecraftClient.getInstance().execute(() -> {
                    synchronized (RingtoneVoiceInjector.class) {
                        if (active && token == generation) { pcm = converted; cursor = 0; }
                    }
                });
            } catch (Exception error) {
                SimpleVoiceCallClient.LOGGER.warn("Could not prepare SVC ringtone audio", error);
                synchronized (RingtoneVoiceInjector.class) { if (token == generation) stop(); }
            }
        });
    }

    public static synchronized void stop() {
        generation++;
        active = false;
        pcm = null;
        cursor = 0;
    }

    /** Called from MicrophoneProcessor after SVC denoising, before voice activation is evaluated. */
    public static synchronized boolean mixInto(short[] mic) {
        if (!active || pcm == null || mic == null || mic.length == 0) return false;
        if (SimpleVoiceCallClient.config == null || !SimpleVoiceCallClient.config.transmitRingtoneInCall
                || VoicechatPluginImpl.getOwnGroupId() != null
                || VoicechatPluginImpl.isMicrophoneMuted()) {
            stop(); return false;
        }
        var api = EncryptedCalls.clientApi();
        if (api == null || api.isMuted()) return false;

        short[] sound = pcm;
        for (int i = 0; i < mic.length; i++) {
            if (cursor < 0) { cursor++; continue; }
            if (cursor >= sound.length) {
                // Keep the phone's ring cadence: sound, then a one-second pause.
                cursor = -RATE;
                cursor++;
                continue;
            }
            mic[i] = saturatingAdd(mic[i], sound[cursor++]);
        }
        return true;
    }

    private static short saturatingAdd(short a, short b) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int) a + b));
    }

    private record Decoded(AudioFormat format, byte[] bytes) {}

    private static Decoded decodeResource(String name) throws Exception {
        String safe = name == null || !name.matches("[a-z0-9_-]+") ? "ring_nokia" : name;
        if (!safe.startsWith("ring_")) safe = "ring_" + safe;
        var id = Identifier.of(SimpleVoiceCallClient.RESOURCE_ID, "sounds/" + safe + ".ogg");
        try (InputStream input = MinecraftClient.getInstance().getResourceManager().getResource(id).orElseThrow().getInputStream();
             OggAudioStream ogg = new OggAudioStream(input); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            AudioFormat format = ogg.getFormat();
            int limit = Math.min(MAX_PCM_BYTES, (int) (format.getFrameRate() * format.getFrameSize() * 30));
            while (out.size() < limit) {
                ByteBuffer chunk = ogg.read(16_384);
                if (!chunk.hasRemaining()) break;
                byte[] data = new byte[chunk.remaining()]; chunk.get(data);
                out.write(data, 0, Math.min(data.length, limit - out.size()));
            }
            if (out.size() == 0) throw new IOException("Empty ringtone resource");
            return new Decoded(format, out.toByteArray());
        }
    }

    private static Decoded decodeFile(Path path) throws Exception {
        if (!Files.isRegularFile(path) || Files.size(path) > 128L * 1024 * 1024) throw new IOException("Invalid ringtone file");
        String lower = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".mp3")) {
            var pcm = Mp3Ringtone.decode(path); return new Decoded(pcm.format(), pcm.bytes());
        }
        if (lower.endsWith(".ogg")) {
            try (InputStream input = Files.newInputStream(path); OggAudioStream ogg = new OggAudioStream(input);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                AudioFormat format = ogg.getFormat();
                int limit = Math.min(MAX_PCM_BYTES, (int) (format.getFrameRate() * format.getFrameSize() * 30));
                while (out.size() < limit) {
                    ByteBuffer chunk = ogg.read(16_384); if (!chunk.hasRemaining()) break;
                    byte[] data = new byte[chunk.remaining()]; chunk.get(data); out.write(data, 0, Math.min(data.length, limit - out.size()));
                }
                return new Decoded(format, out.toByteArray());
            }
        }
        if (lower.endsWith(".wav")) {
            try (AudioInputStream input = AudioSystem.getAudioInputStream(path.toFile())) {
                AudioFormat source = input.getFormat();
                if (source.getChannels() < 1 || source.getChannels() > 2 || source.getSampleRate() < 8_000 || source.getSampleRate() > 96_000) throw new IOException("Unsupported WAV format");
                AudioFormat format = new AudioFormat(source.getSampleRate(), 16, source.getChannels(), true, false);
                try (AudioInputStream converted = AudioSystem.getAudioInputStream(format, input); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[16_384]; int n;
                    while (out.size() < MAX_PCM_BYTES && (n = converted.read(chunk)) >= 0) out.write(chunk, 0, Math.min(n, MAX_PCM_BYTES - out.size()));
                    return new Decoded(format, out.toByteArray());
                }
            }
        }
        throw new IOException("Unsupported ringtone format");
    }

    private static short[] toMono48k(Decoded decoded, float gain) throws IOException {
        AudioFormat format = decoded.format(); byte[] bytes = decoded.bytes();
        if (format.getSampleSizeInBits() != 16 || format.isBigEndian() || format.getChannels() < 1 || format.getChannels() > 2
                || format.getSampleRate() < 8_000 || format.getSampleRate() > 96_000) throw new IOException("Unsupported PCM format");
        int frameSize = format.getFrameSize(); int sourceFrames = bytes.length / frameSize;
        int targetFrames = Math.min(RATE * 30, (int) ((long) sourceFrames * RATE / (long) format.getSampleRate()));
        if (targetFrames <= 0) throw new IOException("Empty ringtone PCM");
        short[] result = new short[targetFrames];
        for (int i = 0; i < targetFrames; i++) {
            int sourceFrame = Math.min(sourceFrames - 1, (int) ((long) i * (long) format.getSampleRate() / RATE));
            int offset = sourceFrame * frameSize;
            int left = (short) ((bytes[offset] & 255) | (bytes[offset + 1] << 8));
            int sample = left;
            if (format.getChannels() == 2) {
                int rightOffset = offset + 2;
                int right = (short) ((bytes[rightOffset] & 255) | (bytes[rightOffset + 1] << 8));
                sample = (left + right) / 2;
            }
            float safeGain = Float.isFinite(gain) ? Math.max(0f, Math.min(1f, gain)) : 0f;
            // SVC forwards this audio to other nearby players. Keep that mix
            // 50% quieter than the local ringtone without changing local volume.
            int scaled = Math.round(sample * safeGain * 0.35f);
            result[i] = (short) Math.max(-24_000, Math.min(24_000, scaled));
        }
        return result;
    }
}
