package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A second invitation never replaces the active CallManager state. */
public class CallOfferScreen extends Screen {
   private ButtonWidget holdButton;private int stableHold,unstableHold;
   public CallOfferScreen() {super(Text.translatable("phone.waiting.title"));}
   protected void init() {
      var offer=CallControlService.incoming();
      if(offer==null)return;
      int w=Math.min(320,this.width-24), x=(this.width-w)/2, y=this.height/2;
      String accept=offer.kind().equals("waiting")&&SimpleVoiceCallClient.callManager.isInActiveCall()
         ?"phone.waiting.replace":"phone.incoming.answer";
      this.addDrawableChild(ButtonWidget.builder(Text.translatable(accept),b->CallControlService.acceptIncoming())
         .dimensions(x,y,w,20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.incoming.decline"),b->CallControlService.declineIncoming())
         .dimensions(x,y+24,w,20).build());
      if(offer.kind().equals("waiting")&&SimpleVoiceCallClient.callManager.isInActiveCall()){
      ButtonWidget hold=ButtonWidget.builder(Text.translatable("phone.hold.answer"),b->CallControlService.holdAndAnswer()).dimensions(x,y+48,w,20).build();
      holdButton=hold;hold.active=false;hold.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("phone.hold.compatibility")));this.addDrawableChild(hold);
      }
   }
   public void tick() {
      if(CallControlService.incoming()==null&&!CallControlService.deciding())this.close();
      boolean eligible=CallControlService.canHoldIncoming();stableHold=eligible?stableHold+1:0;unstableHold=eligible?0:unstableHold+1;
      for(var child:this.children())if(child instanceof ButtonWidget button&&button!=holdButton)button.active=!CallControlService.deciding();
      if(holdButton!=null){if(CallControlService.deciding()||unstableHold>=10)holdButton.active=false;else if(stableHold>=10)holdButton.active=true;}
   }
   public void render(DrawContext context,int mouseX,int mouseY,float delta) {
      PhoneGuiTextures.fullBackground(context,width,height,mouseX,mouseY);
      var offer=CallControlService.incoming();
      if(offer!=null) {
         context.drawCenteredTextWithShadow(this.textRenderer,Text.translatable(offer.kind().equals("waiting")?"phone.waiting.title":offer.kind().equals("invite")?"phone.group.invite_incoming":"phone.transfer.incoming"),this.width/2,this.height/2-44,0xFFFFDD77);
         context.drawCenteredTextWithShadow(this.textRenderer,this.textRenderer.trimToWidth(offer.sourceName(),this.width-32),this.width/2,this.height/2-27,0xFFFFFFFF);
      }
      super.render(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
   }
   public void close() {if(this.client!=null)this.client.setScreen(null);}
   public boolean shouldPause(){return false;}
}

