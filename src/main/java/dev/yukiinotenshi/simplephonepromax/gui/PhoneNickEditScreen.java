package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;

public class PhoneNickEditScreen extends Screen {
   private final Screen parent;
   private final UUID targetUuid;
   private final String targetNumber;
   private final String realName;
   private String currentCustomName;
   private TextFieldWidget nameField;
   private final List<ButtonWidget> texturedButtons = new ArrayList<>();

   public PhoneNickEditScreen(Screen parent, UUID targetUuid, String targetNumber, String realName, String currentCustomName) {
      super(Text.translatable("phone.contact.edit.title"));
      this.parent = parent;
      this.targetUuid = targetUuid;
      this.targetNumber = targetNumber;
      this.realName = realName != null ? realName : Text.translatable("phone.contact.default_name").getString();
      this.currentCustomName = currentCustomName != null && !currentCustomName.isEmpty() ? currentCustomName : "";
   }

   protected void init() {
      super.init();
      this.texturedButtons.clear();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int cx = frame.centerX();
      int w = frame.px(158);
      int h = Math.max(12, frame.px(14));
      int fieldY = frame.y() + frame.px(75);
      this.nameField = new TextFieldWidget(this.textRenderer, cx - w / 2, fieldY, w, h, Text.translatable("phone.contact.edit.name"));
      this.nameField.setMaxLength(48);
      this.nameField.setText(this.currentCustomName);
      this.nameField.setFocused(true);
      this.setInitialFocus(this.nameField);
      this.addDrawableChild(this.nameField);
      ButtonWidget saveBtn = ButtonWidget.builder(Text.translatable("phone.contact.edit.save"), b -> {
         String typed = this.nameField != null ? this.nameField.getText() : "";
         String newName = typed != null ? typed.trim() : "";
         this.saveCustomName(newName);
         if (this.client != null) {
            this.client.setScreen(new PhoneContactsScreen());
         }
      }).size(frame.px(52), Math.max(12, frame.px(14))).position(frame.x() + frame.px(12), frame.y() + frame.px(105)).build();
      this.addTexturedButton(saveBtn);
      ButtonWidget resetBtn = ButtonWidget.builder(Text.translatable("phone.contact.edit.reset"), b -> {
         this.saveCustomName("");
         if (this.client != null) {
            this.client.setScreen(new PhoneContactsScreen());
         }
      }).size(frame.px(62), Math.max(12, frame.px(14))).position(frame.x() + frame.px(67), frame.y() + frame.px(105)).build();
      this.addTexturedButton(resetBtn);
      ButtonWidget cancelBtn = ButtonWidget.builder(Text.translatable("phone.contact.edit.cancel"), b -> {
         if (this.client != null) {
            this.client.setScreen(new PhoneContactsScreen());
         }
      }).size(frame.px(52), Math.max(12, frame.px(14))).position(frame.x() + frame.px(132), frame.y() + frame.px(105)).build();
      this.addTexturedButton(cancelBtn);
   }

   private void addTexturedButton(ButtonWidget button) {
      this.texturedButtons.add(button);
      this.addDrawableChild(button);
   }

   private void saveCustomName(String newName) {
      String uuidS=this.targetUuid==null?null:this.targetUuid.toString();String digits=PhoneNumberManager.onlyDigits(this.targetNumber);
      for(var c:SimpleVoiceCallClient.config.contacts){
         if(!dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(c.server))continue;
         if(uuidS!=null&&uuidS.equals(c.uuid)||uuidS==null&&digits.equals(PhoneNumberManager.onlyDigits(c.number))){c.nickname=newName;c.name=this.realName;SimpleVoiceCallClient.config.save();return;}
      }
      var c=new ModConfig.Contact(this.realName,digits,uuidS);c.nickname=newName;SimpleVoiceCallClient.config.contacts.add(c);SimpleVoiceCallClient.config.save();
   }

   public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTick) {
      drawContext.fill(0, 0, this.width, this.height, -872415232);
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int cx = frame.centerX();
      PhoneGuiTextures.drawUniversalBackground(drawContext, frame, mouseX, mouseY);
      Text title = Text.translatable("phone.contact.edit.title");
      PhoneGuiTextures.drawCenteredTrimmedText(
         drawContext, this.textRenderer, title, cx, frame.y() + frame.px(8), frame.px(170), 16777096
      );
      Text info1 = Text.translatable("phone.contact.edit.real_name", new Object[]{this.realName});
      PhoneGuiTextures.drawCenteredTrimmedText(
         drawContext, this.textRenderer, info1, cx, frame.y() + frame.px(28), frame.px(170), 16777215
      );
      Text info2 = Text.translatable("phone.contact.edit.number", new Object[]{PhoneNumberManager.formatNumber(this.targetNumber)});
      PhoneGuiTextures.drawCenteredTrimmedText(
         drawContext, this.textRenderer, info2, cx, frame.y() + frame.px(43), frame.px(170), 8978312
      );
      Text labelField = Text.translatable("phone.contact.edit.new_name");
      PhoneGuiTextures.drawCenteredTrimmedText(
         drawContext, this.textRenderer, labelField, cx, frame.y() + frame.px(62), frame.px(170), 13421772
      );
      super.render(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
      for (ButtonWidget button : this.texturedButtons) {
         PhoneGuiTextures.drawButton(drawContext, this.textRenderer, button, mouseX, mouseY);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void close() {
      PhoneClientActions.putAwayPhone();
      super.close();
   }
}



