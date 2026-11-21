package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.network.*;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import dev.yukiinotenshi.simplephonepromax.sound.CallAnnouncement;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class DialAttemptScreen extends Screen {
 private static boolean active;
 private final String number;private UUID target;private String cue,status="Вызов…";private long started;private boolean resolved,eligible,eligibilityDone,announced,mailboxPrompt,closed;
 public static boolean active(){return active;}
 public DialAttemptScreen(String number){super(Component.literal("Вызов"));this.number=number;}
 public DialAttemptScreen(String name,UUID target,String cue){this(name);this.target=target;this.cue=cue;resolved=true;}
 protected void init(){
  var f=PhoneGuiTextures.layout(width,height);addRenderableWidget(Button.builder(Component.literal("Завершить"),b->onClose()).bounds(f.x()+16,f.bottom()-36,f.width()-32,20).build());
  if(started!=0)return;active=true;started=System.currentTimeMillis();if("ended".equals(cue))started-=1800;else CallAnnouncement.play("dialing");
  if(!resolved)PhoneDialer.resolve(number).whenComplete((id,e)->minecraft.execute(()->{if(closed)return;target=id;cue=e!=null?"unavailable":id==null?"invalid_number":null;resolved=true;}));
 }
 public void tick(){
  if(closed)return;
  if(minecraft.player==null||!PhoneDetector.hasPhone(minecraft)){onClose();return;}
  if(!resolved||System.currentTimeMillis()-started<1800)return;
  if(!announced){
   if(OrganizationCalls.queued()){closed=true;active=false;CallAnnouncement.stop();minecraft.setScreenAndShow(new OrganizationScreen());return;}
   if(cue==null){boolean online=minecraft.getConnection()!=null&&minecraft.getConnection().getPlayerInfo(target)!=null;
    if(!online||Boolean.FALSE.equals(CallControlService.hasPhone(target)))cue="offline";
    else {closed=true;active=false;CallAnnouncement.stop();if(PhoneDialer.call(target,number,number))minecraft.setScreenAndShow(new ActiveCallScreen());else minecraft.setScreenAndShow(new PhoneMainScreen());return;}
   }
   announced=true;status=switch(cue){case "invalid_number"->"Номер не найден";case "offline"->"Абонент вне сети";case "ended"->"Разговор завершён";default->"Нет ответа";};CallAnnouncement.play(cue);
   eligible=false;eligibilityDone=target==null||"invalid_number".equals(cue);
   if(!eligibilityDone){UUID checked=target;VoicemailClient.eligible(checked).whenComplete((available,error)->minecraft.execute(()->{if(!closed&&checked.equals(target)){eligible=error==null&&Boolean.TRUE.equals(available);eligibilityDone=true;}}));}
   return;
  }
  if(CallAnnouncement.finished()&&eligibilityDone){
   if(eligible&&!mailboxPrompt){mailboxPrompt=true;status="Можно оставить голосовое";CallAnnouncement.play("voicemail");return;}
   if(eligible){closed=true;active=false;CallAnnouncement.stop();minecraft.setScreenAndShow(new VoicemailScreen(target));}else onClose();
  }
 }
 public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float delta){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,mx,my);c.centeredText(font,Component.literal(number),width/2,height/2-25,0xFFFFFFFF);c.centeredText(font,Component.literal(status),width/2,height/2,0xFFFFFFFF);super.extractRenderState(c,mx,my,delta);PhoneGuiTextures.widgets(this,c,mx,my);}
 public void onClose(){closed=true;active=false;CallAnnouncement.stop();if(minecraft!=null)minecraft.setScreenAndShow(null);}
 public void removed(){CallAnnouncement.stop();active=false;closed=true;}
 public boolean isPauseScreen(){return false;}
}

