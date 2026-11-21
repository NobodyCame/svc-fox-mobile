package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.network.CallbackRequests;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class CallbackScreen extends Screen {
 private int page,ticks;
 public CallbackScreen(){super(Component.literal("Просьбы перезвонить"));}
 protected void init(){var f=PhoneGuiTextures.layout(width,height);int x=f.x()+16,w=f.width()-32,count=Math.max(1,(f.height()-90)/26);var rows=CallbackRequests.inbox();page=Math.min(page,Math.max(0,(rows.size()-1)/count));
  for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var r=rows.get(i).getAsJsonObject();String id=r.get("id").getAsString(),name=r.get("name").getAsString();int y=f.y()+36+(i-page*count)*26;
   addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth("Позвонить: "+name,w-100)),b->{CallbackRequests.dismiss(id);minecraft.setScreenAndShow(new CallCheckScreen(UUID.fromString(r.get("source").getAsString()),name));}).bounds(x,y,w-76,20).build());
   addRenderableWidget(Button.builder(Component.literal("Убрать"),b->{CallbackRequests.dismiss(id);}).bounds(x+w-72,y,72,20).build());
  }
  addRenderableWidget(Button.builder(Component.literal("←"),b->{page=Math.max(0,page-1);rebuildWidgets();}).bounds(x,f.bottom()-50,40,20).build());
  addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*count<rows.size())page++;rebuildWidgets();}).bounds(x+44,f.bottom()-50,40,20).build());
  addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x+w-100,f.bottom()-50,100,20).build());
 }
 public void tick(){if(++ticks%40==0)rebuildWidgets();}
 public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.centeredText(font,title,f.centerX(),f.y()+12,0xFFFFFFFF);if(CallbackRequests.inbox().isEmpty())c.centeredText(font,Component.literal("Новых просьб нет"),f.centerX(),f.y()+40,0xFFFFFFFF);super.extractRenderState(c,x,y,d);}
 public void onClose(){minecraft.setScreenAndShow(new PhoneMainScreen());}public boolean isPauseScreen(){return false;}
}

