package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;

public class ActiveCallScreen extends Screen {
   private final List<ButtonWidget> texturedButtons = new ArrayList<>();
   private ButtonWidget transferButton;
   private CallState builtState;
   private boolean builtHeld;
   private ButtonWidget removeParticipantButton;

   public ActiveCallScreen() {
      super(Text.translatable("phone.call.title"));
   }

   private void addTexturedButton(ButtonWidget button) {
      this.texturedButtons.add(button);
      this.addDrawableChild(button);
   }

   private boolean stateForNoise(){return SimpleVoiceCallClient.callManager.isInActiveCall();}
   protected void init() {
      try {
         super.init();
      } catch (Throwable var17) {
      }

      this.texturedButtons.clear();
      this.addTexturedButton(ButtonWidget.builder(Text.literal("Настройки"),b->this.client.setScreen(new PhoneSettingsScreen())).dimensions(Math.max(8,this.width-112),8,104,20).build());
      if(stateForNoise())this.addTexturedButton(ButtonWidget.builder(Text.literal("Громкость игроков"),b->this.client.setScreen(new CallVolumesScreen(this))).dimensions(Math.max(8,this.width-192),56,184,20).build());
      this.transferButton = null;
      this.removeParticipantButton = null;
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      CallState state = SimpleVoiceCallClient.callManager.getState();
      this.builtState = state;
      this.builtHeld = dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasHeld();
      String otherName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
      UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
      String otherNumber = otherUuid != null ? PhoneNumberManager.getDisplayNumberFor(otherUuid) : null;
      if(state==CallState.INCOMING_RINGING&&otherUuid!=null)this.addTexturedButton(ButtonWidget.builder(Text.literal("Не звонить 15 минут"),b->{dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.muteFor15Minutes(otherUuid);SimpleVoiceCallClient.callManager.endCall();this.client.setScreen(new PhoneMainScreen());}).dimensions(frame.x()+16,frame.bottom()-30,frame.width()-32,20).build());
      UUID myUuid = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
      int buttonX = frame.x() + frame.px(110);
      int buttonY = frame.y() + frame.px(31);
      int buttonW = Math.max(50, frame.px(68));
      int buttonH = Math.max(11, frame.px(12));
      int gap = Math.max(2, frame.px(2));
      if (state == CallState.ACTIVE) {
         ButtonWidget muteBtn = ButtonWidget.builder(
               Text.translatable(VoicechatPluginImpl.isMicrophoneMuted() ? "phone.call.unmute" : "phone.call.mute"), b -> {
                  boolean muted = !VoicechatPluginImpl.isMicrophoneMuted();
                  VoicechatPluginImpl.setMicrophoneMuted(muted);
                  if (this.client != null) {
                     this.client.setScreen(new ActiveCallScreen());
                  }
               }
            )
            .size(buttonW, buttonH)
            .position(buttonX, buttonY)
            .build();
         this.addTexturedButton(muteBtn);
         boolean alreadyInContacts = otherNumber != null && SimpleVoiceCallClient.config.hasContact(otherNumber);
         String finalOtherName = otherName;
         String finalOtherNumber = otherNumber;
         UUID finalOtherUuid = otherUuid;
         ButtonWidget contactBtn = ButtonWidget.builder(
               Text.translatable(alreadyInContacts ? "phone.call.already_contact" : "phone.call.add_contact"),
               b -> {
                  if (finalOtherNumber != null) {
                     if (!SimpleVoiceCallClient.config.hasContact(finalOtherNumber)
                        && (finalOtherUuid == null || !SimpleVoiceCallClient.config.hasContact(finalOtherUuid.toString()))) {
                        SimpleVoiceCallClient.config
                           .addContact(
                              new ModConfig.Contact(
                                 finalOtherName != null ? finalOtherName : Text.translatable("phone.contact.default_name").getString(),
                                 finalOtherNumber,
                                 finalOtherUuid != null ? finalOtherUuid.toString() : null
                              )
                           );
                        if (this.client != null) {
                           this.client.setScreen(new ActiveCallScreen());
                        }
                     }
                  }
               }
            )
            .size(buttonW, buttonH)
            .position(buttonX, buttonY + buttonH + gap)
            .build();
         this.addTexturedButton(contactBtn);
         ButtonWidget addCallBtn = ButtonWidget.builder(Text.translatable("phone.call.add_participant"), b -> {
            if (this.client != null) {
               this.client.setScreen(new PhoneMainScreen(true));
            }
         }).size(buttonW, buttonH).position(buttonX, buttonY + 2 * (buttonH + gap)).build();
         this.addTexturedButton(addCallBtn);
         this.removeParticipantButton = ButtonWidget.builder(Text.translatable("phone.call.remove_participant"), b -> {
            if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.canManageGroup()) this.client.setScreen(new GroupParticipantsScreen());
         }).size(buttonW, buttonH).position(buttonX, buttonY + 3 * (buttonH + gap)).build();
         this.removeParticipantButton.active = dev.yukiinotenshi.simplephonepromax.network.CallControlService.canManageGroup();
         this.removeParticipantButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("phone.group.remove_tip")));
         this.addTexturedButton(this.removeParticipantButton);
      }

      if (this.builtHeld) {
         this.addTexturedButton(ButtonWidget.builder(Text.translatable("phone.hold.open"),b->this.client.setScreen(new HeldCallScreen()))
            .dimensions(8,8,Math.min(240,this.width-16),20).build());
      }
      if (state == CallState.ACTIVE) {
         int controlW = Math.min(this.width - 24, Math.max(160, frame.px(150)));
         int controlY = Math.min(this.height - 24, frame.bottom() + 3);
         this.transferButton = ButtonWidget.builder(Text.translatable(
            dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasOutgoingTransfer() ? "phone.transfer.cancel" : "phone.transfer.title"), b -> {
            if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasOutgoingTransfer()) {
               dev.yukiinotenshi.simplephonepromax.network.CallControlService.cancelOutgoing();
               this.client.setScreen(new ActiveCallScreen());
            } else if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.canTransferCurrent()) {
               this.client.setScreen(new TransferCallScreen());
            } else {
               dev.yukiinotenshi.simplephonepromax.network.CallControlService.message("phone.control.incompatible");
            }
         }).dimensions((this.width - controlW) / 2, controlY, controlW, 20).build();
         this.transferButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("phone.control.compatibility_tip")));
         this.addTexturedButton(this.transferButton);
      }

      if (otherUuid != null) {
         boolean blocked = SimpleVoiceCallClient.config.isBlocked(otherUuid);
         int blockY = state == CallState.ACTIVE ? buttonY + 4 * (buttonH + gap) : buttonY;
         ButtonWidget blockBtn = ButtonWidget.builder(Text.translatable(blocked ? "phone.call.unblock" : "phone.call.block"), b -> {
            if (blocked) {
               SimpleVoiceCallClient.config.removeBlocked(otherUuid.toString());
            } else {
               SimpleVoiceCallClient.config.addBlocked(otherUuid, otherName);
               if (state == CallState.INCOMING_RINGING) {
                  ModNetworking.sendCallBlocked(otherUuid);
                  SimpleVoiceCallClient.callManager.declineCall();
               }
            }

            if (this.client != null) {
               this.client.setScreen(new ActiveCallScreen());
            }
         }).size(buttonW, buttonH).position(buttonX, blockY).build();
         this.addTexturedButton(blockBtn);
      }

      int mainButtonsY = state == CallState.ACTIVE ? buttonY + 5 * (buttonH + gap) : buttonY + buttonH + gap;
      if (state == CallState.INCOMING_RINGING) {
         ButtonWidget answerBtn = ButtonWidget.builder(Text.translatable("phone.incoming.answer"), b -> {
            if (myUuid != null && otherUuid != null) {
               ModNetworking.sendCallAccept(myUuid, otherUuid);
            }
         }).size(buttonW, buttonH).position(buttonX, mainButtonsY).build();
         this.addTexturedButton(answerBtn);
         ButtonWidget declineBtn = ButtonWidget.builder(Text.translatable("phone.call.decline"), b -> {
            if (myUuid != null && otherUuid != null) {
               ModNetworking.sendCallDecline(myUuid, otherUuid);
            }

            SimpleVoiceCallClient.callManager.endCall();
            this.close();
         }).size(buttonW, buttonH).position(buttonX, mainButtonsY + buttonH + gap).build();
         this.addTexturedButton(declineBtn);
      } else if (state == CallState.OUTGOING_RINGING) {
         ButtonWidget hangupBtn = ButtonWidget.builder(Text.translatable("phone.call.cancel"), b -> {
            SimpleVoiceCallClient.callManager.endCall();
            if (myUuid != null && otherUuid != null) {
               ModNetworking.sendCallHangup(myUuid, otherUuid);
            }

            this.close();
         }).size(buttonW, buttonH).position(buttonX, mainButtonsY).build();
         this.addTexturedButton(hangupBtn);
      } else {
         ButtonWidget hangupBtn = ButtonWidget.builder(Text.translatable(state == CallState.BUSY ? "phone.settings.back" : "phone.call.hangup"), b -> {
            SimpleVoiceCallClient.callManager.endCall();
            if (state != CallState.BUSY && myUuid != null && otherUuid != null) {
               ModNetworking.sendCallHangup(myUuid, otherUuid);
            }

            this.close();
         }).size(buttonW, buttonH).position(buttonX, mainButtonsY).build();
         this.addTexturedButton(hangupBtn);
      }
   }

   public void tick() {
      if (this.builtState != SimpleVoiceCallClient.callManager.getState() || this.builtHeld != dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasHeld()) { this.clearChildren(); this.init(); }
      if (this.removeParticipantButton != null) this.removeParticipantButton.active = dev.yukiinotenshi.simplephonepromax.network.CallControlService.canManageGroup();
      if (this.transferButton != null) {
         boolean pending = dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasOutgoingTransfer();
         this.transferButton.active = pending || dev.yukiinotenshi.simplephonepromax.network.CallControlService.canTransferCurrent();
         this.transferButton.setMessage(Text.translatable(pending ? "phone.transfer.cancel" : "phone.transfer.title"));
      }
      if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.incoming() != null) {
         this.client.setScreen(new CallOfferScreen());
      }
   }

   public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTick) {
      try {
         CallState state = SimpleVoiceCallClient.callManager.getState();
         String otherName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
         UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
         WallpaperEngine.render(drawContext, this.width, this.height);
         PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
         int displayCenterX = frame.displayX() + frame.displayWidth() / 2;
         int displayTextY = frame.displayY() + (frame.displayHeight() - 8) / 2;
         int infoCenterX = displayCenterX;
         int infoMaxWidth = frame.displayWidth();
         int titleColor = 16777215;
         Text title;
         Text status;
         if (state == CallState.OUTGOING_RINGING) {
            title = Text.translatable("phone.call.calling_player", new Object[]{otherName != null ? otherName : ""});
            status = Text.translatable("phone.call.ringing");
            titleColor = 16777130;
         } else if (state == CallState.INCOMING_RINGING) {
            title = Text.translatable("phone.call.incoming_player", new Object[]{otherName != null ? otherName : ""});
            status = Text.translatable("phone.call.incoming_status");
            titleColor = 5635925;
         } else if (state == CallState.ACTIVE) {
            title = Text.translatable("phone.call.active_player", new Object[]{otherName != null ? otherName : ""});
            status = Text.translatable("phone.call.connected");
            titleColor = 5636095;
         } else if (state == CallState.BUSY) {
            title = Text.translatable("phone.call.busy");
            status = Text.translatable("phone.call.busy_later");
            titleColor = 0xFFFF5555;
         } else {
            title = Text.translatable("phone.call.title");
            status = Text.empty();
         }

         try {
            PhoneGuiTextures.drawPhone(drawContext, frame, mouseX, mouseY);
         } catch (Throwable var19) {
         }

         try {
            PhoneGuiTextures.drawCenteredTrimmedText(
               drawContext, this.textRenderer, title, displayCenterX, displayTextY, Math.max(24, frame.displayWidth() - frame.px(4)), titleColor
            );
         } catch (Throwable var21) {
         }

         if (otherUuid != null) {
            String num = PhoneNumberManager.getDisplayNumberFor(otherUuid);
            Text numL = Text.literal(PhoneNumberManager.formatNumber(num));

            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.textRenderer, numL, infoCenterX, frame.y() + frame.px(49), infoMaxWidth, 16777215
               );
            } catch (Throwable var18) {
            }
         }

         if (state == CallState.ACTIVE) {
            boolean muted = VoicechatPluginImpl.isMicrophoneMuted();
            Text mt = Text.translatable(muted ? "phone.call.muted" : "phone.call.microphone_active");
            int mtc = muted ? 16737894 : 6750054;

            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.textRenderer, mt, infoCenterX, frame.y() + frame.px(66), infoMaxWidth, mtc
               );
            } catch (Throwable var17) {
            }
         }

         if (status.getString() != null && !status.getString().isEmpty()) {
            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.textRenderer, status, infoCenterX, frame.y() + frame.px(84), infoMaxWidth, 16777215
               );
            } catch (Throwable var16) {
            }
         }

         super.render(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
         try {
            for (ButtonWidget button : this.texturedButtons) {
               PhoneGuiTextures.drawButton(drawContext, this.textRenderer, button, mouseX, mouseY);
            }
         } catch (Throwable ignored) {
         }
      } catch (Throwable var20) {
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      PhoneClientActions.putAwayPhone();
      super.close();
   }
}



