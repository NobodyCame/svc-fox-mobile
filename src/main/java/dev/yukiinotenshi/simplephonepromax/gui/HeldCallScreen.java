package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public final class HeldCallScreen extends Screen {
   public HeldCallScreen(){super(Text.translatable("phone.hold.open"));}
   protected void init(){int w=Math.min(330,width-24),x=(width-w)/2,y=height/2;
      addDrawableChild(ButtonWidget.builder(Text.translatable("phone.hold.resume"),b->{CallControlService.resumeHeld();client.setScreen(new ActiveCallScreen());}).dimensions(x,y,w,20).build());
      addDrawableChild(ButtonWidget.builder(Text.translatable("phone.hold.release"),b->{CallControlService.releaseHeld();client.setScreen(new PhoneMainScreen());}).dimensions(x,y+24,w,20).build());
      addDrawableChild(ButtonWidget.builder(Text.translatable("phone.settings.back"),b->close()).dimensions(x,y+48,w,20).build());
   }
   public void render(DrawContext c,int x,int y,float d){PhoneGuiTextures.fullBackground(c,width,height,x,y);c.drawCenteredTextWithShadow(textRenderer,title,width/2,height/2-40,-1);c.drawCenteredTextWithShadow(textRenderer,Text.translatable("phone.hold.limit"),width/2,height/2-23,0xFFFFDD77);super.render(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
   public void tick(){if(!CallControlService.hasHeld())close();}
   public boolean shouldPause(){return false;}
}

