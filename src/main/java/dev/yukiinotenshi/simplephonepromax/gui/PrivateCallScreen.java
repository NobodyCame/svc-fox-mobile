package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.network.*;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class PrivateCallScreen extends Screen {
 private int page,ticks;private String state="";
 public PrivateCallScreen(){super(Component.literal("Приватный звонок"));}
 private String state(){return PrivateCalls.busy()+":"+PrivateCalls.incoming()+":"+PrivateCalls.connected();}
 protected void init(){
  state=state();var f=PhoneGuiTextures.layout(width,height);int x=f.x()+16,w=f.width()-32;
  if(PrivateCalls.busy()){
   int optionsY=f.bottom()-92,primaryY=f.bottom()-66,endY=f.bottom()-40,optionW=(w-4)/2;
   if(PrivateCalls.incoming())addRenderableWidget(Button.builder(Component.literal("Ответить"),b->PrivateCalls.accept()).bounds(x,primaryY,w,20).build());
   if(PrivateCalls.connected())addRenderableWidget(Button.builder(Component.literal(VoicechatPluginImpl.isMicrophoneMuted()?"Включить микрофон":"Выключить микрофон"),b->{VoicechatPluginImpl.setMicrophoneMuted(!VoicechatPluginImpl.isMicrophoneMuted());rebuildWidgets();}).bounds(x,primaryY,w,20).build());
   addRenderableWidget(Button.builder(Component.literal(PrivateCalls.incoming()?"Отклонить":"Завершить"),b->{PrivateCalls.end();rebuildWidgets();}).bounds(x,endY,w,20).build());
   if(PrivateCalls.connected())addRenderableWidget(Button.builder(Component.literal("Громкость"),b->minecraft.setScreenAndShow(new CallVolumesScreen(this))).bounds(x,optionsY,optionW,20).build());
   addRenderableWidget(Button.builder(Component.literal("Настройки"),b->minecraft.setScreenAndShow(new PhoneSettingsScreen())).bounds(x+(PrivateCalls.connected()?optionW+4:0),optionsY,PrivateCalls.connected()?optionW:w,20).build());
  }else{
   int y=f.y()+64;
   var ids=new HashSet<UUID>();var contacts=SimpleVoiceCallClient.config.contacts.stream().filter(c->{try{var id=UUID.fromString(c.uuid);return (c.server==null||c.server.isBlank()||ServerProfiles.matches(c.server))&&CallControlService.wasKnownCompatible(id,"private_call_v1")&&ids.add(id);}catch(Exception e){return false;}}).toList();
   int count=Math.max(1,(f.height()-128)/24);page=Math.min(page,Math.max(0,(contacts.size()-1)/count));
   for(int i=page*count;i<Math.min(contacts.size(),(page+1)*count);i++){var c=contacts.get(i);UUID id=UUID.fromString(c.uuid);boolean online=CallControlService.supports(id,"private_call_v1");var row=Button.builder(Component.literal(font.plainSubstrByWidth(c.name+(online?"":" · офлайн"),w-12)),b->PrivateCalls.call(id)).bounds(x,y+(i-page*count)*24,w,20).build();row.active=online&&!PrivateCalls.busy();addRenderableWidget(row);}
   if(contacts.isEmpty())addRenderableWidget(Button.builder(Component.literal("Нет ранее совместимых контактов"),b->{}).bounds(x,y,w,20).build()).active=false;
   addRenderableWidget(Button.builder(Component.literal("←"),b->{page=Math.max(0,page-1);rebuildWidgets();}).bounds(x,f.bottom()-58,40,20).build());
   addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*count<contacts.size())page++;rebuildWidgets();}).bounds(x+44,f.bottom()-58,40,20).build());
  }
  int footerY=Math.min(height-23,f.bottom()+6);
  addRenderableWidget(Button.builder(Component.literal(PrivateCalls.busy()?"Свернуть":"Назад"),b->onClose()).bounds(x,footerY,w,20).build());
 }
 public void tick(){if(!state.equals(state())||++ticks%40==0)rebuildWidgets();}
 public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){
  dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);
  if(PrivateCalls.busy()){
   PhoneGuiTextures.drawPhone(c,f,x,y);
   int center=f.displayX()+f.displayWidth()/2, max=Math.max(24,f.displayWidth()-f.px(4));
   String peer=PrivateCalls.peerName();
   String headline=PrivateCalls.incoming()?"Входящий приватный звонок":PrivateCalls.connected()?"Приватный разговор":PrivateCalls.outgoingRinging()?"Вызов: "+peer:"Соединение: "+peer;
   int color=PrivateCalls.connected()?0xFF55FFFF:PrivateCalls.incoming()?0xFF55FF55:0xFFFFDD55;
   PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal(headline),center,f.displayY()+(f.displayHeight()-8)/2,max,color);
   PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal("Приватный канал · голос через SVC"),center,f.y()+f.px(49),f.width()-32,0xFFFFFFFF);
   String detail=PrivateCalls.connected()?(VoicechatPluginImpl.isMicrophoneMuted()?"Микрофон выключен":"Микрофон включён"):PrivateCalls.status();
   PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal(detail),center,f.y()+f.px(66),f.width()-32,PrivateCalls.connected()?(VoicechatPluginImpl.isMicrophoneMuted()?0xFFFF7777:0xFF55FF55):0xFFFFFFFF);
  }else{
   c.centeredText(font,title,f.centerX(),f.y()+12,0xFFFFFFFF);
   PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal("Выберите совместимый контакт"),f.centerX(),f.y()+32,f.width()-32,0xFFFFFFFF);
   PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal(PrivateCalls.status()),f.centerX(),f.y()+46,f.width()-32,0xFFFFDD55);
  }
  super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);
 }
 public void onClose(){minecraft.setScreenAndShow(PrivateCalls.busy()?null:new PhoneMainScreen());}
 public boolean isPauseScreen(){return false;}
}

