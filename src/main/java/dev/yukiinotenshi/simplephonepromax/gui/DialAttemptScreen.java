package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.network.*;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import dev.yukiinotenshi.simplephonepromax.sound.CallAnnouncement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.UUID;
public final class DialAttemptScreen extends Screen {
 private static boolean active;
 private final String number;private UUID target;private String cue,status="Вызов…";private long started;private boolean resolved,eligible,eligibilityDone,announced,mailboxPrompt,closed;
 public static boolean active(){return active;}
 public DialAttemptScreen(String number){super(Text.literal("Вызов"));this.number=number;}
 public DialAttemptScreen(String name,UUID target,String cue){this(name);this.target=target;this.cue=cue;resolved=true;}
 protected void init(){
  var f=PhoneGuiTextures.layout(width,height);addDrawableChild(ButtonWidget.builder(Text.literal("Завершить"),b->close()).dimensions(f.x()+16,f.bottom()-36,f.width()-32,20).build());
  if(started!=0)return;active=true;started=System.currentTimeMillis();if("ended".equals(cue))started-=1800;else CallAnnouncement.play("dialing");
  if(!resolved)PhoneDialer.resolve(number).whenComplete((id,e)->client.execute(()->{if(closed)return;target=id;cue=e!=null?"unavailable":id==null?"invalid_number":null;resolved=true;}));
 }
 public void tick(){
  if(closed)return;
  if(client.player==null||!PhoneDetector.hasPhone(client)){close();return;}
  if(!resolved||System.currentTimeMillis()-started<1800)return;
  if(!announced){
   if(OrganizationCalls.queued()){closed=true;active=false;CallAnnouncement.stop();client.setScreen(new OrganizationScreen());return;}
   if(cue==null){boolean online=client.getNetworkHandler()!=null&&client.getNetworkHandler().getPlayerListEntry(target)!=null;
    if(!online||Boolean.FALSE.equals(CallControlService.hasPhone(target)))cue="offline";
    else {closed=true;active=false;CallAnnouncement.stop();if(PhoneDialer.call(target,number,number))client.setScreen(new ActiveCallScreen());else client.setScreen(new PhoneMainScreen());return;}
   }
   announced=true;status=switch(cue){case "invalid_number"->"Номер не найден";case "offline"->"Абонент вне сети";case "ended"->"Разговор завершён";default->"Нет ответа";};CallAnnouncement.play(cue);
   eligible=false;eligibilityDone=target==null||"invalid_number".equals(cue);
   if(!eligibilityDone){UUID checked=target;VoicemailClient.eligible(checked).whenComplete((available,error)->client.execute(()->{if(!closed&&checked.equals(target)){eligible=error==null&&Boolean.TRUE.equals(available);eligibilityDone=true;}}));}
   return;
  }
  if(CallAnnouncement.finished()&&eligibilityDone){
   if(eligible&&!mailboxPrompt){mailboxPrompt=true;status="Можно оставить голосовое";CallAnnouncement.play("voicemail");return;}
   if(eligible){closed=true;active=false;CallAnnouncement.stop();client.setScreen(new VoicemailScreen(target));}else close();
  }
 }
 public void render(DrawContext c,int mx,int my,float delta){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,mx,my);c.drawCenteredTextWithShadow(textRenderer,Text.literal(number),width/2,height/2-25,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,Text.literal(status),width/2,height/2,0xFFFFFFFF);super.render(c,mx,my,delta);PhoneGuiTextures.widgets(this,c,mx,my);}
 public void close(){closed=true;active=false;CallAnnouncement.stop();if(client!=null)client.setScreen(null);}
 public void removed(){CallAnnouncement.stop();active=false;closed=true;}
 public boolean shouldPause(){return false;}
}

