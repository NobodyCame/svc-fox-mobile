package dev.yukiinotenshi.simplephonepromax.network;

import com.google.gson.*;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.gui.EncryptedCallScreen;
import dev.yukiinotenshi.simplephonepromax.phone.CallPolicy;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.opus.*;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.*;

public final class EncryptedCalls {
    public static final boolean ENABLED=false;
    private static VoicechatClientApi api;
    private static SecureVoiceCrypto crypto;
    private static OpusEncoder encoder;
    private static OpusDecoder decoder;
    private static ClientStaticAudioChannel channel;
    private static JsonObject session,incoming;
    private static final ArrayDeque<JsonObject> frames=new ArrayDeque<>();
    private static boolean derived,verified,ready,busy;
    private static long nextMedia,lastResponse,generation;
    private static String error="",serverContext="",notified="";
    public static synchronized void initialize(VoicechatClientApi value){api=value;}
    public static synchronized VoicechatClientApi clientApi(){return api;}
    public static synchronized boolean supported(){return ENABLED&&api!=null;}
    public static synchronized boolean active(){return session!=null;}
    public static synchronized boolean hasIncoming(){return incoming!=null;}
    public static synchronized String status(){return !error.isEmpty()?error:session==null?(incoming==null?"Выберите совместимый контакт":"Входящий шифрованный звонок"):!derived?"Ожидаем ответа":!ready?"Сравните код по доверенному каналу":"Голос зашифрован; микрофон управляется через SVC";}
    public static synchronized String comparison(){return derived?crypto.comparison():"";}
    public static synchronized boolean needsVerification(){return derived&&!verified;}
    private static String str(JsonObject o,String k){return o.get(k).getAsString();}
    public static synchronized void poll(JsonArray sessions){
        if(!ENABLED)return;
        if(session!=null)return;
        var client=MinecraftClient.getInstance();if(client.player==null)return;
        boolean hadIncoming=incoming!=null;incoming=null;
        for(var item:sessions){var s=item.getAsJsonObject();if(client.player.getUuid().toString().equals(str(s,"target"))&&CallPolicy.allows(UUID.fromString(str(s,"source")),false)&&!SimpleVoiceCallClient.callManager.isInCall()) {incoming=s;if(!str(s,"id").equals(notified)){notified=str(s,"id");dev.yukiinotenshi.simplephonepromax.gui.PhoneNotification.show("Шифрованный вызов","Откройте телефон");SimpleVoiceCallClient.soundManager.playRing(true);}break;}}
        if(hadIncoming&&incoming==null)SimpleVoiceCallClient.soundManager.stopRing();
    }
    private static void prepare(JsonObject s)throws Exception {
        SimpleVoiceCallClient.soundManager.stopRing();
        session=s;incoming=null;crypto=new SecureVoiceCrypto();encoder=api.createEncoder();decoder=api.createDecoder();channel=api.createStaticAudioChannel(UUID.randomUUID());
        serverContext=dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current();derived=verified=ready=false;frames.clear();lastResponse=System.currentTimeMillis();error="";
    }
    public static synchronized void call(UUID target){
        if(!ENABLED)return;
        if(VoicemailClient.busy()||active()||api==null||SimpleVoiceCallClient.callManager.isInCall()||VoicechatPluginImpl.getOwnGroupId()!=null||!CallControlService.supports(target,"encrypted_voice_v1")){dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages.show("Шифрованная линия недоступна: завершите текущий разговор и проверьте связь обоих игроков");return;}
        try{
            JsonObject s=new JsonObject();s.addProperty("id",UUID.randomUUID().toString());s.addProperty("source",MinecraftClient.getInstance().player.getUuid().toString());s.addProperty("target",target.toString());prepare(s);
            JsonObject b=body();b.addProperty("target",target.toString());b.addProperty("key",crypto.publicKey());request("secure-offer",b);
            MinecraftClient.getInstance().setScreen(new EncryptedCallScreen());
        }catch(Exception e){SimpleVoiceCallClient.LOGGER.warn("Encrypted call setup failed",e);fail("Не удалось начать шифрованный звонок");}
    }
    public static synchronized void accept(){
        if(!ENABLED)return;
        if(incoming==null||api==null||SimpleVoiceCallClient.callManager.isInCall()||VoicechatPluginImpl.getOwnGroupId()!=null)return;
        try{JsonObject s=incoming;prepare(s);JsonObject b=body();b.addProperty("key",crypto.publicKey());request("secure-accept",b);}catch(Exception e){fail("Не удалось принять звонок");}
    }
    public static synchronized void verify(){if(!needsVerification())return;verified=true;request("secure-verify",body());}
    private static JsonObject body(){JsonObject b=new JsonObject();b.addProperty("id",str(session,"id"));return b;}
    private static void request(String action,JsonObject body){
        long epoch=generation;busy=true;
        CallControlService.extension(action,body).whenComplete((v,e)->MinecraftClient.getInstance().execute(()->{synchronized(EncryptedCalls.class){
            if(epoch!=generation)return;busy=false;
            if(e!=null){SimpleVoiceCallClient.LOGGER.warn("Encrypted call action {} failed: {}",action,e.toString());fail("Шифрованный вызов недоступен");return;}
            try{apply(v);}catch(Exception invalid){fail("Ошибка проверки шифрованного аудио");}
        }}));
    }
    private static void apply(JsonObject s)throws Exception {
        if(session==null||!str(session,"id").equals(str(s,"id")))return;
        boolean source=MinecraftClient.getInstance().player.getUuid().toString().equals(str(s,"source"));
        if(!str(s,source?"source_key":"target_key").equals(crypto.publicKey()))throw new SecurityException("local key replaced");
        if(!derived&&!str(s,"target_key").isEmpty()){
            crypto.derive(str(s,"id"),str(s,"source"),str(s,"target"),str(s,"source_key"),str(s,"target_key"),source);derived=true;
        }
        if(derived&&session.has("target_key")&&!str(session,"target_key").isEmpty()&&(!str(session,"source_key").equals(str(s,"source_key"))||!str(session,"target_key").equals(str(s,"target_key"))))throw new SecurityException("key changed");
        session=s;lastResponse=System.currentTimeMillis();ready=derived&&verified&&s.get("ready").getAsBoolean();
        if(ready&&s.has("frames"))for(var value:s.getAsJsonArray("frames")){var f=value.getAsJsonObject();byte[] cipher=Base64.getDecoder().decode(str(f,"data"));if(cipher.length>4096)throw new SecurityException("oversized frame");byte[] plain=crypto.decrypt(f.get("seq").getAsLong(),cipher);channel.play(decoder.decode(plain));}
    }
    public static synchronized void capture(ClientSoundEvent event){
        if(session==null)return;
        // Cancel before any processing, including unverified or failed handshakes.
        short[] audio=event.getRawAudio();event.cancel();event.setRawAudio(new short[audio.length]);
        if(!ready||VoicechatPluginImpl.isMicrophoneMuted())return;
        try{var packet=crypto.encrypt(encoder.encode(audio));JsonObject f=new JsonObject();f.addProperty("seq",packet.sequence());f.addProperty("data",Base64.getEncoder().encodeToString(packet.ciphertext()));if(frames.size()>=10)frames.removeFirst();frames.addLast(f);}catch(Exception e){ready=false;MinecraftClient.getInstance().execute(()->fail("Ошибка шифрования; микрофон остановлен"));}
    }
    public static synchronized void tick(){
        if(session==null)return;var client=MinecraftClient.getInstance();long now=System.currentTimeMillis();
        if(client.player==null||!serverContext.equals(dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current())||!VoicechatPluginImpl.isReady()){end();return;}
        if(now-lastResponse>8000){fail("Соединение потеряно; звонок завершён");return;}
        if(!busy&&now>=nextMedia){nextMedia=now+100;JsonObject b=body();JsonArray batch=new JsonArray();while(!frames.isEmpty())batch.add(frames.removeFirst());b.add("frames",batch);request("secure-media",b);}
    }
    private static synchronized void fail(String message){end();error=message;dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages.show(message);}
    public static synchronized void end(){
        SimpleVoiceCallClient.soundManager.stopRing();
        JsonObject closing=session!=null?session:incoming;
        if(closing!=null&&MinecraftClient.getInstance().player!=null){JsonObject b=new JsonObject();b.addProperty("id",str(closing,"id"));CallControlService.extension("secure-end",b);}
        if(session!=null)VoicechatPluginImpl.setMicrophoneMuted(false);
        generation++;ready=verified=derived=busy=false;session=incoming=null;frames.clear();
        if(encoder!=null)encoder.close();if(decoder!=null)decoder.close();if(crypto!=null)crypto.close();encoder=null;decoder=null;crypto=null;channel=null;
    }
}

