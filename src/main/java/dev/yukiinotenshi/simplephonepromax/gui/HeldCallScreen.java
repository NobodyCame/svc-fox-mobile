package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class HeldCallScreen extends Screen {
   public HeldCallScreen(){super(Component.translatable("phone.hold.open"));}
   protected void init(){int w=Math.min(330,width-24),x=(width-w)/2,y=height/2;
      addRenderableWidget(Button.builder(Component.translatable("phone.hold.resume"),b->{CallControlService.resumeHeld();minecraft.setScreenAndShow(new ActiveCallScreen());}).bounds(x,y,w,20).build());
      addRenderableWidget(Button.builder(Component.translatable("phone.hold.release"),b->{CallControlService.releaseHeld();minecraft.setScreenAndShow(new PhoneMainScreen());}).bounds(x,y+24,w,20).build());
      addRenderableWidget(Button.builder(Component.translatable("phone.settings.back"),b->onClose()).bounds(x,y+48,w,20).build());
   }
   public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){PhoneGuiTextures.fullBackground(c,width,height,x,y);c.centeredText(font,title,width/2,height/2-40,-1);c.centeredText(font,Component.translatable("phone.hold.limit"),width/2,height/2-23,0xFFFFDD77);super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
   public void tick(){if(!CallControlService.hasHeld())onClose();}
   public boolean isPauseScreen(){return false;}
}

