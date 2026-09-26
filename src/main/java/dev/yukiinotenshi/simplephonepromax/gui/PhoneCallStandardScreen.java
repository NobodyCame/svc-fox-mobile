package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import dev.yukiinotenshi.simplephonepromax.compat.LegacyCallBridge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class PhoneCallStandardScreen extends Screen {
   private final Screen parent;
   public PhoneCallStandardScreen(Screen parent){super(Text.translatable("phone.standard.title"));this.parent=parent;}
   @Override protected void init(){
      int w=Math.min(300,width-24),x=(width-w)/2,y=height/2-30;
      addDrawableChild(ButtonWidget.builder(Text.literal(("yuki".equals(SimpleVoiceCallClient.config.callStandard)?"✓ ":"")+CallStandard.label("yuki")),b->select("yuki")).dimensions(x,y,w,20).build());
      ButtonWidget legacy=ButtonWidget.builder(Text.literal(("legacy".equals(SimpleVoiceCallClient.config.callStandard)?"✓ ":"")+CallStandard.label("legacy")),b->select("legacy")).dimensions(x,y+52,w,20).build();
      legacy.active=CallStandard.originalInstalled()&&CallStandard.canSwitch();addDrawableChild(legacy);
      addDrawableChild(ButtonWidget.builder(Text.translatable("phone.settings.back"),b->client.setScreen(parent)).dimensions(x,Math.min(height-24,y+138),w,20).build());
   }
   private void select(String mode){if(!CallStandard.select(mode)){if(!CallStandard.canSwitch())dev.yukiinotenshi.simplephonepromax.network.CallControlService.message("phone.standard.busy");else LegacyCallBridge.unavailable();}client.setScreen(new PhoneCallStandardScreen(parent));}
   @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
      PhoneGuiTextures.fullBackground(c,width,height,mouseX,mouseY);
      int y=height/2-30,w=Math.min(420,width-24);
      c.drawCenteredTextWithShadow(textRenderer,title,width/2,y-48,0xFFFFFFFF);
      c.drawCenteredTextWithShadow(textRenderer,Text.translatable("phone.standard.active",CallStandard.label(CallStandard.isLegacy()?"legacy":"fox")),width/2,y-30,0xFFB8D6EE);
      c.drawWrappedTextWithShadow(textRenderer,Text.translatable("phone.standard.fox_info"),(width-w)/2,y+24,w,0xFFFFFFFF);
      c.drawWrappedTextWithShadow(textRenderer,Text.translatable("phone.standard.legacy_info"),(width-w)/2,y+76,w,0xFFFFFFFF);
      String status=!CallStandard.originalInstalled()||CallStandard.isLegacy()&&!LegacyCallBridge.ready()?"phone.standard.original_required":!CallStandard.canSwitch()?"phone.standard.busy":"phone.standard.applied_info";
      c.drawWrappedTextWithShadow(textRenderer,Text.translatable(status),(width-w)/2,y+105,w,0xFFFFD07A);
      super.render(c,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,c,mouseX,mouseY);
   }
   @Override public void close(){client.setScreen(parent);}
   @Override public boolean shouldPause(){return false;}
}

