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
   protected void init() {
      var offer=CallControlService.incoming();
      if(offer==null)return;
      int w=Math.min(320,this.width-24), x=(this.width-w)/2, y=this.height/2;
      String accept=offer.kind().equals("waiting")&&SimpleVoiceCallClient.callManager.isInActiveCall()
         ?"phone.waiting.replace":"phone.incoming.answer";
      this.addRenderableWidget(Button.builder(Component.translatable(accept),b->CallControlService.acceptIncoming())
         .bounds(x,y,w,20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("phone.incoming.decline"),b->CallControlService.declineIncoming())
         .bounds(x,y+24,w,20).build());
      if(offer.kind().equals("waiting")&&SimpleVoiceCallClient.callManager.isInActiveCall()){
      Button hold=Button.builder(Component.translatable("phone.hold.answer"),b->CallControlService.holdAndAnswer()).bounds(x,y+48,w,20).build();
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
         context.centeredText(this.font,Component.translatable(offer.kind().equals("waiting")?"phone.waiting.title":offer.kind().equals("invite")?"phone.group.invite_incoming":"phone.transfer.incoming"),this.width/2,this.height/2-44,0xFFFFDD77);
         context.centeredText(this.font,this.font.plainSubstrByWidth(offer.sourceName(),this.width-32),this.width/2,this.height/2-27,0xFFFFFFFF);
      }
      super.extractRenderState(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
   }
   public void onClose() {if(this.minecraft!=null)this.minecraft.setScreenAndShow(null);}
   public boolean isPauseScreen(){return false;}
}

