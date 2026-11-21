package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TransferCallScreen extends Screen {
   private String search="";
   private String listSignature="";
   private int page;
   private int refreshTicks;
   public TransferCallScreen(){super(Component.translatable("phone.transfer.title"));}
   protected void init(){rebuild();}
   private void rebuild(){
      this.clearWidgets();
      int w=Math.min(340,this.width-24),x=(this.width-w)/2,top=30;
      var field=new EditBox(this.font,x,top,w,20,Component.translatable("phone.search"));
      field.setValue(search);field.setHint(Component.translatable("phone.search.nearby"));
      field.setResponder(s->{search=s;page=0;rebuild();});this.addRenderableWidget(field);this.setFocused(field);
      var handler=this.minecraft.getConnection();
      var local=this.minecraft.player;
      var candidates=(handler==null||local==null?java.util.stream.Stream.<net.minecraft.client.multiplayer.PlayerInfo>empty():handler.getOnlinePlayers().stream())
         .filter(p->!p.getProfile().id().equals(local.getUUID()))
         .filter(p->CallControlService.canTransferTo(p.getProfile().id()))
         .sorted(Comparator.comparing(p->p.getProfile().name(),String.CASE_INSENSITIVE_ORDER)).toList();
      listSignature=candidates.stream().map(p->p.getProfile().id().toString()).sorted().reduce("",(a,b)->a+";"+b)+"|"+CallControlService.available();
      var list=candidates.stream().filter(p->(p.getProfile().name()+" "+PhoneNumberManager.getDisplayNumberFor(p.getProfile().id())).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))).toList();
      int count=Math.max(1,(this.height-116)/24);
      page=Math.min(page,Math.max(0,(list.size()-1)/count));
      for(int i=page*count;i<Math.min(list.size(),(page+1)*count);i++){
         var player=list.get(i);String label=player.getProfile().name()+"  "+PhoneNumberManager.formatNumber(PhoneNumberManager.getDisplayNumberFor(player.getProfile().id()));
         this.addRenderableWidget(Button.builder(Component.literal(this.font.plainSubstrByWidth(label,w-12)),b->CallControlService.transfer(player.getProfile().id()))
            .bounds(x,58+(i-page*count)*24,w,20).build());
      }
      int bottom=this.height-48;
      var previous=Button.builder(Component.literal("←"),b->{page--;rebuild();}).bounds(x,bottom,40,20).build();previous.active=page>0;this.addRenderableWidget(previous);
      var next=Button.builder(Component.literal("→"),b->{page++;rebuild();}).bounds(x+w-40,bottom,40,20).build();next.active=(page+1)*count<list.size();this.addRenderableWidget(next);
      this.addRenderableWidget(Button.builder(Component.translatable("phone.add_call.back_to_call"),b->onClose()).bounds(x+44,bottom,w-88,20).build());
   }
   public void tick(){
      if(!SimpleVoiceCallClient.callManager.isInActiveCall()){onClose();return;}
      if(++refreshTicks%10==0){String current=candidateSignature();if(!current.equals(listSignature))rebuild();}
   }
   private String candidateSignature(){
      var handler=this.minecraft.getConnection();var local=this.minecraft.player;
      if(handler==null||local==null)return "|"+CallControlService.available();
      return handler.getOnlinePlayers().stream().filter(p->!p.getProfile().id().equals(local.getUUID()))
         .filter(p->CallControlService.canTransferTo(p.getProfile().id()))
         .map(p->p.getProfile().id().toString()).sorted().reduce("",(a,b)->a+";"+b)+"|"+CallControlService.available();
   }
   public void extractRenderState(GuiGraphicsExtractor c,int mouseX,int mouseY,float delta){
      PhoneGuiTextures.fullBackground(c,width,height,mouseX,mouseY);
      c.centeredText(this.font,this.title,this.width/2,12,0xFFFFFFFF);
      c.centeredText(this.font,Component.translatable("phone.transfer.free_only"),this.width/2,this.height-18,0xFFAAAAAA);
      if(listSignature.startsWith("|"))c.centeredText(this.font,Component.literal(CallControlService.available()?"Нет свободных совместимых игроков":"Получаем список игроков…"),this.width/2,this.height/2,0xFFAAAAAA);
      super.extractRenderState(c,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,c,mouseX,mouseY);
   }
   public void onClose(){this.minecraft.setScreenAndShow(SimpleVoiceCallClient.callManager.isInActiveCall()?new ActiveCallScreen():null);}
   public boolean isPauseScreen(){return false;}
}

