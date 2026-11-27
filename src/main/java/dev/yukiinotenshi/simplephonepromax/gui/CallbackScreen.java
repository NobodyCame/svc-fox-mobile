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
 protected void init(){var f=PhoneGuiTextures.dialogLayout(width,height,126);int x=f.x()+12,w=f.width()-24,h=height<160?14:20,top=f.y()+32,footer=f.bottom()-h-5;
  int count=Math.max(1,PhoneGuiTextures.pageSize(top,footer,h,4)), dismissW=Math.min(72,w/3), arrow=Math.min(40,w/4);var rows=CallbackRequests.inbox();page=Math.min(page,Math.max(0,(rows.size()-1)/count));
  for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var r=rows.get(i).getAsJsonObject();String id=r.get("id").getAsString(),name=r.get("name").getAsString();int y=top+(i-page*count)*(h+4);
   addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth("Позвонить: "+name,w-dismissW-14)),b->{CallbackRequests.dismiss(id);minecraft.setScreenAndShow(new CallCheckScreen(UUID.fromString(r.get("source").getAsString()),name));}).bounds(x,y,w-dismissW-4,h).build());
   addRenderableWidget(Button.builder(Component.literal("Убрать"),b->{CallbackRequests.dismiss(id);}).bounds(x+w-dismissW,y,dismissW,h).build());
  }
  addRenderableWidget(Button.builder(Component.literal("←"),b->{page=Math.max(0,page-1);rebuildWidgets();}).bounds(x,footer,arrow,h).build()).active=page>0;
  addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*count<rows.size())page++;rebuildWidgets();}).bounds(x+arrow+4,footer,arrow,h).build()).active=(page+1)*count<rows.size();
  addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x+2*(arrow+4),footer,w-2*(arrow+4),h).build());
 }
 public void tick(){if(++ticks%40==0)rebuildWidgets();}
 public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.dialogLayout(width,height,126);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.centeredText(font,title,f.centerX(),f.y()+12,0xFFFFFFFF);if(CallbackRequests.inbox().isEmpty())c.centeredText(font,Component.literal("Новых просьб нет"),f.centerX(),f.y()+40,0xFFFFFFFF);super.extractRenderState(c,x,y,d);}
 public void onClose(){minecraft.setScreenAndShow(new PhoneMainScreen());}public boolean isPauseScreen(){return false;}
}

