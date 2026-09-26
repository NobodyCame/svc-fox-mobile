package dev.yukiinotenshi.simplephonepromax.network;
import com.google.gson.*;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.opus.*;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class VoicemailClient {
 public static final boolean ENABLED=true;
 private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 private static final ScheduledExecutorService AUDIO=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"phone-mailbox");t.setDaemon(true);return t;});
 private static final List<JsonObject> inbox=new ArrayList<>();
 private static final List<JsonObject> outbox=new ArrayList<>();
 private static final JsonArray recording=new JsonArray();
 private static OpusEncoder encoder;private static volatile boolean capturing,sending;
 private static UUID target;private static String account="",recordAccount="",status="";private static long nextPoll,recordAt,epoch;private static boolean polling;
 private static ScheduledFuture<?> playback;
 private static OpusDecoder playingDecoder;private static long playEpoch,lastMicFrameAt;
 private static final Set<String> deleted=new HashSet<>();
 public static synchronized boolean busy(){return capturing||sending;}
 public static boolean recording(){return capturing;}public static boolean sending(){return sending;}
 public static String status(){return status;}public static long seconds(){return capturing?(System.currentTimeMillis()-recordAt)/1000:0;}
 public static List<JsonObject> messages(){return List.copyOf(inbox);}
 public static List<JsonObject> sentMessages(){return List.copyOf(outbox);}
 public static long micFrameAgeMs(){return lastMicFrameAt==0?-1:System.currentTimeMillis()-lastMicFrameAt;}
 private static Path folder(){return FabricLoader.getInstance().getConfigDir().resolve(SimpleVoiceCallClient.MOD_ID).resolve("voicemail").resolve(UUID.nameUUIDFromBytes(account.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());}
 private static Path path(String id){return folder().resolve(UUID.fromString(id)+".json");}
 private static void saveDeleted(){try{Files.createDirectories(folder());Files.writeString(folder().resolve("deleted.json"),JSON.toJson(deleted));}catch(Exception ignored){}}
 private static void saveIndex(){try{Files.createDirectories(folder());Files.writeString(folder().resolve("index.json"),JSON.toJson(inbox));}catch(Exception e){status="Не удалось сохранить список сообщений";}}
 private static JsonObject body(String id){JsonObject b=new JsonObject();b.addProperty("id",id);return b;}
 public static CompletableFuture<Boolean> eligible(UUID player){if(!ENABLED||!CallControlService.available()||player==null)return CompletableFuture.completedFuture(false);JsonObject b=new JsonObject();b.addProperty("target",player.toString());return CallControlService.extension("vm-eligible",b).thenApply(v->v.get("eligible").getAsBoolean()).exceptionally(e->false);}
 public static synchronized boolean start(UUID player){
  if(!ENABLED){status="Автоответчик пока неактивен";return false;}
  if(player==null||busy()||EncryptedCalls.active()||SimpleVoiceCallClient.callManager.isInCall()||VoicechatPluginImpl.getOwnGroupId()!=null||EncryptedCalls.clientApi()==null||!CallControlService.available())return false;
  try{encoder=EncryptedCalls.clientApi().createEncoder();if(encoder==null)throw new IllegalStateException("SVC encoder unavailable");recording.asList().clear();target=player;recordAccount=account;recordAt=System.currentTimeMillis();capturing=true;status="Запись · микрофон SVC";return true;}
  catch(Throwable e){if(encoder!=null)encoder.close();encoder=null;status="Не удалось открыть микрофон SVC";return false;}
 }
 public static synchronized void capture(ClientSoundEvent event){
  short[] samples=event.getRawAudio();if(samples!=null&&samples.length>0)lastMicFrameAt=System.currentTimeMillis();
  if(!capturing)return;event.cancel();
  try{if(samples.length>0&&recording.size()<3000)recording.add(Base64.getEncoder().encodeToString(encoder.encode(samples)));}
  catch(Exception e){capturing=false;if(encoder!=null)encoder.close();encoder=null;recording.asList().clear();status="Ошибка записи микрофона SVC";}
 }
 public static synchronized void cancel(){boolean wasCapturing=capturing;capturing=false;if(encoder!=null)encoder.close();encoder=null;recording.asList().clear();if(wasCapturing)status="Запись отменена";}
 public static synchronized void send(){
  if(!capturing)return;capturing=false;if(encoder!=null)encoder.close();encoder=null;
  if(recording.isEmpty()){status="Запись пуста — проверьте микрофон и PTT SVC";return;}
  JsonArray audio=recording.deepCopy();recording.asList().clear();sending=true;String id=UUID.randomUUID().toString();JsonObject b=body(id);b.addProperty("target",target.toString());long generation=epoch;
  CompletableFuture<JsonObject> chain=CallControlService.extension("vm-start",b);
  for(int from=0;from<audio.size();from+=40){JsonObject part=body(id);part.addProperty("part",from/40);JsonArray batch=new JsonArray();for(int i=from;i<Math.min(audio.size(),from+40);i++)batch.add(audio.get(i));part.add("frames",batch);chain=chain.thenCompose(v->requestOnClient("vm-part",part,generation));}
  chain=chain.thenCompose(v->requestOnClient("vm-finish",body(id),generation));
  chain.whenComplete((v,e)->MinecraftClient.getInstance().execute(()->{if(generation!=epoch)return;sending=false;status=e==null?"Сообщение отправлено":"Не удалось отправить сообщение";PhoneMessages.show(status);if(e!=null&&CallControlService.available())CallControlService.extension("vm-delete",body(id));}));
 }
 private static CompletableFuture<JsonObject> requestOnClient(String action,JsonObject b,long generation){CompletableFuture<JsonObject> result=new CompletableFuture<>();MinecraftClient.getInstance().execute(()->{if(generation!=epoch){result.completeExceptionally(new IllegalStateException("session changed"));return;}CallControlService.extension(action,b).whenComplete((v,e)->{if(e!=null)result.completeExceptionally(e);else result.complete(v);});});return result;}
 public static void tick(){
  if(!ENABLED)return;
  var c=MinecraftClient.getInstance();String current=c.player==null?"":ServerProfiles.current()+"/"+c.player.getUuid();
  if(!current.equals(account)){epoch++;cancel();sending=false;stopPlayback();inbox.clear();outbox.clear();deleted.clear();account=current;polling=false;nextPoll=0;
   if(!account.isEmpty())try{Path tombstones=folder().resolve("deleted.json");if(Files.isRegularFile(tombstones)&&Files.size(tombstones)<1048576)for(var id:JsonParser.parseString(Files.readString(tombstones)).getAsJsonArray())deleted.add(id.getAsString());}catch(Exception ignored){}
   if(!account.isEmpty())try{Path index=folder().resolve("index.json");if(Files.isRegularFile(index)&&Files.size(index)<1048576)for(var e:JsonParser.parseString(Files.readString(index)).getAsJsonArray())inbox.add(e.getAsJsonObject());}catch(Exception ignored){}
  }
  if(capturing&&System.currentTimeMillis()-recordAt>=60000)send();
  if(account.isEmpty()||!CallControlService.available()||polling||System.currentTimeMillis()<nextPoll)return;
  // User deletion of an already downloaded file also deletes its inbox record.
  for(var entry:new ArrayList<>(inbox))if(!Files.isRegularFile(path(entry.get("id").getAsString()))){delete(entry);}
  for(String id:List.copyOf(deleted))CallControlService.extension("vm-delete",body(id));
  polling=true;nextPoll=System.currentTimeMillis()+15000;long generation=epoch;
  CallControlService.extension("vm-list",new JsonObject()).whenComplete((v,e)->c.execute(()->{if(generation!=epoch)return;polling=false;if(e!=null)return;
   outbox.clear();if(v.has("sent")&&v.get("sent").isJsonArray())for(var sent:v.getAsJsonArray("sent"))outbox.add(sent.getAsJsonObject().deepCopy());
   Set<String> remoteIds=new HashSet<>();for(var remote:v.getAsJsonArray("messages"))remoteIds.add(remote.getAsJsonObject().get("id").getAsString());deleted.removeIf(id->!remoteIds.contains(id));saveDeleted();
   for(var item:v.getAsJsonArray("messages")){var entry=item.getAsJsonObject();String id=entry.get("id").getAsString();
    if(deleted.contains(id))continue;
    if("expired".equals(entry.get("status").getAsString())){PhoneMessages.show(entry.get("name").getAsString()+": голосовое удалено — истекли 72 часа");CallControlService.extension("vm-delete",body(id));continue;}
    if(inbox.stream().anyMatch(local->id.equals(local.get("id").getAsString()))) {CallControlService.extension("vm-ack",body(id));continue;}
    download(entry,generation);
   }
  }));
 }
 private static void download(JsonObject entry,long generation){String id=entry.get("id").getAsString();Path destination=path(id);
  CallControlService.extension("vm-download",body(id)).thenApplyAsync(v->{try{
   JsonArray frames=v.getAsJsonArray("frames");if(frames.isEmpty()||frames.size()>3000)throw new IllegalArgumentException("audio size");
   for(var f:frames)if(Base64.getDecoder().decode(f.getAsString()).length>2048)throw new IllegalArgumentException("audio frame size");
   Files.createDirectories(destination.getParent());Path temp=destination.resolveSibling(id+".tmp");Files.writeString(temp,JSON.toJson(v));Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);return true;
  }catch(Exception ex){return false;}},AUDIO).whenComplete((ok,e)->MinecraftClient.getInstance().execute(()->{if(generation!=epoch||e!=null||!Boolean.TRUE.equals(ok))return;
   if(inbox.stream().noneMatch(local->id.equals(local.get("id").getAsString())))inbox.add(entry);saveIndex();CallControlService.extension("vm-ack",body(id));dev.yukiinotenshi.simplephonepromax.gui.PhoneNotification.show("Голосовое сообщение",entry.get("name").getAsString(),"chaos");
  }));
 }
 public static void delete(JsonObject entry){String id=entry.get("id").getAsString();try{Files.deleteIfExists(path(id));}catch(Exception e){status="Не удалось удалить файл";return;}inbox.remove(entry);deleted.add(id);saveDeleted();saveIndex();if(CallControlService.available())CallControlService.extension("vm-delete",body(id));}
 public static synchronized void stopPlayback(){playEpoch++;if(playback!=null)playback.cancel(false);playback=null;if(playingDecoder!=null&&!playingDecoder.isClosed())playingDecoder.close();playingDecoder=null;}
 public static synchronized void play(JsonObject entry){
  stopPlayback();if(EncryptedCalls.clientApi()==null)return;Path file=path(entry.get("id").getAsString());
  long generation=playEpoch;
  AUDIO.execute(()->{try{
   if(Files.size(file)>4194304)throw new IllegalArgumentException();var frames=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("frames");if(frames.size()>3000)throw new IllegalArgumentException();
   OpusDecoder decoder=EncryptedCalls.clientApi().createDecoder();ClientStaticAudioChannel channel=EncryptedCalls.clientApi().createStaticAudioChannel(UUID.randomUUID());int[] position={0};
   synchronized(VoicemailClient.class){if(generation!=playEpoch){decoder.close();return;}playingDecoder=decoder;playback=AUDIO.scheduleAtFixedRate(()->{synchronized(VoicemailClient.class){try{if(position[0]>=frames.size()){decoder.close();CallControlService.extension("vm-listen",body(entry.get("id").getAsString()));stopPlayback();return;}channel.play(decoder.decode(Base64.getDecoder().decode(frames.get(position[0]++).getAsString())));}catch(Exception e){decoder.close();stopPlayback();}}},0,20,TimeUnit.MILLISECONDS);}
  }catch(Exception e){status="Запись недоступна";}});
 }
}

