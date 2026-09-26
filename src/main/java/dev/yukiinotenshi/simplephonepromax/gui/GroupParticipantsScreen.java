package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import java.util.*;

public class GroupParticipantsScreen extends Screen {
   private final Map<ButtonWidget,UUID> removeButtons=new HashMap<>();
   private int page;
   public GroupParticipantsScreen(){super(Text.translatable("phone.group.members"));}
   protected void init(){rebuild();}
   private void rebuild(){
      this.clearChildren();removeButtons.clear();
      int w=Math.min(550,this.width-24),x=(this.width-w)/2;
      var rows=CallControlService.groupParticipants().stream().filter(p->!p.getUuid().equals(this.client.player.getUuid()))
         .sorted(Comparator.comparing(p->p.getName(),String.CASE_INSENSITIVE_ORDER)).toList();
      int count=Math.max(1,(this.height-108)/24);page=Math.min(page,Math.max(0,(rows.size()-1)/count));
      for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){
         var row=rows.get(i);
         var button=ButtonWidget.builder(Text.literal(this.textRenderer.trimToWidth(row.getName(),w-12)),b->CallControlService.removeParticipant(row.getUuid()))
            .dimensions(x,48+(i-page*count)*24,w/2,20).build();
         button.active=CallControlService.canRemove(row.getUuid());
         button.setTooltip(Tooltip.of(Text.translatable("phone.group.remove_tip")));
         removeButtons.put(button,row.getUuid());this.addDrawableChild(button);
         var owner=ButtonWidget.builder(Text.literal("Передать"),b->CallControlService.offerOwnership(row.getUuid(),false)).dimensions(x+w/2+2,48+(i-page*count)*24,w/4-3,20).build();owner.active=CallControlService.canHandover();this.addDrawableChild(owner);
         var deputy=ButtonWidget.builder(Text.literal("Заместитель"),b->CallControlService.offerOwnership(row.getUuid(),true)).dimensions(x+w*3/4+1,48+(i-page*count)*24,w/4-1,20).build();deputy.active=CallControlService.canHandover();this.addDrawableChild(deputy);
      }
      int bottom=this.height-48;
      var previous=ButtonWidget.builder(Text.literal("←"),b->{page--;rebuild();}).dimensions(x,bottom,40,20).build();previous.active=page>0;this.addDrawableChild(previous);
      var next=ButtonWidget.builder(Text.literal("→"),b->{page++;rebuild();}).dimensions(x+w-40,bottom,40,20).build();next.active=(page+1)*count<rows.size();this.addDrawableChild(next);
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.add_call.back_to_call"),b->close()).dimensions(x+44,bottom,w-88,20).build());
   }
   public void tick(){
      if(!SimpleVoiceCallClient.callManager.isInActiveCall()){close();return;}
      removeButtons.forEach((button,id)->button.active=CallControlService.canRemove(id));
   }
   public void render(DrawContext c,int mouseX,int mouseY,float delta){
      PhoneGuiTextures.fullBackground(c,width,height,mouseX,mouseY);
      c.drawCenteredTextWithShadow(this.textRenderer,this.title,this.width/2,12,0xFFFFFFFF);
      c.drawCenteredTextWithShadow(this.textRenderer,Text.translatable("phone.group.choose_remove"),this.width/2,28,0xFFFFDD77);
      super.render(c,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,c,mouseX,mouseY);
   }
   public void close(){this.client.setScreen(SimpleVoiceCallClient.callManager.isInActiveCall()?new ActiveCallScreen():null);}
   public boolean shouldPause(){return false;}
}

