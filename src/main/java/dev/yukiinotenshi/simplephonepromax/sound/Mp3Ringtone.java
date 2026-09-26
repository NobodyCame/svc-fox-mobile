package dev.yukiinotenshi.simplephonepromax.sound;

import java.nio.file.*;
import java.io.*;
import javax.sound.sampled.AudioFormat;
import javazoom.jl.decoder.*;

public final class Mp3Ringtone {
   public record Pcm(AudioFormat format, byte[] bytes) {}
   public static Pcm decode(Path path) throws Exception {
      long deadline=System.nanoTime()+15_000_000_000L;
      if(Files.size(path)>128*1024*1024)throw new IOException("MP3 больше 128 МиБ");
      try (InputStream input=Files.newInputStream(path);ByteArrayOutputStream pcm=new ByteArrayOutputStream()) {
         Bitstream stream=new Bitstream(input);Decoder decoder=new Decoder();AudioFormat format=null;
         try {
            for(Header header;(header=stream.readFrame())!=null;) {
               if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IOException("MP3 decode timed out");
               SampleBuffer samples=(SampleBuffer)decoder.decodeFrame(header,stream);
               if(format==null)format=new AudioFormat(samples.getSampleFrequency(),16,samples.getChannelCount(),true,false);
               if(format.getSampleRate()!=samples.getSampleFrequency()||format.getChannels()!=samples.getChannelCount())throw new IOException("MP3 format changes inside file");
               short[] values=samples.getBuffer();
               for(int i=0;i<samples.getBufferLength();i++){pcm.write(values[i]&255);pcm.write((values[i]>>8)&255);}
               stream.closeFrame();
               if(pcm.size()>=Math.min(32*1024*1024,(int)(format.getFrameRate()*format.getFrameSize()*60)))break;
            }
         } finally {stream.close();}
         if(format==null||pcm.size()==0)throw new IOException("Empty or invalid MP3 ringtone");
         return new Pcm(format,pcm.toByteArray());
      }
   }
}

