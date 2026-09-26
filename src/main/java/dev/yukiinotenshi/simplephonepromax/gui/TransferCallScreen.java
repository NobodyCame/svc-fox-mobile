package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

public class TransferCallScreen extends Screen {
   private String search="";
   private String listSignature="";
   private int page;
   private int refreshTicks;
   public TransferCallScreen(){super(Text.translatable("phone.transfer.title"));}
   protected void init(){rebuild();}
   private void rebuild(){
      this.clearChildren();
      int w=Math.min(340,this.width-24),x=(this.width-w)/2,top=30;
      var field=new TextFieldWidget(this.textRenderer,x,top,w,20,Text.translatable("phone.search"));
      field.setText(search);field.setPlaceholder(Text.translatable("phone.search.nearby"));
      field.setChangedListener(s->{search=s;page=0;rebuild();});this.addDrawableChild(field);this.setFocused(field);
      var handler=this.client.getNetworkHandler();
      var local=this.client.player;
      var candidates=(handler==null||local==null?java.util.stream.Stream.<net.minecraft.client.network.PlayerListEntry>empty():handler.getPlayerList().stream())
         .filter(p->!p.getProfile().id().equals(local.getUuid()))
         .filter(p->CallControlService.canTransferTo(p.getProfile().id()))
         .sorted(Comparator.comparing(p->p.getProfile().name(),String.CASE_INSENSITIVE_ORDER)).toList();
      listSignature=candidates.stream().map(p->p.getProfile().id().toString()).sorted().reduce("",(a,b)->a+";"+b)+"|"+CallControlService.available();
      var list=candidates.stream().filter(p->(p.getProfile().name()+" "+PhoneNumberManager.getDisplayNumberFor(p.getProfile().id())).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))).toList();
      int count=Math.max(1,(this.height-116)/24);
      page=Math.min(page,Math.max(0,(list.size()-1)/count));
      for(int i=page*count;i<Math.min(list.size(),(page+1)*count);i++){
         var player=list.get(i);String label=player.getProfile().name()+"  "+PhoneNumberManager.formatNumber(PhoneNumberManager.getDisplayNumberFor(player.getProfile().id()));
         this.addDrawableChild(ButtonWidget.builder(Text.literal(this.textRenderer.trimToWidth(label,w-12)),b->CallControlService.transfer(player.getProfile().id()))
            .dimensions(x,58+(i-page*count)*24,w,20).build());
      }
      int bottom=this.height-48;
      var previous=ButtonWidget.builder(Text.literal("←"),b->{page--;rebuild();}).dimensions(x,bottom,40,20).build();previous.active=page>0;this.addDrawableChild(previous);
      var next=ButtonWidget.builder(Text.literal("→"),b->{page++;rebuild();}).dimensions(x+w-40,bottom,40,20).build();next.active=(page+1)*count<list.size();this.addDrawableChild(next);
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.add_call.back_to_call"),b->close()).dimensions(x+44,bottom,w-88,20).build());
   }
   public void tick(){
      if(!SimpleVoiceCallClient.callManager.isInActiveCall()){close();return;}
      if(++refreshTicks%10==0){String current=candidateSignature();if(!current.equals(listSignature))rebuild();}
   }
   private String candidateSignature(){
      var handler=this.client.getNetworkHandler();var local=this.client.player;
      if(handler==null||local==null)return "|"+CallControlService.available();
      return handler.getPlayerList().stream().filter(p->!p.getProfile().id().equals(local.getUuid()))
         .filter(p->CallControlService.canTransferTo(p.getProfile().id()))
         .map(p->p.getProfile().id().toString()).sorted().reduce("",(a,b)->a+";"+b)+"|"+CallControlService.available();
   }
   public void render(DrawContext c,int mouseX,int mouseY,float delta){
      PhoneGuiTextures.fullBackground(c,width,height,mouseX,mouseY);
      c.drawCenteredTextWithShadow(this.textRenderer,this.title,this.width/2,12,0xFFFFFFFF);
      c.drawCenteredTextWithShadow(this.textRenderer,Text.translatable("phone.transfer.free_only"),this.width/2,this.height-18,0xFFAAAAAA);
      if(listSignature.startsWith("|"))c.drawCenteredTextWithShadow(this.textRenderer,Text.literal(CallControlService.available()?"Нет свободных совместимых игроков":"Получаем список игроков…"),this.width/2,this.height/2,0xFFAAAAAA);
      super.render(c,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,c,mouseX,mouseY);
   }
   public void close(){this.client.setScreen(SimpleVoiceCallClient.callManager.isInActiveCall()?new ActiveCallScreen():null);}
   public boolean shouldPause(){return false;}
}

