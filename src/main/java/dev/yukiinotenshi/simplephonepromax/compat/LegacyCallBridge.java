package dev.yukiinotenshi.simplephonepromax.compat;

import com.google.gson.Gson;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen;
import dev.yukiinotenshi.simplephonepromax.gui.IncomingCallScreen;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Delegates to the separately installed Simple Voice Call JAR, never an embedded copy. */
public final class LegacyCallBridge {
   private static final String PREFIX="com.yogurt278990.simplevoicecall.";
   private static final Gson JSON=new Gson();
   private static final Map<String,Method> METHODS=new HashMap<>();
   private static Object manager, config, sound;
   private static boolean ready;
   private static final Set<String> knownHistory=new HashSet<>();
   private static String lastState="NONE";
   private static long ringingAt;

   private static Object invoke(Object receiver,Class<?> owner,String name,Object...args) throws ReflectiveOperationException {
      String key=owner.getName()+"#"+name+Arrays.toString(Arrays.stream(args).map(a->a==null?null:a.getClass()).toArray());
      Method method=METHODS.get(key);
      if(method==null) {
         for(Method candidate:owner.getMethods()) {
            if(!candidate.getName().equals(name)||candidate.getParameterCount()!=args.length)continue;
            Class<?>[] types=candidate.getParameterTypes();boolean match=true;
            for(int i=0;i<types.length;i++)if(args[i]!=null&&!types[i].isInstance(args[i])&&!(types[i]==boolean.class&&args[i] instanceof Boolean))match=false;
            if(match){method=candidate;break;}
         }
         if(method==null)throw new NoSuchMethodException(owner.getName()+"."+name);
         METHODS.put(key,method);
      }
      return method.invoke(receiver,args);
   }
   public static void initialize() {
      if(!CallStandard.originalInstalled())return;
      try {
         Class<?> client=Class.forName(PREFIX+"SimpleVoiceCallClient");
         config=Class.forName(PREFIX+"config.ModConfig").getConstructor().newInstance();
         invoke(config,config.getClass(),"load");client.getField("config").set(null,config);
         manager=Class.forName(PREFIX+"call.CallManager").getConstructor().newInstance();
         client.getField("callManager").set(null,manager);
         sound=Class.forName(PREFIX+"sound.SoundManager").getConstructor().newInstance();
         client.getField("soundManager").set(null,sound);
         client.getField("phoneDetector").set(null,Class.forName(PREFIX+"phone.PhoneDetector").getConstructor().newInstance());
         // Validate the interface before enabling any ticks or network actions.
         manager.getClass().getMethod("startOutgoingCall",String.class,UUID.class);
         manager.getClass().getMethod("getState");manager.getClass().getMethod("endCall");
         Class.forName(PREFIX+"network.VoiceCallTransport").getMethod("acceptIncoming");
         invoke(null,Class.forName(PREFIX+"phone.PhoneNumberManager"),"load");
         invoke(null,Class.forName(PREFIX+"network.ModNetworking"),"register");
         ready=true;syncPreferences();syncHistory();
         SimpleVoiceCallClient.LOGGER.info("Legacy-compatible call engine loaded from the installed Simple Voice Call mod; Fox Mobile supplies the UI");
      } catch(ReflectiveOperationException|LinkageError e) { fail(e); }
   }
   public static boolean ready() {return ready;}
   private static void fail(Throwable error) {
      if(ready) {
         try {invoke(null,Class.forName(PREFIX+"network.VoiceCallTransport"),"leaveGroup");invoke(sound,sound.getClass(),"stopRing");}catch(Throwable ignored){}
      }
      ready=false;
      SimpleVoiceCallClient.LOGGER.error("Installed Simple Voice Call mod is incompatible with the legacy bridge; calls disabled",error);
   }
   public static void unavailable() {
      MinecraftClient c=MinecraftClient.getInstance();
      if(c.player!=null)c.player.sendMessage(Text.translatable("phone.standard.original_required"),false);
   }
   public static Object manager(String name,Object...args) {
      if(!ready)return null;
      try {return invoke(manager,manager.getClass(),name,args);}catch(ReflectiveOperationException|LinkageError e){fail(e);return null;}
   }
   public static Object original(String component,String name,Object...args) {
      if(!ready)return null;
      try {return invoke(null,Class.forName(PREFIX+component),name,args);}catch(ReflectiveOperationException|LinkageError e){fail(e);return null;}
   }
   public static String state() {Object value=manager("getState");return value==null?"NONE":value.toString();}
   public static long ringingAt() {if(ringingAt==0&&state().endsWith("RINGING"))ringingAt=System.currentTimeMillis();return ringingAt;}
   private static void syncPreferences() throws ReflectiveOperationException {
      for(String name:List.of("ringtone","useCustomRingtone","customRingtone","passwordProtectedCalls","muted","blockedPlayers")) {
         try {config.getClass().getField(name).set(config,SimpleVoiceCallClient.config.getClass().getField(name).get(SimpleVoiceCallClient.config));}
         catch(NoSuchFieldException ignored) { /* Older Simple Voice Call versions may lack a preference. */ }
      }
   }
   private static void syncHistory() throws ReflectiveOperationException {
      Object entries=config.getClass().getField("callHistory").get(config);boolean changed=false;
      if(entries instanceof List<?> list)for(Object item:list) {
         ModConfig.CallHistoryEntry entry=JSON.fromJson(JSON.toJson(item),ModConfig.CallHistoryEntry.class);
         String key=entry.timestampMs+"/"+entry.uuid+"/"+entry.type;
         if(knownHistory.add(key)&&SimpleVoiceCallClient.config.callHistory.stream().noneMatch(e->e.timestampMs==entry.timestampMs&&Objects.equals(e.uuid,entry.uuid)&&Objects.equals(e.type,entry.type))) {
            SimpleVoiceCallClient.config.callHistory.add(entry);changed=true;
         }
      }
      if(changed) {
         SimpleVoiceCallClient.config.callHistory.sort(Comparator.comparingLong((ModConfig.CallHistoryEntry e)->e.timestampMs).reversed());
         while(SimpleVoiceCallClient.config.callHistory.size()>100)SimpleVoiceCallClient.config.callHistory.removeLast();
         SimpleVoiceCallClient.config.save();
      }
   }
   public static void tick() {
      if(!ready)return;
      try {
         syncPreferences();manager("tick");
         invoke(sound,sound.getClass(),"tick");
         original("network.ModNetworking","tick");
         syncHistory();
         String now=state();
         if(!now.equals(lastState)) {
            ringingAt=now.endsWith("RINGING")?System.currentTimeMillis():0;
            MinecraftClient c=MinecraftClient.getInstance();
            if(now.equals("NONE")&&(c.currentScreen instanceof ActiveCallScreen||c.currentScreen instanceof IncomingCallScreen))c.setScreen(null);
            lastState=now;
         }
      }catch(ReflectiveOperationException|LinkageError e){fail(e);}
   }
   public static void disconnect() {original("network.ModNetworking","onVoicechatDisconnected");manager("connectionLost");}
}

