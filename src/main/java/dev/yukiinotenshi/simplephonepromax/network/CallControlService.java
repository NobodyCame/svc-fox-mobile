package dev.yukiinotenshi.simplephonepromax.network;

import com.google.gson.*;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.call.CallManager;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen;
import dev.yukiinotenshi.simplephonepromax.gui.CallOfferScreen;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDetector;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** All game state is accessed on the client thread; HTTP requests are serialized. */
public final class CallControlService {
   private static final Gson JSON = new Gson();
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
   private static CompletableFuture<?> tail = CompletableFuture.completedFuture(null);
   private static final Map<UUID, JsonObject> peers = new HashMap<>();
   private static final Map<UUID, Long> peerCapabilitySeen = new HashMap<>();
   private static final Set<String> handled = new HashSet<>();
   private static final Map<String, Long> deferred = new HashMap<>();
   private static final Map<UUID, UUID> groupOwners = new HashMap<>();
   private static final Set<String> handledRemovals = new HashSet<>();
   private static String pendingRemoval;
   private static boolean removing;
   private static String context = "";
   private static volatile long epoch;
   private static long lastPoll, lastSuccess, incomingAt;
   private static boolean polling, creating, deciding, completing;
   private static Offer incoming, outgoing;
   private static Offer joining;
   private static boolean cancelCreation;
   private static long joinDeadline;
   private static final Map<String, JsonObject> holds=new HashMap<>();
   private static Held held;
   private static Offer holdThenAnswer;
   private record Held(String id,UUID group,UUID anchor,String name,UUID created) {}
   public static boolean hasHeld(){return held!=null&&!held.group.equals(VoicechatPluginImpl.getOwnGroupId());}
   public static boolean keepsGroup(UUID group) {
      return available()&&holds.values().stream().anyMatch(h->group.equals(uuid(h,"group"))&&Set.of("preparing","holding").contains(str(h,"status")));
   }
   public static boolean canHoldIncoming() {
      MinecraftClient c=MinecraftClient.getInstance();
      return incoming!=null&&incoming.kind.equals("waiting")&&held==null&&!deciding&&calls().isInActiveCall()&&available()
         &&supports(c.player.getUuid(),"call_hold_v1")&&groupParticipants().stream().allMatch(p->supports(p.getUuid(),"call_hold_v1"));
   }
   public static void holdAndAnswer() {
      if(!canHoldIncoming())return;
      Offer offer=incoming;UUID group=VoicechatPluginImpl.getOwnGroupId();
      PlayerState anchor=groupParticipants().stream().filter(p->!p.getUuid().equals(MinecraftClient.getInstance().player.getUuid())).findFirst().orElse(null);
      if(anchor==null)return;
      String id=UUID.randomUUID().toString();JsonObject b=identity();b.addProperty("id",id);b.addProperty("group",group.toString());
      UUID created=VoiceCallTransport.getCreatedGroup();deciding=true;long generation=epoch;
      finish(post("poll",snapshot()).thenCompose(v->generation==epoch?post("hold",b):CompletableFuture.failedFuture(new IllegalStateException("session changed"))),(v,e)->{
         if(e!=null){deciding=false;message("phone.control.unavailable");return;}
         held=new Held(id,group,anchor.getUuid(),anchor.getName(),created);holdThenAnswer=offer;
      });
   }
   private static void handleHolds(JsonObject response) {
      holds.clear();
      if(response.has("holds"))for(JsonElement item:response.getAsJsonArray("holds")){JsonObject h=item.getAsJsonObject();holds.put(str(h,"id"),h);}
      if(held==null)return;
      JsonObject h=holds.get(held.id);
      if(h==null||!Set.of("preparing","holding").contains(str(h,"status"))) {
         held=null;holdThenAnswer=null;deciding=false;return;
      }
      if(holdThenAnswer!=null&&"holding".equals(str(h,"status"))) {
         Offer offer=holdThenAnswer;holdThenAnswer=null;deciding=false;
         if(incoming==null||!incoming.id.equals(offer.id)||!validIncoming(offer)){releaseHeld();return;}
         respond(offer,"accepted",true);
      }
   }
   public static void releaseHeld() {
      if(held==null)return;
      JsonObject b=identity();b.addProperty("id",held.id);held=null;holdThenAnswer=null;deciding=false;
      finish(post("hold-release",b),(v,e)->{});
   }
   public static void resumeHeld() {
      if(!hasHeld()||!available())return;
      Held saved=held;
      PlayerState peer=VoicechatPluginImpl.getPlayerStates().stream().filter(p->saved.group.equals(p.getGroup())&&!p.getUuid().equals(MinecraftClient.getInstance().player.getUuid())).findFirst().orElse(null);
      if(peer==null||!VoiceCallTransport.joinConsentedCall(saved.group,peer.getUuid(),peer.getName(),true)){message("phone.control.join_failed");return;}
      VoiceCallTransport.restoreCreatedGroup(saved.created);
      // Keep the backend hold until the polling snapshot confirms rejoining.
   }
   public static void receiveLegacyWaiting(UUID caller,String name,UUID group) {
      MinecraftClient c=MinecraftClient.getInstance();
      if(incoming!=null||!calls().isInActiveCall()||!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.allows(caller,true)||!PhoneDetector.hasPhone(c)||SimpleVoiceCallClient.config.isBlocked(caller)||SimpleVoiceCallClient.config.isBlocked(name))return;
      incoming=new Offer("local:"+group,"waiting",caller,name,c.player.getUuid(),c.player.getName().getString(),group,caller,VoicechatPluginImpl.getOwnGroupId(),"pending");
      incomingAt=System.currentTimeMillis();message("phone.waiting.arrived");
      if(c.currentScreen instanceof ActiveCallScreen)c.setScreen(new CallOfferScreen());
   }
   public static boolean canInvite(UUID target) {
      JsonObject peer=peers.get(target);
      return calls().isInActiveCall()&&outgoing==null&&!creating&&supports(target,"group_invite_v1")&&peer!=null&&"NONE".equals(str(peer,"state"))&&str(peer,"group").isEmpty()&&peer.get("phone").getAsBoolean();
   }
   public static boolean invite(UUID target) {
      if(!canInvite(target))return false;
      create("invite",target,VoicechatPluginImpl.getOwnGroupId(),MinecraftClient.getInstance().player.getUuid());return true;
   }

   public record Offer(String id, String kind, UUID source, String sourceName, UUID target,
                       String targetName, UUID group, UUID anchor, UUID targetGroup, String status) {
      static Offer read(JsonObject o) {
         return new Offer(str(o,"id"),str(o,"kind"),uuid(o,"source"),str(o,"source_name"),uuid(o,"target"),
            str(o,"target_name"),uuid(o,"group"),uuid(o,"anchor"),uuid(o,"target_group"),str(o,"status"));
      }
   }
   private static String str(JsonObject o,String key) { return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():""; }
   private static UUID uuid(JsonObject o,String key) { String s=str(o,key);return s.isBlank()?null:UUID.fromString(s); }
   private static CallManager calls() { return SimpleVoiceCallClient.callManager; }
   private static JsonArray successors=new JsonArray();
   private static String ownershipNotice="";
   public static JsonObject ownershipRequest(){var c=MinecraftClient.getInstance();if(c.player==null)return null;for(var item:successors){var r=item.getAsJsonObject();if(c.player.getUuid().toString().equals(str(r,"target"))&&!r.get("accepted").getAsBoolean())return r;}return null;}
   public static boolean canHandover(){return isGroupCreator()&&groupParticipants().stream().allMatch(p->supports(p.getUuid(),"group_owner_v1"));}
   public static void offerOwnership(UUID target,boolean deputy){if(!canHandover())return;JsonObject b=identity();b.addProperty("group",VoicechatPluginImpl.getOwnGroupId().toString());b.addProperty("target",target.toString());b.addProperty("deputy",deputy);finish(post("owner-offer",b),(v,e)->{if(e!=null)message("phone.control.unavailable");});}
   public static void respondOwnership(boolean accept){var request=ownershipRequest();if(request==null)return;JsonObject b=identity();b.addProperty("group",str(request,"group"));finish(post(accept?"owner-accept":"owner-decline",b),(v,e)->{if(e!=null)message("phone.control.unavailable");});}
   public static Offer incoming() { return incoming; }
   public static long incomingAt() { return incomingAt; }
   public static boolean deciding() { return deciding; }
   public static boolean hasOutgoingTransfer() { return creating || outgoing!=null&&outgoing.kind.equals("transfer"); }
   public static String status(){return context.isEmpty()?"Нет соединения с оператором":available()?"Сервис отвечает; возможности подтверждены":"Сервис не отвечает или данные устарели";}
   public static boolean available() { return !context.isEmpty() && System.currentTimeMillis()-lastSuccess<20000; }
   public static boolean supports(UUID player,String capability) {
      JsonObject peer=peers.get(player);
      if(!available()||peer==null||!peer.has("protocol")||peer.get("protocol").getAsInt()!=1||!peer.has("capabilities"))return false;
      for(JsonElement value:peer.getAsJsonArray("capabilities"))if(capability.equals(value.getAsString()))return true;
      return false;
   }
   public static boolean wasKnownCompatible(UUID player,String capability) {
      JsonObject peer=peers.get(player);Long seen=peerCapabilitySeen.get(player);
      if(peer==null||seen==null||System.currentTimeMillis()-seen>600000||!peer.has("capabilities"))return false;
      for(JsonElement value:peer.getAsJsonArray("capabilities"))if(capability.equals(value.getAsString()))return true;
      return false;
   }
   public static boolean rejectsCallForDoNotDisturb(UUID target) {
      if(target==null||!supports(target,"dnd_v1"))return false;
      JsonObject peer=peers.get(target);
      if(peer==null||!peer.has("dnd")||!peer.get("dnd").getAsBoolean())return false;
      MinecraftClient c=MinecraftClient.getInstance();
      if(c.player==null||!peer.has("dnd_allow"))return true;
      for(JsonElement value:peer.getAsJsonArray("dnd_allow"))if(c.player.getUuid().toString().equals(value.getAsString()))return false;
      return true;
   }
   public static CompletableFuture<JsonObject> diagnosticSnapshot(){return post("poll",snapshot());}
   public static Boolean hasPhone(UUID target){var p=peers.get(target);return available()&&p!=null?p.get("phone").getAsBoolean():null;}
   public static boolean canWait(UUID target) {
      JsonObject peer=peers.get(target);
      return supports(target,"call_waiting_v1")&&peer!=null&&"ACTIVE".equals(str(peer,"state"))&&peer.get("phone").getAsBoolean();
   }
   public static boolean canTransferTo(UUID target) {
      JsonObject peer=peers.get(target);
      return supports(target,"call_transfer_v1")&&peer!=null&&"NONE".equals(str(peer,"state"))&&str(peer,"group").isEmpty()&&peer.get("phone").getAsBoolean()
         &&(groupParticipants().stream().filter(p->!p.getUuid().equals(MinecraftClient.getInstance().player.getUuid())).count()<=1||supports(target,"group_transfer_v1"));
   }
   public static boolean canTransferCurrent() {
      MinecraftClient c=MinecraftClient.getInstance();
      if(c.player==null||!calls().isInActiveCall()||!available())return false;
      UUID group=VoicechatPluginImpl.getOwnGroupId();
      var others=VoicechatPluginImpl.getPlayerStates().stream()
         .filter(p->group!=null&&group.equals(p.getGroup())&&!p.getUuid().equals(c.player.getUuid())).toList();
      return !others.isEmpty()&&others.stream().allMatch(p->supports(p.getUuid(),others.size()>1?"group_transfer_v1":"call_transfer_v1"));
   }
   public static List<PlayerState> groupParticipants() {
      UUID group=VoicechatPluginImpl.getOwnGroupId();
      return VoicechatPluginImpl.getPlayerStates().stream().filter(p->group!=null&&group.equals(p.getGroup())).toList();
   }
   public static boolean isGroupCreator() {
      MinecraftClient c=MinecraftClient.getInstance();
      UUID group=VoicechatPluginImpl.getOwnGroupId();
      return c.player!=null&&available()&&calls().isInActiveCall()&&group!=null
         &&c.player.getUuid().equals(groupOwners.get(group));
   }
   public static boolean canRemove(UUID target) {
      MinecraftClient c=MinecraftClient.getInstance();
      UUID group=VoicechatPluginImpl.getOwnGroupId();
      if(!isGroupCreator()||removing||pendingRemoval!=null||target.equals(c.player.getUuid())
         ||!supports(target,"group_remove_v1")||!VoiceCallTransport.playerInGroup(target,group))return false;
      Set<UUID> members=new HashSet<>();members.add(c.player.getUuid());
      groupParticipants().forEach(p->members.add(p.getUuid()));
      return members.size()>=3&&members.stream().allMatch(memberId->supports(memberId,"group_remove_v1"));
   }
   public static boolean canManageGroup() {
      return isGroupCreator()&&groupParticipants().stream().anyMatch(p->canRemove(p.getUuid()));
   }
   public static void removeParticipant(UUID target) {
      if(!canRemove(target))return;
      UUID group=VoicechatPluginImpl.getOwnGroupId();
      JsonObject b=identity();String id=UUID.randomUUID().toString();
      b.addProperty("id",id);b.addProperty("group",group.toString());b.addProperty("target",target.toString());
      removing=true;long generation=epoch;
      finish(post("poll",snapshot()).thenCompose(v->generation==epoch?post("remove",b):CompletableFuture.failedFuture(new IllegalStateException("session changed"))),(v,e)->{
         removing=false;
         if(e!=null)message("phone.group.remove_failed");
         else {pendingRemoval=id;message("phone.group.remove_sent");}
      });
   }
   private static void handleRemoval(JsonObject command) {
      UUID self=MinecraftClient.getInstance().player.getUuid();
      String id=str(command,"id"),status=str(command,"status");
      if(id.equals(pendingRemoval)&&!status.equals("pending")) {
         pendingRemoval=null;message(status.equals("completed")?"phone.group.removed":"phone.group.remove_failed");
      }
      if(!self.equals(uuid(command,"target"))||!status.equals("pending")||handledRemovals.contains(id))return;
      UUID group=uuid(command,"group"),creator=uuid(command,"source");
      Set<UUID> members=new HashSet<>();members.add(self);groupParticipants().forEach(p->members.add(p.getUuid()));
      boolean valid=calls().isInActiveCall()&&group.equals(VoicechatPluginImpl.getOwnGroupId())
         &&creator.equals(groupOwners.get(group))&&VoiceCallTransport.playerInGroup(creator,group)
         &&supports(creator,"group_remove_v1")&&members.size()>=3
         &&members.stream().allMatch(memberId->supports(memberId,"group_remove_v1"));
      handledRemovals.add(id);
      if(valid) {
         calls().endCall();message("phone.group.you_removed");
      } else {
         JsonObject b=identity();b.addProperty("id",id);b.addProperty("result","rejected");
         finish(post("remove-result",b),(v,e)->{});
      }
   }
   public static void message(String key) {
      MinecraftClient c=MinecraftClient.getInstance();
      if(c.player!=null)dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages.show(Text.translatable(key).getString());
   }
   public static void deactivate() {
      if(!context.isEmpty()&&MinecraftClient.getInstance().player!=null) {
         JsonObject b=snapshot();b.addProperty("protocol",0);b.add("capabilities",new JsonArray());b.addProperty("state","NONE");post("poll",b);
      }
      reset();
   }
   public static void reset() {
      epoch++;context="";lastPoll=lastSuccess=0;polling=creating=deciding=completing=false;
      incoming=outgoing=joining=null;successors=new JsonArray();peers.clear();handled.clear();deferred.clear();
      peerCapabilitySeen.clear();groupOwners.clear();handledRemovals.clear();pendingRemoval=null;removing=false;holds.clear();held=null;holdThenAnswer=null;
   }
   private static String server(MinecraftClient c) {
      return c.getCurrentServerEntry()==null?"singleplayer":c.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
   }
   private static JsonObject identity() {
      MinecraftClient c=MinecraftClient.getInstance();
      JsonObject body=new JsonObject();
      body.addProperty("player",c.player.getUuid().toString());
      body.addProperty("server",server(c));
      body.addProperty("secret",SimpleVoiceCallClient.config.callControlSecret);
      return body;
   }
   private static JsonObject snapshot() {
      MinecraftClient c=MinecraftClient.getInstance();
      JsonObject b=identity();
      b.addProperty("name",c.player.getName().getString());
      UUID group=VoicechatPluginImpl.getOwnGroupId();
      b.addProperty("group",group==null?null:group.toString());
      b.addProperty("state",PrivateCalls.hasSession()?"BUSY":calls().getState().name());
      b.addProperty("phone",PhoneDetector.hasPhone(c));
      b.addProperty("protocol",1);
      b.addProperty("dnd",dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.dndActive());
      JsonArray dndAllow=new JsonArray();
      if(dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.favoritesOnly()){
         String prefix=dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current()+"/";
         for(String favorite:SimpleVoiceCallClient.config.favoriteContacts)try{
            String value=favorite.startsWith(prefix)?favorite.substring(prefix.length()):favorite;
            dndAllow.add(UUID.fromString(value).toString());
         }catch(IllegalArgumentException ignored){}
      }
      b.add("dnd_allow",dndAllow);
      JsonArray features=new JsonArray();features.add("private_call_v1");features.add("callback_v1");features.add("call_waiting_v1");features.add("call_transfer_v1");features.add("group_remove_v1");features.add("group_invite_v1");features.add("group_transfer_v1");features.add("call_hold_v1");features.add("group_owner_v1");features.add("organization_queue_v1");features.add("dnd_v1");if(VoicemailClient.ENABLED)features.add("voicemail_v1");
      if(EncryptedCalls.supported())features.add("encrypted_voice_v1");
      b.add("capabilities",features);
      UUID created=VoiceCallTransport.getCreatedGroup();
      b.addProperty("created_group",created==null?null:created.toString());
      Set<UUID> ids=new HashSet<>();
      if(group!=null) {ids.add(c.player.getUuid());groupParticipants().forEach(p->ids.add(p.getUuid()));}
      JsonArray members=new JsonArray();ids.forEach(id->members.add(id.toString()));b.add("members",members);
      JsonArray acks=new JsonArray();holds.values().stream().filter(h->group!=null&&group.equals(uuid(h,"group"))&&Set.of("preparing","holding").contains(str(h,"status"))).forEach(h->acks.add(str(h,"id")));b.add("hold_acks",acks);
      return b;
   }
   public static CompletableFuture<JsonObject> extensionWithSnapshot(String action,JsonObject fields){
      if(MinecraftClient.getInstance().player==null)return CompletableFuture.failedFuture(new IllegalStateException("player disconnected"));
      return post("poll",snapshot()).thenCompose(v->extension(action,fields));
   }
   public static CompletableFuture<JsonObject> extension(String action,JsonObject fields){
      if(MinecraftClient.getInstance().player==null)return CompletableFuture.failedFuture(new IllegalStateException("player disconnected"));
      JsonObject b=identity();fields.entrySet().forEach(e->b.add(e.getKey(),e.getValue()));return post(action,b);
   }
   private static synchronized CompletableFuture<JsonObject> post(String action, JsonObject body) {
      try {
         String base=SimpleVoiceCallClient.config.backendBaseUrl.replaceAll("/+$","");
         HttpRequest request=HttpRequest.newBuilder(URI.create(base+"/calls/"+action))
            .timeout(Duration.ofSeconds(5)).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(body))).build();
         CompletableFuture<JsonObject> next=tail.handle((v,e)->null).thenCompose(v->dev.yukiinotenshi.simplephonepromax.network.OperatorHttp.client(request.uri()).sendAsync(request,HttpResponse.BodyHandlers.ofString()))
            .thenApply(response->{if(response.statusCode()!=200){
               String detail=response.body()==null?"":response.body().trim();
               try {
                  JsonObject error=JSON.fromJson(detail,JsonObject.class);
                  if(error!=null&&error.has("error"))detail=error.get("error").getAsString();
               } catch(Exception ignored) { }
               if(detail.length()>120)detail=detail.substring(0,120);
               throw new IllegalStateException("HTTP "+response.statusCode()+(detail.isBlank()?"":": "+detail));}
               return JSON.fromJson(response.body(),JsonObject.class);});
         tail=next;return next;
      } catch(Exception e) {return CompletableFuture.failedFuture(e);}
   }
   private static void finish(CompletableFuture<JsonObject> request, java.util.function.BiConsumer<JsonObject,Throwable> result) {
      long generation=epoch;
      request.whenComplete((value,error)->MinecraftClient.getInstance().execute(()->{
         if(generation==epoch)result.accept(value,error);
      }));
   }
   public static void tick() {
      MinecraftClient c=MinecraftClient.getInstance();
      if(incoming!=null&&incoming.id.startsWith("local:")&&(System.currentTimeMillis()-incomingAt>45000||!validIncoming(incoming)))incoming=null;
      if(dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()||c.player==null||!VoicechatPluginImpl.isReady()||!SimpleVoiceCallClient.config.backendNumbersEnabled
         ||SimpleVoiceCallClient.config.backendBaseUrl.isBlank()) {if(!context.isEmpty())reset();return;}
      String key=c.player.getUuid()+"@"+server(c)+"@"+SimpleVoiceCallClient.config.backendBaseUrl;
      if(!key.equals(context)) {reset();context=key;}
      if(SimpleVoiceCallClient.config.callControlSecret==null||SimpleVoiceCallClient.config.callControlSecret.isBlank()) {
         SimpleVoiceCallClient.config.callControlSecret=UUID.randomUUID().toString()+UUID.randomUUID();
         SimpleVoiceCallClient.config.save();
      }
      long now=System.currentTimeMillis();
      if(joining!=null) {
         boolean connected=joining.group.equals(VoicechatPluginImpl.getOwnGroupId())&&calls().isInActiveCall();
         if(connected&&(!joining.kind.equals("transfer")||(!VoiceCallTransport.playerInGroup(joining.source,joining.group)
            &&VoiceCallTransport.playerInGroup(joining.anchor,joining.group))))joining=null;
         else if(now>joinDeadline) {
            Offer failed=joining;joining=null;respond(failed,"failed",false);
            if(calls().getState()==CallState.INCOMING_RINGING||connected)calls().endCall();
            message("phone.control.join_failed");
         }
      }
      if(outgoing!=null) {
         Offer offer=outgoing;
         boolean validSource=offer.group.equals(VoicechatPluginImpl.getOwnGroupId())
            &&(offer.kind.equals("transfer")?calls().isInActiveCall()&&VoiceCallTransport.playerInGroup(offer.anchor,offer.group):calls().isInCall());
         if(!validSource)cancelOutgoing();
         else if("accepted".equals(offer.status)&&VoiceCallTransport.playerInGroup(offer.target,offer.group)&&!completing) {
            completing=true;JsonObject b=identity();b.addProperty("id",offer.id);
            finish(post("complete",b),(v,e)->{
               completing=false;
               if(e==null&&outgoing!=null&&outgoing.id.equals(offer.id)) {
                  outgoing=null;
                  finishTransfer(offer);
               }
            });
         }
      }
      if(!polling&&now-lastPoll>=1000) {
         polling=true;lastPoll=now;
         finish(post("poll",snapshot()),(v,e)->{
            polling=false;
            if(e!=null)return;
            lastSuccess=System.currentTimeMillis();
            java.util.Set<UUID> seenPeers=new java.util.HashSet<>();for(JsonElement item:v.getAsJsonArray("peers")) {JsonObject p=item.getAsJsonObject();UUID id=uuid(p,"player");seenPeers.add(id);peers.put(id,p);peerCapabilitySeen.put(id,System.currentTimeMillis());if(p.has("operator_number")&&!p.get("operator_number").isJsonNull())dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService.observePeerNumber(id,str(p,"name"),str(p,"operator_number"));}peers.keySet().retainAll(seenPeers);
            groupOwners.clear();
            if(v.has("callbacks"))CallbackRequests.poll(v.getAsJsonArray("callbacks"));
            if(v.has("private"))PrivateCalls.poll(v.getAsJsonArray("private"));
            if(v.has("secure"))EncryptedCalls.poll(v.getAsJsonArray("secure"));
            successors=v.has("successors")?v.getAsJsonArray("successors"):new JsonArray();
            var ownerRequest=ownershipRequest();String ownerKey=ownerRequest==null?"":ownerRequest.toString();
            if(!ownerKey.equals(ownershipNotice)&&ownerRequest!=null&&MinecraftClient.getInstance().player!=null)MinecraftClient.getInstance().player.sendMessage(Text.literal("Предложено управление группой — откройте настройки").formatted(net.minecraft.util.Formatting.YELLOW),false);
            ownershipNotice=ownerKey;
            if(v.has("owners"))v.getAsJsonObject("owners").entrySet().forEach(ownerEntry->groupOwners.put(UUID.fromString(ownerEntry.getKey()),UUID.fromString(ownerEntry.getValue().getAsString())));
            handleHolds(v);
            if(v.has("removals"))for(JsonElement item:v.getAsJsonArray("removals"))handleRemoval(item.getAsJsonObject());
            for(JsonElement item:v.getAsJsonArray("offers"))handle(Offer.read(item.getAsJsonObject()));
         });
      }
      if(incoming!=null&&!validIncoming(incoming)) {Offer old=incoming;incoming=null;respond(old,"declined",false);}
   }
   private static void handle(Offer offer) {
      UUID self=MinecraftClient.getInstance().player.getUuid();
      if(offer.source.equals(self)&&outgoing!=null&&outgoing.id.equals(offer.id)) {
         outgoing=offer;
         if(!Set.of("pending","accepted").contains(offer.status)) {
            outgoing=null;
            if(offer.status.equals("completed"))finishTransfer(offer);
            if(!offer.status.equals("completed")) {
               message("phone.control.not_accepted");
               if(offer.kind.equals("waiting")&&calls().getState()==CallState.OUTGOING_RINGING) {
                  VoiceCallTransport.leaveGroup();calls().remoteDecline();
               }
            }
         }
      }
      if(!offer.target.equals(self))return;
      if(!offer.status.equals("pending")) {
         if(incoming!=null&&incoming.id.equals(offer.id))incoming=null;
         if(joining!=null&&joining.id.equals(offer.id)&&!offer.status.equals("accepted")) {
            joining=null;
            if(!offer.status.equals("completed")&&offer.group.equals(VoicechatPluginImpl.getOwnGroupId())) {
               calls().endCall();message("phone.control.not_accepted");
            }
         }
         return;
      }
      if(handled.contains(offer.id))return;
      if(!validIncoming(offer)) {
         long first=deferred.computeIfAbsent(offer.id,id->System.currentTimeMillis());
         if(System.currentTimeMillis()-first>=3000) {handled.add(offer.id);deferred.remove(offer.id);respond(offer,"declined",false);}
         return;
      }
      if(!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.admit(offer.source,"offer:"+offer.id)){handled.add(offer.id);respond(offer,"declined",false);return;}
      deferred.remove(offer.id);
      handled.add(offer.id);incoming=offer;incomingAt=System.currentTimeMillis();
      message(offer.kind.equals("waiting")?"phone.waiting.arrived":"phone.transfer.arrived");
      MinecraftClient c=MinecraftClient.getInstance();
      if(c.currentScreen instanceof ActiveCallScreen)c.setScreen(new CallOfferScreen());
   }
   public static boolean validIncoming(Offer o) {
      MinecraftClient c=MinecraftClient.getInstance();
      if(c.player==null||!PhoneDetector.hasPhone(c)||!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.allows(o.source,o.kind.equals("waiting"))||SimpleVoiceCallClient.config.isBlocked(o.source)
         ||SimpleVoiceCallClient.config.isBlocked(o.sourceName)||!VoiceCallTransport.playerInGroup(o.source,o.group))return false;
      if(o.id.startsWith("local:"))return calls().isInActiveCall()&&Objects.equals(VoicechatPluginImpl.getOwnGroupId(),o.targetGroup)&&VoiceCallTransport.isIncomingGroupFor(VoiceCallTransport.findGroup(o.group),c.player.getUuid());
      String capability=o.kind.equals("waiting")?"call_waiting_v1":o.kind.equals("invite")?"group_invite_v1":"call_transfer_v1";
      if(!supports(o.source,capability)||(o.kind.equals("transfer")&&!supports(o.anchor,capability)))return false;
      Group group=VoiceCallTransport.findGroup(o.group);
      if(group==null||!group.getName().startsWith("C")||group.getName().length()!=23)return false;
      PlayerState source=VoiceCallTransport.findState(o.source);
      if(source==null||SimpleVoiceCallClient.config.isBlocked(source.getName()))return false;
      UUID own=VoicechatPluginImpl.getOwnGroupId();
      if(o.kind.equals("waiting"))return VoiceCallTransport.isIncomingGroupFor(group,c.player.getUuid())
         &&((calls().isInActiveCall()&&Objects.equals(own,o.targetGroup))||(!calls().isInCall()&&own==null));
      return !calls().isInCall()&&own==null&&VoiceCallTransport.playerInGroup(o.anchor,o.group)
         &&!SimpleVoiceCallClient.config.isBlocked(o.anchor);
   }
   public static void acceptIncoming() {
      if(incoming==null||deciding)return;
      Offer offer=incoming;
      if(!validIncoming(offer)) {incoming=null;message("phone.control.unavailable");return;}
      respond(offer,"accepted",true);
   }
   public static void declineIncoming() {
      if(incoming==null||deciding)return;
      respond(incoming,"declined",true);
   }
   private static void respond(Offer offer,String decision,boolean interactive) {
      if(offer.id.startsWith("local:")) {
         if(decision.equals("accepted")&&validIncoming(offer)) {
            if(!VoiceCallTransport.joinConsentedCall(offer.group,offer.source,offer.sourceName,true)){message("phone.control.join_failed");return;}
         }
         incoming=null;
         if(interactive)MinecraftClient.getInstance().setScreen(calls().isInCall()?new ActiveCallScreen():null);
         return;
      }
      if(context.isEmpty())return;
      if(interactive)deciding=true;
      JsonObject b=identity();b.addProperty("id",offer.id);b.addProperty("decision",decision);
      finish(post("respond",b),(v,e)->{
         if(interactive)deciding=false;
         if(e!=null) {if(interactive)message("phone.control.unavailable");return;}
         if(incoming!=null&&incoming.id.equals(offer.id))incoming=null;
         if(decision.equals("accepted")) {
            if(!validIncoming(offer)) {respond(offer,"failed",false);message("phone.control.unavailable");return;}
            PlayerState anchor=VoiceCallTransport.findState(offer.anchor);
            String name=anchor!=null?anchor.getName():offer.sourceName;
            if(!VoiceCallTransport.joinConsentedCall(offer.group,offer.anchor,name,offer.kind.equals("waiting"))) {
               respond(offer,"failed",false);message("phone.control.join_failed");return;
            }
            joining=offer;joinDeadline=System.currentTimeMillis()+12000;
         } else if(interactive)CallManager.addHistory(offer.sourceName,offer.source,"declined_in",0);
         if(interactive)MinecraftClient.getInstance().setScreen(calls().isInCall()?new ActiveCallScreen():null);
      });
   }
   public static void ensureWaiting(UUID target,UUID group) {
      if(outgoing!=null||creating||!canWait(target))return;
      create("waiting",target,group,MinecraftClient.getInstance().player.getUuid());
   }
   public static void transfer(UUID target) {
      UUID group=VoicechatPluginImpl.getOwnGroupId();
      UUID self=MinecraftClient.getInstance().player.getUuid();
      List<PlayerState> others=VoicechatPluginImpl.getPlayerStates().stream()
         .filter(p->group!=null&&group.equals(p.getGroup())&&!self.equals(p.getUuid())).toList();
      if(!calls().isInActiveCall()||!canTransferTo(target)||others.isEmpty()||others.getFirst().getUuid().equals(target)
         ||outgoing!=null||creating) {message("phone.transfer.unavailable");return;}
      if(!canTransferCurrent()) {message("phone.control.incompatible");return;}
      create("transfer",target,group,others.getFirst().getUuid());
   }
   private static void create(String kind,UUID target,UUID group,UUID anchor) {
      creating=true;cancelCreation=false;long generation=epoch;JsonObject b=identity();b.addProperty("id",UUID.randomUUID().toString());
      b.addProperty("kind",kind);b.addProperty("target",target.toString());b.addProperty("group",group.toString());b.addProperty("anchor",anchor.toString());
      finish(post("poll",snapshot()).thenCompose(v->generation==epoch?post("offer",b):CompletableFuture.failedFuture(new IllegalStateException("session changed"))),(v,e)->{
         creating=false;
         if(e!=null) {
            message("phone.control.unavailable");
            if(kind.equals("waiting")&&calls().getState()==CallState.OUTGOING_RINGING)calls().remoteBusy();
            return;
         }
         outgoing=Offer.read(v);
         if(cancelCreation||!group.equals(VoicechatPluginImpl.getOwnGroupId())) {cancelOutgoing();return;}
         if(kind.equals("transfer")||kind.equals("invite")) {
            message(kind.equals("transfer")?"phone.transfer.sent":"phone.group.invite_sent");MinecraftClient.getInstance().setScreen(new ActiveCallScreen());
         }
      });
   }
   public static void cancelOutgoing() {
      if(creating)cancelCreation=true;
      if(outgoing==null)return;
      Offer offer=outgoing;outgoing=null;
      JsonObject b=identity();b.addProperty("id",offer.id);finish(post("cancel",b),(v,e)->{});
   }
   private static void finishTransfer(Offer offer) {
      if(offer.kind.equals("transfer")&&calls().isInActiveCall()
         &&offer.group.equals(VoicechatPluginImpl.getOwnGroupId())
         &&VoiceCallTransport.playerInGroup(offer.target,offer.group)
         &&VoiceCallTransport.playerInGroup(offer.anchor,offer.group)) {
         CallManager.addHistory(offer.targetName,offer.target,"transferred",0);
         calls().endCall();message("phone.transfer.completed");
      }
   }
}

