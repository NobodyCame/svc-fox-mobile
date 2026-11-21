package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import dev.yukiinotenshi.simplephonepromax.compat.LegacyCallBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PhoneCallStandardScreen extends Screen {
   private final Screen parent;
   public PhoneCallStandardScreen(Screen parent){super(Component.translatable("phone.standard.title"));this.parent=parent;}
   @Override protected void init(){
      int w=Math.min(300,width-24),x=(width-w)/2,y=height/2-30;
      addRenderableWidget(Button.builder(Component.literal(("yuki".equals(SimpleVoiceCallClient.config.callStandard)?"✓ ":"")+CallStandard.label("yuki")),b->select("yuki")).bounds(x,y,w,20).build());
      Button legacy=Button.builder(Component.literal(("legacy".equals(SimpleVoiceCallClient.config.callStandard)?"✓ ":"")+CallStandard.label("legacy")),b->select("legacy")).bounds(x,y+52,w,20).build();
      legacy.active=CallStandard.originalInstalled()&&CallStandard.canSwitch();addRenderableWidget(legacy);
      addRenderableWidget(Button.builder(Component.translatable("phone.settings.back"),b->minecraft.setScreenAndShow(parent)).bounds(x,Math.min(height-24,y+138),w,20).build());
   }
   private void select(String mode){if(!CallStandard.select(mode)){if(!CallStandard.canSwitch())dev.yukiinotenshi.simplephonepromax.network.CallControlService.message("phone.standard.busy");else LegacyCallBridge.unavailable();}minecraft.setScreenAndShow(new PhoneCallStandardScreen(parent));}
   @Override public void extractRenderState(GuiGraphicsExtractor c,int mouseX,int mouseY,float delta){
      PhoneGuiTextures.fullBackground(c,width,height,mouseX,mouseY);
      int y=height/2-30,w=Math.min(420,width-24);
      c.centeredText(font,title,width/2,y-48,0xFFFFFFFF);
      c.centeredText(font,Component.translatable("phone.standard.active",CallStandard.label(CallStandard.isLegacy()?"legacy":"fox")),width/2,y-30,0xFFB8D6EE);
      c.textWithWordWrap(font,Component.translatable("phone.standard.fox_info"),(width-w)/2,y+24,w,0xFFFFFFFF);
      c.textWithWordWrap(font,Component.translatable("phone.standard.legacy_info"),(width-w)/2,y+76,w,0xFFFFFFFF);
      String status=!CallStandard.originalInstalled()||CallStandard.isLegacy()&&!LegacyCallBridge.ready()?"phone.standard.original_required":!CallStandard.canSwitch()?"phone.standard.busy":"phone.standard.applied_info";
      c.textWithWordWrap(font,Component.translatable(status),(width-w)/2,y+105,w,0xFFFFD07A);
      super.extractRenderState(c,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,c,mouseX,mouseY);
   }
   @Override public void onClose(){minecraft.setScreenAndShow(parent);}
   @Override public boolean isPauseScreen(){return false;}
}

