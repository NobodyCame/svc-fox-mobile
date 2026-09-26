package dev.yukiinotenshi.simplephonepromax.network;

import com.google.gson.*;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import dev.yukiinotenshi.simplephonepromax.gui.*;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import net.minecraft.client.MinecraftClient;
import java.util.*;

/** Password rendezvous only; microphone and audio remain in SVC. */
public final class PrivateCalls {
 private static JsonObject session;
 private static final java.util.LinkedHashSet<String> ended=new java.util.LinkedHashSet<>();
 private static boolean creating,requesting,creatingGroup,joining,connected;
private static long generation,lastSeen,started,connectedAt,missingSince,orphanUntil,acceptedAt,joiningAt;
 private static int joinAttempts;
 private static String context="",status="Выберите контакт",orphanName="";
 private static UUID pendingTarget;
 private static MinecraftClient client(){return MinecraftClient.getInstance();}
 private static String str(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():"";}
 public static boolean hasSession(){return session!=null;}
 public static boolean busy(){return creating||session!=null;}
 public static boolean incoming(){return session!=null&&str(session,"status").equals("pending")&&!source();}
 public static boolean connected(){return connected;}
 public static boolean outgoingRinging(){return creating||session!=null&&source()&&str(session,"status").equals("pending");}
 public static String status(){return status;}
 public static String peerName(){UUID id=session==null?pendingTarget:UUID.fromString(str(session,source()?"target":"source"));if(id==null)return "";var entry=client().getNetworkHandler()==null?null:client().getNetworkHandler().getPlayerListEntry(id);if(entry!=null)return entry.getProfile().name();for(var contact:SimpleVoiceCallClient.config.contacts)if(id.toString().equals(contact.uuid))return contact.name;return "Абонент";}
 private static boolean source(){return session!=null&&client().player!=null&&str(session,"source").equals(client().player.getUuid().toString());}
 private static boolean canStart(){return !CallStandard.isLegacy()&&CallControlService.available()&&VoicechatPluginImpl.isReady()&&client().player!=null&&PhoneDetector.hasPhone(client())&&!SimpleVoiceCallClient.callManager.isInCall()&&VoicechatPluginImpl.getOwnGroupId()==null&&!CallControlService.hasHeld()&&!OrganizationCalls.queued()&&!dev.yukiinotenshi.simplephonepromax.gui.DialAttemptScreen.active()&&CallControlService.incoming()==null;}
 private static JsonObject body(){var b=new JsonObject();b.addProperty("id",str(session,"id"));return b;}
 public static void call(UUID target){
  if(target==null||client().player==null||busy()||!canStart()||!CallPolicy.cooldown(client().player.getUuid(),"private")||!CallControlService.supports(target,"private_call_v1")){PhoneMessages.show("Приватный звонок недоступен");return;}
  creating=true;joining=false;joinAttempts=0;acceptedAt=joiningAt=0;pendingTarget=target;status="Вызов…";context=ServerProfiles.current();long epoch=++generation;started=System.currentTimeMillis();client().setScreen(new PrivateCallScreen());
  var b=new JsonObject();b.addProperty("target",target.toString());
  CallControlService.extensionWithSnapshot("private-offer",b).whenComplete((v,e)->client().execute(()->{
   if(epoch!=generation){if(v!=null){var end=new JsonObject();end.addProperty("id",str(v,"id"));CallControlService.extension("private-end",end);}return;}
   creating=false;if(e!=null){status="Абонент недоступен или занят";pendingTarget=null;PhoneMessages.show(status);return;}pendingTarget=null;apply(v);client().setScreen(new PrivateCallScreen());
  }));
 }
 public static void poll(JsonArray list){
  JsonObject found=null;for(var item:list){var s=item.getAsJsonObject();if(!ended.contains(str(s,"id"))&&(session==null||str(session,"id").equals(str(s,"id")))){found=s;break;}}
  if(found==null){if(session!=null){if(missingSince==0)missingSince=System.currentTimeMillis();else if(System.currentTimeMillis()-missingSince>2500)finish("Звонок завершён",false);}return;}
  missingSince=0;
  if(session==null){
   if(creating)return;
   UUID caller=UUID.fromString(str(found,"source"));
   if(!canStart()||!CallPolicy.allows(caller,false)||!CallPolicy.admit(caller,"private:"+str(found,"id"))){var b=new JsonObject();b.addProperty("id",str(found,"id"));CallControlService.extension("private-end",b);return;}
   if(!str(found,"status").equals("pending"))return;
   generation++;joining=false;joinAttempts=0;acceptedAt=joiningAt=0;context=ServerProfiles.current();started=System.currentTimeMillis();
   apply(found);PhoneNotification.show("Приватный вызов",peerName());SimpleVoiceCallClient.soundManager.playRing(true);return;
  }
  apply(found);
 }
 private static void apply(JsonObject value){
  // An older poll cannot undo acceptance or a published group.
  if(session!=null&&str(session,"status").equals("accepted")&&str(value,"status").equals("pending"))return;
  if(session!=null&&!str(session,"group").isEmpty()&&str(value,"group").isEmpty())return;
  boolean wasAccepted=session!=null&&str(session,"status").equals("accepted");
  session=value;lastSeen=System.currentTimeMillis();
  if(str(value,"status").equals("accepted")){if(!wasAccepted)acceptedAt=lastSeen;SimpleVoiceCallClient.soundManager.stopRing();status=connected?"Разговор":"Соединение…";}else status=source()?"Ожидаем ответа":"Входящий приватный вызов";
 }
 public static void accept(){
  if(!incoming()||requesting)return;
  if(SimpleVoiceCallClient.callManager.isInCall()||VoicechatPluginImpl.getOwnGroupId()!=null){end();return;}
  command("private-accept",body(),false);
 }
 private static void command(String action,JsonObject b,boolean snapshot){
  requesting=true;long epoch=generation;
  (snapshot?CallControlService.extensionWithSnapshot(action,b):CallControlService.extension(action,b)).whenComplete((v,e)->client().execute(()->{
   if(epoch!=generation)return;requesting=false;
   if(e!=null){finish("Не удалось соединить приватный звонок",true);return;}apply(v);
  }));
 }
 public static void tick(){
  var own=VoicechatPluginImpl.getOwnGroupId();
  if(!orphanName.isEmpty()){
   if(System.currentTimeMillis()>orphanUntil)orphanName="";
   else for(var g:VoicechatPluginImpl.getGroups())if(g.getId().equals(own)&&g.getName().equals(orphanName)){VoicechatPluginImpl.leaveGroup();orphanName="";break;}
  }
  if(!busy())return;
  long now=System.currentTimeMillis();
  if(client().player==null||!context.equals(ServerProfiles.current())||!VoicechatPluginImpl.isReady()||!PhoneDetector.hasPhone(client())){finish("Звонок завершён",true);return;}
  if(creating){if(now-started>12000)finish("Нет связи с оператором",false);return;}
  if(now-lastSeen>30000){finish("Связь с оператором потеряна",true);return;}
  if(!str(session,"status").equals("accepted"))return;
  String name=str(session,"name"),group=str(session,"group");
  if(source()){
   if(!creatingGroup){if(own!=null){finish("Голосовая линия занята",true);return;}creatingGroup=true;if(!VoicechatPluginImpl.createPrivateGroup(name,str(session,"password"))){finish("Не удалось создать группу",true);return;}}
   // The caller is already in the group reported by the SVC player-state packet.
   // Do not depend on the local group directory: NORMAL/password groups may not be listed yet.
   if(group.isEmpty()&&own!=null&&!requesting){var b=body();b.addProperty("group",own.toString());command("private-group",b,true);}
  }else if(!group.isEmpty()&&!joining){
   UUID id=UUID.fromString(group),caller=UUID.fromString(str(session,"source"));
   if(VoiceCallTransport.playerInGroup(caller,id)){joining=true;joiningAt=now;joinAttempts++;if(!VoicechatPluginImpl.joinGroup(id,str(session,"password")))finish("Не удалось войти в группу",true);}
  }else if(!source()&&joining&&!connected&&now-joiningAt>5000){
   UUID id=UUID.fromString(str(session,"group"));
   if(VoiceCallTransport.playerInGroup(UUID.fromString(str(session,"source")),id)){
    if(joinAttempts>=3){finish("Не удалось войти в приватную группу",true);return;}
    joining=false;
   }
  }
  if(!group.isEmpty()){
   UUID id=UUID.fromString(group),peer=UUID.fromString(str(session,source()?"target":"source"));
   boolean together=id.equals(VoicechatPluginImpl.getOwnGroupId())&&VoiceCallTransport.playerInGroup(peer,id);
   if(together){if(!connected)connectedAt=now;connected=true;status="Разговор";}
   else if(connected)finish("Разговор завершён",true);
  }
  if(!connected&&acceptedAt>0&&now-acceptedAt>20000)finish("Не удалось подключить приватную линию",true);
 }
 public static void end(){finish("Звонок завершён",true);}
 private static void finish(String message,boolean notify){
  if(!busy())return;
  if(session!=null){
   ended.add(str(session,"id"));if(ended.size()>32)ended.remove(ended.iterator().next());
   // The client tick can finish a call while the player is being detached. Do not
   // attempt an authenticated request after the Minecraft identity has vanished.
   if(notify&&client().player!=null)CallControlService.extension("private-end",body());
   UUID peer=UUID.fromString(str(session,source()?"target":"source"));
   dev.yukiinotenshi.simplephonepromax.call.CallManager.addHistory(peerName(),peer,connected?(source()?"completed_out":"completed_in"):(source()?"canceled_out":"missed_in"),connected?(int)((System.currentTimeMillis()-connectedAt)/1000):0);
   orphanName=str(session,"name");orphanUntil=System.currentTimeMillis()+10000;
   UUID own=VoicechatPluginImpl.getOwnGroupId();
   if(own!=null&&(creatingGroup||own.toString().equals(str(session,"group"))))VoicechatPluginImpl.leaveGroup();
  }
  generation++;creating=requesting=creatingGroup=joining=connected=false;session=null;pendingTarget=null;missingSince=acceptedAt=joiningAt=0;joinAttempts=0;
  SimpleVoiceCallClient.soundManager.stopRing();VoicechatPluginImpl.setMicrophoneMuted(false);status=message;
  PhoneMessages.show(message);
 }
}

