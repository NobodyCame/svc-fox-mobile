package dev.yukiinotenshi.simplephonepromax.sound;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Locale;
import javax.sound.sampled.*;
import net.minecraft.client.sound.OggAudioStream;

public final class ExternalSoundPlayer {
    private static final int MAX_PCM = 32 * 1024 * 1024;
    private ExternalSoundPlayer() {}
    public static Clip play(Path path) throws Exception { return play(path, 1f); }
    public static Clip play(Path path, float volume) throws Exception {
        Clip clip = prepare(path,volume); if(volume>0)clip.start(); return clip;
    }
    /** Called on the single decoder worker. Playback starts on the client thread. */
    public static Clip prepare(Path path, float volume) throws Exception {
        if(Files.size(path)>128*1024*1024)throw new IOException("Файл рингтона больше 128 МиБ");
        long deadline=System.nanoTime()+15_000_000_000L;
        String name=path.getFileName().toString().toLowerCase(Locale.ROOT);
        byte[] data; AudioFormat format;
        if(name.endsWith(".mp3")) {
            var pcm=Mp3Ringtone.decode(path); data=pcm.bytes();format=pcm.format();
        } else if(name.endsWith(".ogg")) {
            try(var input=Files.newInputStream(path);var ogg=new OggAudioStream(input);var output=new ByteArrayOutputStream()) {
                int limit=Math.min(MAX_PCM,(int)(ogg.getFormat().getFrameRate()*ogg.getFormat().getFrameSize()*60));
                while(output.size()<limit) {
                    check(deadline,output.size());
                    ByteBuffer chunk=ogg.read(16384); if(!chunk.hasRemaining())break;
                    byte[] part=new byte[chunk.remaining()];chunk.get(part);output.write(part,0,Math.min(part.length,limit-output.size()));
                }
                format=ogg.getFormat();data=output.toByteArray();
            }
        } else if(name.endsWith(".wav")) {
            try(var source=AudioSystem.getAudioInputStream(path.toFile())) {
                var src=source.getFormat();
                if(src.getChannels()<1||src.getChannels()>2||src.getSampleRate()<8000||src.getSampleRate()>96000)throw new IOException("Неподдерживаемый формат WAV");
                format=new AudioFormat(src.getSampleRate(),16,src.getChannels(),true,false);
                try(var converted=AudioSystem.getAudioInputStream(format,source);var output=new ByteArrayOutputStream()) {
                    byte[] chunk=new byte[16384];int n;
                    int limit=Math.min(MAX_PCM,(int)(format.getFrameRate()*format.getFrameSize()*60));
                    while(output.size()<limit&&(n=converted.read(chunk))!=-1){check(deadline,output.size());output.write(chunk,0,Math.min(n,limit-output.size()));}
                    data=output.toByteArray();
                }
            }
        } else throw new IOException("Поддерживаются MP3, OGG и WAV");
        check(deadline,data.length);
        if(format.getSampleSizeInBits()!=16||format.isBigEndian())throw new IOException("Неподдерживаемый PCM");
        // Apply gain to samples, also on devices without MASTER_GAIN controls.
        float gain=Float.isFinite(volume)?Math.max(0,Math.min(1,volume)):0;
        int peak=0;for(int i=0;i+1<data.length;i+=2)peak=Math.max(peak,Math.abs((short)((data[i]&255)|(data[i+1]<<8))));
        if(peak>0)gain*=Math.min(2f,30000f/peak);
        for(int i=0;i+1<data.length;i+=2){int sample=(short)((data[i]&255)|(data[i+1]<<8));int scaled=Math.round(sample*gain);data[i]=(byte)scaled;data[i+1]=(byte)(scaled>>8);}
        Clip clip=AudioSystem.getClip();
        try{clip.open(format,data,0,data.length);return clip;}catch(Exception|Error e){clip.close();throw e;}
    }
    private static void check(long deadline,int bytes)throws IOException {
        if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IOException("Превышено время декодирования рингтона");
        if(bytes>MAX_PCM)throw new IOException("Рингтон больше 32 МиБ после декодирования");
    }
}

