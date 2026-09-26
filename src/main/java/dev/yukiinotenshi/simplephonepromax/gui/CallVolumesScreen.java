package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.network.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
/** Uses SVC's own per-player volume config, so it also works with original clients. */
public final class CallVolumesScreen extends Screen {
 private final Screen parent;
 public CallVolumesScreen(Screen parent){super(Text.literal("Громкость собеседников"));this.parent=parent;}
 protected void init(){var f=PhoneGuiTextures.layout(width,height);int x=f.x()+16,w=f.width()-32,y=f.y()+34;
  for(var person:CallControlService.groupParticipants()){
   if(client.player!=null&&person.getUuid().equals(client.player.getUuid()))continue;
   var id=person.getUuid();String name=person.getName();double initial=de.maxhenkel.voicechat.VoicechatClient.PLAYER_VOLUME_CONFIG.getVolume(id)/2;
   addDrawableChild(new SliderWidget(x,y,w-110,20,Text.empty(),Math.max(0,Math.min(1,initial))){
    {updateMessage();}protected void updateMessage(){setMessage(Text.literal(name+": "+Math.round(value*200)+"%"));}
    protected void applyValue(){de.maxhenkel.voicechat.VoicechatClient.PLAYER_VOLUME_CONFIG.setVolume(id,value*2,name);}
   });
   addDrawableChild(ButtonWidget.builder(Text.literal("Тихо на 15 мин"),b->{dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.muteFor15Minutes(id);dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages.show("Вызовы от "+name+" отключены на 15 минут");}).dimensions(x+w-106,y,106,20).build());
   y+=25;if(y>f.bottom()-64)break;
  }
  addDrawableChild(ButtonWidget.builder(Text.literal("Готово"),b->close()).dimensions(x,f.bottom()-32,w,20).build());
 }
 public void removed(){de.maxhenkel.voicechat.VoicechatClient.PLAYER_VOLUME_CONFIG.save();}
 public void close(){client.setScreen(parent);}public boolean shouldPause(){return false;}
 public void render(DrawContext c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.drawCenteredTextWithShadow(textRenderer,title,f.centerX(),f.y()+12,0xFFFFFFFF);super.render(c,x,y,d);}
}

