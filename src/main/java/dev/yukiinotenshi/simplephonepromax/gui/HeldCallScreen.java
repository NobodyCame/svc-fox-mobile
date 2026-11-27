package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class HeldCallScreen extends Screen {
   public HeldCallScreen(){super(Component.translatable("phone.hold.open"));}
   private int buttonHeight(){return height<150?15:20;}
   private int top(){return Math.min(height/2,height-3*(buttonHeight()+4));}
   protected void init(){int w=Math.min(330,width-24),x=(width-w)/2,y=top(),h=buttonHeight(),step=h+4;
      addRenderableWidget(Button.builder(Component.translatable("phone.hold.resume"),b->{CallControlService.resumeHeld();minecraft.setScreenAndShow(new ActiveCallScreen());}).bounds(x,y,w,h).build());
      addRenderableWidget(Button.builder(Component.translatable("phone.hold.release"),b->{CallControlService.releaseHeld();minecraft.setScreenAndShow(new PhoneMainScreen());}).bounds(x,y+step,w,h).build());
      addRenderableWidget(Button.builder(Component.translatable("phone.settings.back"),b->onClose()).bounds(x,y+2*step,w,h).build());
   }
   public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){PhoneGuiTextures.fullBackground(c,width,height,x,y);PhoneGuiTextures.drawCenteredTrimmedText(c,font,title,width/2,Math.max(2,top()-34),width-24,-1);PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.translatable("phone.hold.limit"),width/2,Math.max(13,top()-18),width-24,0xFFFFDD77);super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
   public void tick(){if(!CallControlService.hasHeld())onClose();}
   public boolean isPauseScreen(){return false;}
}

