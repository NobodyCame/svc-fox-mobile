package dev.yukiinotenshi.simplephonepromax.gui;

import com.google.gson.JsonObject;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import dev.yukiinotenshi.simplephonepromax.network.OperatorHttp;
import dev.yukiinotenshi.simplephonepromax.network.VoicemailClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Read-only checks for operator reachability, client compatibility and SVC audio events. */
public final class ConnectionDiagnosticsScreen extends Screen {
 private final Screen parent;
 private final List<ButtonWidget> buttons=new ArrayList<>();
 private String result="Нажмите «Проверить связь»";
 private volatile String healthFailure="",sessionFailure="";
 private int healthCode=-1,foxPeers=-1;
 private boolean sessionOk,dndSynced,voicemailEncrypted,checking;

 public ConnectionDiagnosticsScreen(Screen parent){super(Text.literal("Диагностика связи"));this.parent=parent;}

 protected void init(){
  buttons.clear();var f=PhoneGuiTextures.layout(width,height);int x=f.x()+f.px(16),w=f.width()-f.px(32),h=Math.max(14,f.px(13));
  var check=ButtonWidget.builder(Text.literal(checking?"Проверка…":"Проверить связь"),b->check()).dimensions(x,f.y()+f.px(43),w,h).build();check.active=!checking;add(check);
  add(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x,f.bottom()-h-f.px(4),w,h).build());
 }
 private void add(ButtonWidget button){buttons.add(button);addDrawableChild(button);}

 private void check(){
  if(checking||client==null||client.player==null)return;
  checking=true;result="Проверяю оператора и игровой клиент…";healthCode=-1;foxPeers=-1;sessionOk=false;dndSynced=false;voicemailEncrypted=false;healthFailure=sessionFailure="";clearAndInit();
  try{
   URI uri=URI.create(SimpleVoiceCallClient.config.backendBaseUrl.replaceAll("/+$","")+"/health");
   HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).GET().build();
   CompletableFuture<Integer> health=OperatorHttp.client(uri).sendAsync(request,HttpResponse.BodyHandlers.discarding()).thenApply(HttpResponse::statusCode).exceptionally(error->{healthFailure=errorText(error);return -1;});
   CompletableFuture<JsonObject> control=CallControlService.diagnosticSnapshot().whenComplete((value,error)->{if(error!=null)sessionFailure=errorText(error);});
   CompletableFuture<JsonObject> mailbox=control.thenCompose(snapshot->CallControlService.extension("vm-list",new JsonObject())).exceptionally(error->null);
   health.thenCombine(control.handle((v,e)->e==null?v:null).thenCombine(mailbox,(snapshot,list)->new JsonObject[]{snapshot,list}),
      (code,data)->new Probe(code,data[0],data[1])).whenComplete((probe,error)->MinecraftClient.getInstance().execute(()->{
       checking=false;
       if(error!=null||probe==null){result="Не удалось завершить проверку";clearAndInit();return;}
       healthCode=probe.health;
       JsonObject snapshot=probe.snapshot;
       if(snapshot!=null&&snapshot.has("peers")){
        String self=client.player.getUuid().toString();foxPeers=0;
        for(var element:snapshot.getAsJsonArray("peers")){
         JsonObject peer=element.getAsJsonObject();String id=peer.has("player")?peer.get("player").getAsString():"";
         if(self.equals(id)){sessionOk=true;if(peer.has("capabilities"))for(var cap:peer.getAsJsonArray("capabilities"))if("dnd_v1".equals(cap.getAsString()))dndSynced=true;}
         else if(peer.has("protocol")&&peer.get("protocol").getAsInt()==1)foxPeers++;
        }
       }
       voicemailEncrypted=probe.mailbox!=null&&probe.mailbox.has("storage_encryption")&&"fernet-v1".equals(probe.mailbox.get("storage_encryption").getAsString());
       result="Проверка завершена";clearAndInit();
      }));
  }catch(Exception error){checking=false;result="Не удалось начать проверку";clearAndInit();}
 }

 private record Probe(int health,JsonObject snapshot,JsonObject mailbox){}
 private static String yes(boolean value){return value?"OK":"нет";}

 public void render(DrawContext context,int mouseX,int mouseY,float delta){
  dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(context,width,height);var f=PhoneGuiTextures.layout(width,height);
  PhoneGuiTextures.drawUniversalBackground(context,f,mouseX,mouseY);context.drawCenteredTextWithShadow(textRenderer,title,f.centerX(),f.y()+12,0xFFFFFFFF);
  int y=f.y()+f.px(64),step=f.px(11),lineW=f.width()-f.px(20);
  PhoneGuiTextures.drawCenteredTrimmedText(context,textRenderer,Text.literal(result),f.centerX(),y,lineW,0xFFFFFF55);
  y+=step;
  PhoneGuiTextures.drawCenteredTrimmedText(context,textRenderer,Text.literal("HTTPS: "+(healthCode<0?"—":healthCode==200?"OK":"HTTP "+healthCode)+" · сессия: "+yes(sessionOk)),f.centerX(),y,lineW,healthCode==200&&sessionOk?0xFF80FF80:0xFFFFD050);
  if(!healthFailure.isBlank()||!sessionFailure.isBlank()){
   y+=step;String problem=!sessionFailure.isBlank()?"Сессия: "+sessionFailure:"HTTPS: "+healthFailure;
   PhoneGuiTextures.drawCenteredTrimmedText(context,textRenderer,Text.literal(problem),f.centerX(),y,lineW,0xFFFFD050);
  }
  y+=step;
  PhoneGuiTextures.drawCenteredTrimmedText(context,textRenderer,Text.literal("Fox рядом: "+(foxPeers<0?"—":foxPeers)+" · DND: "+yes(dndSynced)),f.centerX(),y,lineW,dndSynced?0xFF80FF80:0xFFFFD050);
  y+=step;
  PhoneGuiTextures.drawCenteredTrimmedText(context,textRenderer,Text.literal("Автоответчик: "+(voicemailEncrypted?"хранилище зашифровано":"нет подтверждения")),f.centerX(),y,lineW,voicemailEncrypted?0xFF80FF80:0xFFFFD050);
  y+=step;
  long age=VoicemailClient.micFrameAgeMs();String mic=age<0?"ожидает речи через SVC":age<10000?"PCM получен "+(age/1000)+" с назад":"нет свежих PCM; скажите фразу в микрофон";
  PhoneGuiTextures.drawCenteredTrimmedText(context,textRenderer,Text.literal("Микрофон SVC: "+(VoicechatPluginImpl.isReady()?mic:"SVC не подключён")),f.centerX(),y,lineW,VoicechatPluginImpl.isReady()?0xFF80FF80:0xFFFFD050);
  super.render(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
  for(var button:buttons)PhoneGuiTextures.drawButton(context,textRenderer,button,mouseX,mouseY);
 }

 private static String errorText(Throwable error){
  Throwable cause=error;while(cause.getCause()!=null&&cause.getCause()!=cause)cause=cause.getCause();
  String text=cause.getClass().getSimpleName()+(cause.getMessage()==null?"":": "+cause.getMessage());
  return text.replaceAll("[\\r\\n]+"," ").substring(0,Math.min(96,text.length()));
 }

 public void close(){client.setScreen(parent);}
 public boolean shouldPause(){return false;}
}
