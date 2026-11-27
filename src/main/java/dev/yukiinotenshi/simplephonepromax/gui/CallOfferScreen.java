package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A second invitation never replaces the active CallManager state. */
public class CallOfferScreen extends Screen {
   private Button holdButton;private int stableHold,unstableHold;
   public CallOfferScreen() {super(Component.translatable("phone.waiting.title"));}
   private int buttonHeight(){return height<150?15:20;}
   private int top(){return Math.min(height/2,height-3*(buttonHeight()+4));}
   protected void init() {
      holdButton=null;
      var offer=CallControlService.incoming();
      if(offer==null)return;
      int w=Math.min(320,this.width-24), x=(this.width-w)/2, y=top(),h=buttonHeight(),step=h+4;
      String accept=offer.kind().equals("waiting")&&SimpleVoiceCallClient.callManager.isInActiveCall()
         ?"phone.waiting.replace":"phone.incoming.answer";
      this.addRenderableWidget(Button.builder(Component.translatable(accept),b->CallControlService.acceptIncoming())
         .bounds(x,y,w,h).build());
      this.addRenderableWidget(Button.builder(Component.translatable("phone.incoming.decline"),b->CallControlService.declineIncoming())
         .bounds(x,y+step,w,h).build());
      if(offer.kind().equals("waiting")&&SimpleVoiceCallClient.callManager.isInActiveCall()){
      Button hold=Button.builder(Component.translatable("phone.hold.answer"),b->CallControlService.holdAndAnswer()).bounds(x,y+2*step,w,h).build();
      holdButton=hold;hold.active=false;hold.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("phone.hold.compatibility")));this.addRenderableWidget(hold);
      }
   }
   public void tick() {
      if(CallControlService.incoming()==null&&!CallControlService.deciding())this.onClose();
      boolean eligible=CallControlService.canHoldIncoming();stableHold=eligible?stableHold+1:0;unstableHold=eligible?0:unstableHold+1;
      for(var child:this.children())if(child instanceof Button button&&button!=holdButton)button.active=!CallControlService.deciding();
      if(holdButton!=null){if(CallControlService.deciding()||unstableHold>=10)holdButton.active=false;else if(stableHold>=10)holdButton.active=true;}
   }
   public void extractRenderState(GuiGraphicsExtractor context,int mouseX,int mouseY,float delta) {
      PhoneGuiTextures.fullBackground(context,width,height,mouseX,mouseY);
      var offer=CallControlService.incoming();
      if(offer!=null) {
         PhoneGuiTextures.drawCenteredTrimmedText(context,font,Component.translatable(offer.kind().equals("waiting")?"phone.waiting.title":offer.kind().equals("invite")?"phone.group.invite_incoming":"phone.transfer.incoming"),width/2,Math.max(2,top()-34),width-24,0xFFFFDD77);
         PhoneGuiTextures.drawCenteredTrimmedText(context,font,Component.literal(offer.sourceName()),width/2,Math.max(13,top()-18),width-24,0xFFFFFFFF);
      }
      super.extractRenderState(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
   }
   public void onClose() {if(this.minecraft!=null)this.minecraft.setScreenAndShow(null);}
   public boolean isPauseScreen(){return false;}
}

