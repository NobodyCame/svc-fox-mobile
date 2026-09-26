package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.network.VoicemailClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.UUID;

public final class VoicemailScreen extends Screen {
 private final UUID target;
 private int ticks,page;
 private boolean showingSent;

 public VoicemailScreen(UUID target){super(Text.literal("Голосовые сообщения"));this.target=target;}

 protected void init(){
  var f=PhoneGuiTextures.layout(width,height);int w=f.width()-32,x=f.x()+16,footer=f.bottom()-30;
  if(target!=null){
   int top=f.y()+f.px(70);
   var record=ButtonWidget.builder(Text.literal(VoicemailClient.recording()?"Остановить и отправить":"Записать сообщение"),b->{if(VoicemailClient.recording())VoicemailClient.send();else VoicemailClient.start(target);clearAndInit();}).dimensions(x,top,w,20).build();record.active=!VoicemailClient.sending();addDrawableChild(record);
   addDrawableChild(ButtonWidget.builder(Text.literal("Отмена"),b->{VoicemailClient.cancel();close();}).dimensions(x,top+26,w,20).build());
  }else{
   addDrawableChild(ButtonWidget.builder(Text.literal(showingSent?"← Входящие":"Исходящие →"),b->{showingSent=!showingSent;page=0;clearAndInit();}).dimensions(x,f.y()+43,w,20).build());
   var rows=showingSent?VoicemailClient.sentMessages():VoicemailClient.messages();int top=f.y()+84;
   int count=Math.max(1,(footer-top-30)/26);page=Math.min(page,Math.max(0,(rows.size()-1)/count));
   for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){
    var entry=rows.get(i);int y=top+(i-page*count)*26;
    if(showingSent){
     String recipient=entry.has("recipient_name")&&!entry.get("recipient_name").isJsonNull()?entry.get("recipient_name").getAsString():entry.has("target")?entry.get("target").getAsString():"Контакт";
     addDrawableChild(ButtonWidget.builder(Text.literal(recipient+" · "+receipt(entry)),b->{}).dimensions(x,y,w,20).build()).active=false;
    }else{
     String sender=entry.get("name").getAsString();boolean listened=entry.has("listened")&&!entry.get("listened").isJsonNull();
     addDrawableChild(ButtonWidget.builder(Text.literal("▶ "+sender+(listened?" · прослушано":" · новое")),b->VoicemailClient.play(entry)).dimensions(x,y,w-85,20).build());
     addDrawableChild(ButtonWidget.builder(Text.literal("Удалить"),b->{VoicemailClient.delete(entry);clearAndInit();}).dimensions(x+w-80,y,80,20).build());
    }
   }
   addDrawableChild(ButtonWidget.builder(Text.literal("←"),b->{if(page>0)page--;clearAndInit();}).dimensions(x,footer-24,40,20).build());
   addDrawableChild(ButtonWidget.builder(Text.literal("→"),b->{if((page+1)*count<rows.size())page++;clearAndInit();}).dimensions(x+44,footer-24,40,20).build());
  }
  addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x,f.bottom()-24,w,20).build());
 }

 private static String receipt(com.google.gson.JsonObject entry){
  if("expired".equals(entry.get("status").getAsString()))return "истекло";
  if(entry.has("listened")&&!entry.get("listened").isJsonNull())return "прослушано";
  if(entry.has("downloaded")&&!entry.get("downloaded").isJsonNull())return "доставлено";
  return "на сервере · ждёт входа";
 }

 public void tick(){super.tick();if(++ticks%20==0)clearAndInit();}
 public void render(DrawContext c,int x,int y,float d){
  dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);
  c.drawCenteredTextWithShadow(textRenderer,title,f.centerX(),f.y()+12,0xFFFFFFFF);
  c.drawCenteredTextWithShadow(textRenderer,Text.literal(VoicemailClient.status()+(VoicemailClient.recording()?" · "+VoicemailClient.seconds()+" с":"")),f.centerX(),f.y()+32,0xFFFFFFFF);
  if(target!=null){PhoneGuiTextures.drawCenteredTrimmedText(c,textRenderer,Text.literal("Микрофон SVC · запись не передаётся в эфир"),f.centerX(),f.y()+f.px(43),f.width()-28,0xFFFFFFFF);PhoneGuiTextures.drawCenteredTrimmedText(c,textRenderer,Text.literal("Удерживайте PTT SVC во время записи"),f.centerX(),f.y()+f.px(54),f.width()-28,0xFFBFC6D0);}
  else PhoneGuiTextures.drawCenteredTrimmedText(c,textRenderer,Text.literal(showingSent?"Статус меняется после загрузки и прослушивания":"Сообщения хранятся на устройстве"),f.centerX(),f.y()+68,f.width()-28,0xFFFFFFFF);
  super.render(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);
 }
 public void close(){if(VoicemailClient.recording())VoicemailClient.cancel();VoicemailClient.stopPlayback();client.setScreen(new PhoneMainScreen());}
 public void removed(){if(VoicemailClient.recording())VoicemailClient.cancel();VoicemailClient.stopPlayback();}
 public boolean shouldPause(){return false;}
}
