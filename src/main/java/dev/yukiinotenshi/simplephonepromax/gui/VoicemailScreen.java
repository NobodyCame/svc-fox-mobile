package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.network.VoicemailClient;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VoicemailScreen extends Screen {
 private final UUID target;
 private int ticks,page;
 private boolean showingSent;

 public VoicemailScreen(UUID target){super(Component.literal("Голосовые сообщения"));this.target=target;}
 private PhoneGuiTextures.Frame frame(){return PhoneGuiTextures.dialogLayout(width,height,142);}
 private int rowHeight(){return height<160?14:20;}

 protected void init(){
  var f=frame();
  int h=rowHeight(),w=f.width()-24,x=f.x()+12,footer=f.bottom()-h-5;
  if(target!=null){
   int top=footer-2*(h+4);
   var record=Button.builder(Component.literal(VoicemailClient.recording()?"Остановить и отправить":"Записать сообщение"),b->{if(VoicemailClient.recording())VoicemailClient.send();else VoicemailClient.start(target);rebuildWidgets();}).bounds(x,top,w,h).build();record.active=!VoicemailClient.sending();addRenderableWidget(record);
   addRenderableWidget(Button.builder(Component.literal("Отмена"),b->{VoicemailClient.cancel();onClose();}).bounds(x,top+h+4,w,h).build());
  }else{
   addRenderableWidget(Button.builder(Component.literal(showingSent?"← Входящие":"Исходящие →"),b->{showingSent=!showingSent;page=0;rebuildWidgets();}).bounds(x,f.y()+30,w,h).build());
   var rows=showingSent?VoicemailClient.sentMessages():VoicemailClient.messages();
   int top=f.y()+h+44,gap=3,deleteW=Math.min(80,w/3);
   int count=Math.max(1,PhoneGuiTextures.pageSize(top,footer,h,gap));page=Math.min(page,Math.max(0,(rows.size()-1)/count));
   for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){
    var entry=rows.get(i);int y=top+(i-page*count)*(h+gap);
    if(showingSent){
     String recipient=entry.has("recipient_name")&&!entry.get("recipient_name").isJsonNull()?entry.get("recipient_name").getAsString():entry.has("target")?entry.get("target").getAsString():"Контакт";
     addRenderableWidget(Button.builder(Component.literal(recipient+" · "+receipt(entry)),b->{}).bounds(x,y,w,h).build()).active=false;
    }else{
     String sender=entry.get("name").getAsString();boolean listened=entry.has("listened")&&!entry.get("listened").isJsonNull();
     addRenderableWidget(Button.builder(Component.literal("▶ "+sender+(listened?" · прослушано":" · новое")),b->VoicemailClient.play(entry)).bounds(x,y,w-deleteW-4,h).build());
     addRenderableWidget(Button.builder(Component.literal("Удалить"),b->{VoicemailClient.delete(entry);rebuildWidgets();}).bounds(x+w-deleteW,y,deleteW,h).build());
    }
   }
   int arrow=Math.min(40,w/4);
   addRenderableWidget(Button.builder(Component.literal("←"),b->{page--;rebuildWidgets();}).bounds(x,footer,arrow,h).build()).active=page>0;
   addRenderableWidget(Button.builder(Component.literal("→"),b->{page++;rebuildWidgets();}).bounds(x+arrow+4,footer,arrow,h).build()).active=(page+1)*count<rows.size();
   x+=2*(arrow+4);w-=2*(arrow+4);
  }
  addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x,footer,w,h).build());
 }

 private static String receipt(com.google.gson.JsonObject entry){
  if("expired".equals(entry.get("status").getAsString()))return "истекло";
  if(entry.has("listened")&&!entry.get("listened").isJsonNull())return "прослушано";
  if(entry.has("downloaded")&&!entry.get("downloaded").isJsonNull())return "доставлено";
  return "на сервере · ждёт входа";
 }

 public void tick(){super.tick();if(++ticks%20==0)rebuildWidgets();}
 public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){
  dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=frame();PhoneGuiTextures.drawUniversalBackground(c,f,x,y);
  PhoneGuiTextures.drawCenteredTrimmedText(c,font,title,f.centerX(),f.y()+6,f.width()-24,0xFFFFFFFF);
  PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal(VoicemailClient.status()+(VoicemailClient.recording()?" · "+VoicemailClient.seconds()+" с":"")),f.centerX(),f.y()+19,f.width()-24,0xFFFFFFFF);
  if(target!=null){PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal("Микрофон SVC · запись не передаётся в эфир"),f.centerX(),f.y()+32,f.width()-24,0xFFFFFFFF);PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal("Удерживайте PTT SVC во время записи"),f.centerX(),f.y()+43,f.width()-24,0xFFBFC6D0);}
  else PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal(showingSent?"Статус меняется после загрузки и прослушивания":"Сообщения хранятся на устройстве"),f.centerX(),f.y()+rowHeight()+32,f.width()-24,0xFFFFFFFF);
  super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);
 }
 public void onClose(){if(VoicemailClient.recording())VoicemailClient.cancel();VoicemailClient.stopPlayback();minecraft.setScreenAndShow(new PhoneMainScreen());}
 public void removed(){if(VoicemailClient.recording())VoicemailClient.cancel();VoicemailClient.stopPlayback();}
 public boolean isPauseScreen(){return false;}
}
