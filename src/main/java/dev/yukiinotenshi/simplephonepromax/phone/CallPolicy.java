package dev.yukiinotenshi.simplephonepromax.phone;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import java.time.LocalTime;
import java.util.UUID;
public final class CallPolicy {
   private static final java.util.Map<String,Long> actionCooldowns=new java.util.HashMap<>();
   public static synchronized boolean cooldown(UUID player,String action){if(player==null)return false;long now=System.currentTimeMillis();String key=ServerProfiles.contactKey(player)+"/"+action;long last=actionCooldowns.getOrDefault(key,0L);if(now-last<3000)return false;actionCooldowns.put(key,now);actionCooldowns.entrySet().removeIf(e->now-e.getValue()>60000);return true;}
   private static final java.util.Map<String,java.util.ArrayDeque<Long>> attempts=new java.util.HashMap<>();
   private static final java.util.LinkedHashMap<String,Long> events=new java.util.LinkedHashMap<>();
   public static synchronized boolean admit(UUID caller,String event){
      if(caller==null)return false;long now=System.currentTimeMillis();String key=ServerProfiles.contactKey(caller);
      if(SimpleVoiceCallClient.config.temporaryMutes.getOrDefault(key,0L)>now)return false;
      events.entrySet().removeIf(e->now-e.getValue()>120000);String eid=key+"/"+event;if(events.containsKey(eid))return true;
      var queue=attempts.computeIfAbsent(key,k->new java.util.ArrayDeque<>());while(!queue.isEmpty()&&now-queue.peekFirst()>60000)queue.removeFirst();
      if(queue.size()>=3||!queue.isEmpty()&&now-queue.peekLast()<3000)return false;
      queue.addLast(now);events.put(eid,now);attempts.entrySet().removeIf(e->e.getValue().isEmpty()||now-e.getValue().peekLast()>120000);return true;
   }
   public static void muteFor15Minutes(UUID caller){if(caller!=null){SimpleVoiceCallClient.config.temporaryMutes.put(ServerProfiles.contactKey(caller),System.currentTimeMillis()+900000);SimpleVoiceCallClient.config.save();}}

   private static String effectiveDndMode(){
      var c=SimpleVoiceCallClient.config;
      int hour=LocalTime.now().getHour();
      boolean quiet=c.quietSchedule&&(c.quietStartHour<c.quietEndHour?hour>=c.quietStartHour&&hour<c.quietEndHour:hour>=c.quietStartHour||hour<c.quietEndHour);
      return quiet?"favorites":c.doNotDisturb;
   }
   public static boolean dndActive(){return !"all".equals(effectiveDndMode());}
   public static boolean favoritesOnly(){return "favorites".equals(effectiveDndMode());}
   public static boolean isFavorite(UUID caller){
      return caller!=null&&(SimpleVoiceCallClient.config.favoriteContacts.contains(caller.toString())||SimpleVoiceCallClient.config.favoriteContacts.contains(ServerProfiles.contactKey(caller)));
   }
   private static boolean allowedByDnd(UUID caller){
      String mode=effectiveDndMode();
      return "all".equals(mode)||"favorites".equals(mode)&&isFavorite(caller);
   }

   public static boolean allows(UUID caller,boolean waiting){
      if(dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy())return false;
      if(dev.yukiinotenshi.simplephonepromax.gui.DialAttemptScreen.active()||dev.yukiinotenshi.simplephonepromax.network.VoicemailClient.busy()||dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.active())return false;
      var c=SimpleVoiceCallClient.config;
      if(caller==null||c.temporaryMutes.getOrDefault(ServerProfiles.contactKey(caller),0L)>System.currentTimeMillis()||c.isBlocked(caller)||(waiting&&!c.allowCallWaiting))return false;
      return allowedByDnd(caller);
   }
}

