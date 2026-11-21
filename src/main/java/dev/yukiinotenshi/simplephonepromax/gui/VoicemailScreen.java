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

 protected void init(){
  var f=PhoneGuiTextures.layout(width,height);int w=f.width()-32,x=f.x()+16,footer=f.bottom()-30;
  if(target!=null){
   int top=f.y()+f.px(70);
   var record=Button.builder(Component.literal(VoicemailClient.recording()?"Остановить и отправить":"Записать сообщение"),b->{if(VoicemailClient.recording())VoicemailClient.send();else VoicemailClient.start(target);rebuildWidgets();}).bounds(x,top,w,20).build();record.active=!VoicemailClient.sending();addRenderableWidget(record);
   addRenderableWidget(Button.builder(Component.literal("Отмена"),b->{VoicemailClient.cancel();onClose();}).bounds(x,top+26,w,20).build());
  }else{
   addRenderableWidget(Button.builder(Component.literal(showingSent?"← Входящие":"Исходящие →"),b->{showingSent=!showingSent;page=0;rebuildWidgets();}).bounds(x,f.y()+43,w,20).build());
   var rows=showingSent?VoicemailClient.sentMessages():VoicemailClient.messages();int top=f.y()+84;
   int count=Math.max(1,(footer-top-30)/26);page=Math.min(page,Math.max(0,(rows.size()-1)/count));
   for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){
    var entry=rows.get(i);int y=top+(i-page*count)*26;
    if(showingSent){
     String recipient=entry.has("recipient_name")&&!entry.get("recipient_name").isJsonNull()?entry.get("recipient_name").getAsString():entry.has("target")?entry.get("target").getAsString():"Контакт";
     addRenderableWidget(Button.builder(Component.literal(recipient+" · "+receipt(entry)),b->{}).bounds(x,y,w,20).build()).active=false;
    }else{
     String sender=entry.get("name").getAsString();boolean listened=entry.has("listened")&&!entry.get("listened").isJsonNull();
     addRenderableWidget(Button.builder(Component.literal("▶ "+sender+(listened?" · прослушано":" · новое")),b->VoicemailClient.play(entry)).bounds(x,y,w-85,20).build());
     addRenderableWidget(Button.builder(Component.literal("Удалить"),b->{VoicemailClient.delete(entry);rebuildWidgets();}).bounds(x+w-80,y,80,20).build());
    }
   }
   addRenderableWidget(Button.builder(Component.literal("←"),b->{if(page>0)page--;rebuildWidgets();}).bounds(x,footer-24,40,20).build());
   addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*count<rows.size())page++;rebuildWidgets();}).bounds(x+44,footer-24,40,20).build());
  }
  addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x,f.bottom()-24,w,20).build());
 }

 private static String receipt(com.google.gson.JsonObject entry){
  if("expired".equals(entry.get("status").getAsString()))return "истекло";
  if(entry.has("listened")&&!entry.get("listened").isJsonNull())return "прослушано";
  if(entry.has("downloaded")&&!entry.get("downloaded").isJsonNull())return "доставлено";
  return "на сервере · ждёт входа";
 }

 public void tick(){super.tick();if(++ticks%20==0)rebuildWidgets();}
 public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){
  dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);
  c.centeredText(font,title,f.centerX(),f.y()+12,0xFFFFFFFF);
  c.centeredText(font,Component.literal(VoicemailClient.status()+(VoicemailClient.recording()?" · "+VoicemailClient.seconds()+" с":"")),f.centerX(),f.y()+32,0xFFFFFFFF);
  if(target!=null){PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal("Микрофон SVC · запись не передаётся в эфир"),f.centerX(),f.y()+f.px(43),f.width()-28,0xFFFFFFFF);PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal("Удерживайте PTT SVC во время записи"),f.centerX(),f.y()+f.px(54),f.width()-28,0xFFBFC6D0);}
  else PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal(showingSent?"Статус меняется после загрузки и прослушивания":"Сообщения хранятся на устройстве"),f.centerX(),f.y()+68,f.width()-28,0xFFFFFFFF);
  super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);
 }
 public void onClose(){if(VoicemailClient.recording())VoicemailClient.cancel();VoicemailClient.stopPlayback();minecraft.setScreenAndShow(new PhoneMainScreen());}
 public void removed(){if(VoicemailClient.recording())VoicemailClient.cancel();VoicemailClient.stopPlayback();}
 public boolean isPauseScreen(){return false;}
}
