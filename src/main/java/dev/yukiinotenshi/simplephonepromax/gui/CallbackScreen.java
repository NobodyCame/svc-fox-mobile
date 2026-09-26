package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.network.CallbackRequests;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.UUID;
public final class CallbackScreen extends Screen {
 private int page,ticks;
 public CallbackScreen(){super(Text.literal("Просьбы перезвонить"));}
 protected void init(){var f=PhoneGuiTextures.layout(width,height);int x=f.x()+16,w=f.width()-32,count=Math.max(1,(f.height()-90)/26);var rows=CallbackRequests.inbox();page=Math.min(page,Math.max(0,(rows.size()-1)/count));
  for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var r=rows.get(i).getAsJsonObject();String id=r.get("id").getAsString(),name=r.get("name").getAsString();int y=f.y()+36+(i-page*count)*26;
   addDrawableChild(ButtonWidget.builder(Text.literal(textRenderer.trimToWidth("Позвонить: "+name,w-100)),b->{CallbackRequests.dismiss(id);client.setScreen(new CallCheckScreen(UUID.fromString(r.get("source").getAsString()),name));}).dimensions(x,y,w-76,20).build());
   addDrawableChild(ButtonWidget.builder(Text.literal("Убрать"),b->{CallbackRequests.dismiss(id);}).dimensions(x+w-72,y,72,20).build());
  }
  addDrawableChild(ButtonWidget.builder(Text.literal("←"),b->{page=Math.max(0,page-1);clearAndInit();}).dimensions(x,f.bottom()-50,40,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("→"),b->{if((page+1)*count<rows.size())page++;clearAndInit();}).dimensions(x+44,f.bottom()-50,40,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x+w-100,f.bottom()-50,100,20).build());
 }
 public void tick(){if(++ticks%40==0)clearAndInit();}
 public void render(DrawContext c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.drawCenteredTextWithShadow(textRenderer,title,f.centerX(),f.y()+12,0xFFFFFFFF);if(CallbackRequests.inbox().isEmpty())c.drawCenteredTextWithShadow(textRenderer,Text.literal("Новых просьб нет"),f.centerX(),f.y()+40,0xFFFFFFFF);super.render(c,x,y,d);}
 public void close(){client.setScreen(new PhoneMainScreen());}public boolean shouldPause(){return false;}
}

