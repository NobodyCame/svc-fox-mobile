package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GroupParticipantsScreen extends Screen {
   private final Map<Button,UUID> removeButtons=new HashMap<>();
   private int page;
   public GroupParticipantsScreen(){super(Component.translatable("phone.group.members"));}
   protected void init(){rebuild();}
   private void rebuild(){
      this.clearWidgets();removeButtons.clear();
      int w=Math.min(550,this.width-24),x=(this.width-w)/2;
      var rows=CallControlService.groupParticipants().stream().filter(p->!p.getUuid().equals(this.minecraft.player.getUUID()))
         .sorted(Comparator.comparing(p->p.getName(),String.CASE_INSENSITIVE_ORDER)).toList();
      int count=Math.max(1,(this.height-108)/24);page=Math.min(page,Math.max(0,(rows.size()-1)/count));
      for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){
         var row=rows.get(i);
         var button=Button.builder(Component.literal(this.font.plainSubstrByWidth(row.getName(),w-12)),b->CallControlService.removeParticipant(row.getUuid()))
            .bounds(x,48+(i-page*count)*24,w/2,20).build();
         button.active=CallControlService.canRemove(row.getUuid());
         button.setTooltip(Tooltip.create(Component.translatable("phone.group.remove_tip")));
         removeButtons.put(button,row.getUuid());this.addRenderableWidget(button);
         var owner=Button.builder(Component.literal("Передать"),b->CallControlService.offerOwnership(row.getUuid(),false)).bounds(x+w/2+2,48+(i-page*count)*24,w/4-3,20).build();owner.active=CallControlService.canHandover();this.addRenderableWidget(owner);
         var deputy=Button.builder(Component.literal("Заместитель"),b->CallControlService.offerOwnership(row.getUuid(),true)).bounds(x+w*3/4+1,48+(i-page*count)*24,w/4-1,20).build();deputy.active=CallControlService.canHandover();this.addRenderableWidget(deputy);
      }
      int bottom=this.height-48;
      var previous=Button.builder(Component.literal("←"),b->{page--;rebuild();}).bounds(x,bottom,40,20).build();previous.active=page>0;this.addRenderableWidget(previous);
      var next=Button.builder(Component.literal("→"),b->{page++;rebuild();}).bounds(x+w-40,bottom,40,20).build();next.active=(page+1)*count<rows.size();this.addRenderableWidget(next);
      this.addRenderableWidget(Button.builder(Component.translatable("phone.add_call.back_to_call"),b->onClose()).bounds(x+44,bottom,w-88,20).build());
   }
   public void tick(){
      if(!SimpleVoiceCallClient.callManager.isInActiveCall()){onClose();return;}
      removeButtons.forEach((button,id)->button.active=CallControlService.canRemove(id));
   }
   public void extractRenderState(GuiGraphicsExtractor c,int mouseX,int mouseY,float delta){
      PhoneGuiTextures.fullBackground(c,width,height,mouseX,mouseY);
      c.centeredText(this.font,this.title,this.width/2,12,0xFFFFFFFF);
      c.centeredText(this.font,Component.translatable("phone.group.choose_remove"),this.width/2,28,0xFFFFDD77);
      super.extractRenderState(c,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,c,mouseX,mouseY);
   }
   public void onClose(){this.minecraft.setScreenAndShow(SimpleVoiceCallClient.callManager.isInActiveCall()?new ActiveCallScreen():null);}
   public boolean isPauseScreen(){return false;}
}

