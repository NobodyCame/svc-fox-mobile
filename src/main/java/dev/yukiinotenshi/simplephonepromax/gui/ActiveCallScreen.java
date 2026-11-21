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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ActiveCallScreen extends Screen {
   private final List<Button> texturedButtons = new ArrayList<>();
   private Button transferButton;
   private CallState builtState;
   private boolean builtHeld;
   private Button removeParticipantButton;

   public ActiveCallScreen() {
      super(Component.translatable("phone.call.title"));
   }

   private void addTexturedButton(Button button) {
      this.texturedButtons.add(button);
      this.addRenderableWidget(button);
   }

   private boolean stateForNoise(){return SimpleVoiceCallClient.callManager.isInActiveCall();}
   protected void init() {
      try {
         super.init();
      } catch (Throwable var17) {
      }

      this.texturedButtons.clear();
      this.addTexturedButton(Button.builder(Component.literal("Настройки"),b->this.minecraft.setScreenAndShow(new PhoneSettingsScreen())).bounds(Math.max(8,this.width-112),8,104,20).build());
      if(stateForNoise())this.addTexturedButton(Button.builder(Component.literal("Громкость игроков"),b->this.minecraft.setScreenAndShow(new CallVolumesScreen(this))).bounds(Math.max(8,this.width-192),56,184,20).build());
      this.transferButton = null;
      this.removeParticipantButton = null;
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      CallState state = SimpleVoiceCallClient.callManager.getState();
      this.builtState = state;
      this.builtHeld = dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasHeld();
      String otherName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
      UUID otherUuid = SimpleVoiceCallClient.callManager.getOtherPlayerUuid();
      String otherNumber = otherUuid != null ? PhoneNumberManager.getDisplayNumberFor(otherUuid) : null;
      if(state==CallState.INCOMING_RINGING&&otherUuid!=null)this.addTexturedButton(Button.builder(Component.literal("Не звонить 15 минут"),b->{dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.muteFor15Minutes(otherUuid);SimpleVoiceCallClient.callManager.endCall();this.minecraft.setScreenAndShow(new PhoneMainScreen());}).bounds(frame.x()+16,frame.bottom()-30,frame.width()-32,20).build());
      UUID myUuid = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
      int buttonX = frame.x() + frame.px(110);
      int buttonY = frame.y() + frame.px(31);
      int buttonW = Math.max(50, frame.px(68));
      int buttonH = Math.max(11, frame.px(12));
      int gap = Math.max(2, frame.px(2));
      if (state == CallState.ACTIVE) {
         Button muteBtn = Button.builder(
               Component.translatable(VoicechatPluginImpl.isMicrophoneMuted() ? "phone.call.unmute" : "phone.call.mute"), b -> {
                  boolean muted = !VoicechatPluginImpl.isMicrophoneMuted();
                  VoicechatPluginImpl.setMicrophoneMuted(muted);
                  if (this.minecraft != null) {
                     this.minecraft.setScreenAndShow(new ActiveCallScreen());
                  }
               }
            )
            .size(buttonW, buttonH)
            .pos(buttonX, buttonY)
            .build();
         this.addTexturedButton(muteBtn);
         String contactNumber = otherNumber != null ? otherNumber : otherUuid == null ? null : PhoneNumberManager.getDisplayNumberFor(otherUuid);
         boolean alreadyInContacts = contactNumber != null && SimpleVoiceCallClient.config.hasContact(contactNumber);
         String finalOtherName = otherName;
         String finalOtherNumber = otherNumber;
         UUID finalOtherUuid = otherUuid;
         String savedLabel = "";
         if (contactNumber != null) for (ModConfig.Contact contact : SimpleVoiceCallClient.config.contacts) {
            if (dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(contact.server)
               && PhoneNumberManager.onlyDigits(contactNumber).equals(PhoneNumberManager.onlyDigits(contact.number))) { savedLabel = contact.nickname == null ? "" : contact.nickname; break; }
         }
         String finalSavedLabel = savedLabel;
         Button contactBtn = Button.builder(
               Component.translatable(alreadyInContacts ? "phone.call.edit_contact" : "phone.call.add_contact"),
               b -> {
                  String contactName = finalOtherName != null ? finalOtherName : "";
                  if (this.minecraft != null) this.minecraft.setScreenAndShow(new PhoneNickEditScreen(this, finalOtherUuid, finalOtherNumber, contactName, finalSavedLabel));
               }
            )
            .size(buttonW, buttonH)
            .pos(buttonX, buttonY + buttonH + gap)
            .build();
         this.addTexturedButton(contactBtn);
         Button addCallBtn = Button.builder(Component.translatable("phone.call.add_participant"), b -> {
            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new PhoneMainScreen(true));
            }
         }).size(buttonW, buttonH).pos(buttonX, buttonY + 2 * (buttonH + gap)).build();
         this.addTexturedButton(addCallBtn);
         this.removeParticipantButton = Button.builder(Component.translatable("phone.call.remove_participant"), b -> {
            if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.canManageGroup()) this.minecraft.setScreenAndShow(new GroupParticipantsScreen());
         }).size(buttonW, buttonH).pos(buttonX, buttonY + 3 * (buttonH + gap)).build();
         this.removeParticipantButton.active = dev.yukiinotenshi.simplephonepromax.network.CallControlService.canManageGroup();
         this.removeParticipantButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("phone.group.remove_tip")));
         this.addTexturedButton(this.removeParticipantButton);
      }

      if (this.builtHeld) {
         this.addTexturedButton(Button.builder(Component.translatable("phone.hold.open"),b->this.minecraft.setScreenAndShow(new HeldCallScreen()))
            .bounds(8,8,Math.min(240,this.width-16),20).build());
      }
      if (state == CallState.ACTIVE) {
         int controlW = Math.min(this.width - 24, Math.max(160, frame.px(150)));
         int controlY = Math.min(this.height - 24, frame.bottom() + 3);
         this.transferButton = Button.builder(Component.translatable(
            dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasOutgoingTransfer() ? "phone.transfer.cancel" : "phone.transfer.title"), b -> {
            if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasOutgoingTransfer()) {
               dev.yukiinotenshi.simplephonepromax.network.CallControlService.cancelOutgoing();
               this.minecraft.setScreenAndShow(new ActiveCallScreen());
            } else if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.canTransferCurrent()) {
               this.minecraft.setScreenAndShow(new TransferCallScreen());
            } else {
               dev.yukiinotenshi.simplephonepromax.network.CallControlService.message("phone.control.incompatible");
            }
         }).bounds((this.width - controlW) / 2, controlY, controlW, 20).build();
         this.transferButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("phone.control.compatibility_tip")));
         this.addTexturedButton(this.transferButton);
      }

      if (otherUuid != null) {
         boolean blocked = SimpleVoiceCallClient.config.isBlocked(otherUuid);
         int blockY = state == CallState.ACTIVE ? buttonY + 4 * (buttonH + gap) : buttonY;
         Button blockBtn = Button.builder(Component.translatable(blocked ? "phone.call.unblock" : "phone.call.block"), b -> {
            if (blocked) {
               SimpleVoiceCallClient.config.removeBlocked(otherUuid.toString());
            } else {
               SimpleVoiceCallClient.config.addBlocked(otherUuid, otherName);
               if (state == CallState.INCOMING_RINGING) {
                  ModNetworking.sendCallBlocked(otherUuid);
                  SimpleVoiceCallClient.callManager.declineCall();
               }
            }

            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new ActiveCallScreen());
            }
         }).size(buttonW, buttonH).pos(buttonX, blockY).build();
         this.addTexturedButton(blockBtn);
      }

      int mainButtonsY = state == CallState.ACTIVE ? buttonY + 5 * (buttonH + gap) : buttonY + buttonH + gap;
      if (state == CallState.INCOMING_RINGING) {
         Button answerBtn = Button.builder(Component.translatable("phone.incoming.answer"), b -> {
            if (myUuid != null && otherUuid != null) {
               ModNetworking.sendCallAccept(myUuid, otherUuid);
            }
         }).size(buttonW, buttonH).pos(buttonX, mainButtonsY).build();
         this.addTexturedButton(answerBtn);
         Button declineBtn = Button.builder(Component.translatable("phone.call.decline"), b -> {
            if (myUuid != null && otherUuid != null) {
               ModNetworking.sendCallDecline(myUuid, otherUuid);
            }

            SimpleVoiceCallClient.callManager.endCall();
            this.onClose();
         }).size(buttonW, buttonH).pos(buttonX, mainButtonsY + buttonH + gap).build();
         this.addTexturedButton(declineBtn);
      } else if (state == CallState.OUTGOING_RINGING) {
         Button hangupBtn = Button.builder(Component.translatable("phone.call.cancel"), b -> {
            SimpleVoiceCallClient.callManager.endCall();
            if (myUuid != null && otherUuid != null) {
               ModNetworking.sendCallHangup(myUuid, otherUuid);
            }

            this.onClose();
         }).size(buttonW, buttonH).pos(buttonX, mainButtonsY).build();
         this.addTexturedButton(hangupBtn);
      } else {
         Button hangupBtn = Button.builder(Component.translatable(state == CallState.BUSY ? "phone.settings.back" : "phone.call.hangup"), b -> {
            SimpleVoiceCallClient.callManager.endCall();
            if (state != CallState.BUSY && myUuid != null && otherUuid != null) {
               ModNetworking.sendCallHangup(myUuid, otherUuid);
            }

            this.onClose();
         }).size(buttonW, buttonH).pos(buttonX, mainButtonsY).build();
         this.addTexturedButton(hangupBtn);
      }
   }

   public void tick() {
      if (this.builtState != SimpleVoiceCallClient.callManager.getState() || this.builtHeld != dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasHeld()) { this.clearWidgets(); this.init(); }
      if (this.removeParticipantButton != null) this.removeParticipantButton.active = dev.yukiinotenshi.simplephonepromax.network.CallControlService.canManageGroup();
      if (this.transferButton != null) {
         boolean pending = dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasOutgoingTransfer();
         this.transferButton.active = pending || dev.yukiinotenshi.simplephonepromax.network.CallControlService.canTransferCurrent();
         this.transferButton.setMessage(Component.translatable(pending ? "phone.transfer.cancel" : "phone.transfer.title"));
      }
      if (dev.yukiinotenshi.simplephonepromax.network.CallControlService.incoming() != null) {
         this.minecraft.setScreenAndShow(new CallOfferScreen());
      }
   }

   public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTick) {
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
         Component title;
         Component status;
         if (state == CallState.OUTGOING_RINGING) {
            title = Component.translatable("phone.call.calling_player", new Object[]{otherName != null ? otherName : ""});
            status = Component.translatable("phone.call.ringing");
            titleColor = 16777130;
         } else if (state == CallState.INCOMING_RINGING) {
            title = Component.translatable("phone.call.incoming_player", new Object[]{otherName != null ? otherName : ""});
            status = Component.translatable("phone.call.incoming_status");
            titleColor = 5635925;
         } else if (state == CallState.ACTIVE) {
            String visibleNumber = otherUuid == null ? null : PhoneNumberManager.getDisplayNumberFor(otherUuid);
            boolean savedContact = visibleNumber != null && SimpleVoiceCallClient.config.hasContact(visibleNumber);
            String callerLabel = otherName != null ? otherName : "";
            if (!savedContact && otherUuid != null) {
               String visibleCallerNumber = PhoneNumberManager.getDisplayNumberFor(otherUuid);
               callerLabel = visibleCallerNumber == null || visibleCallerNumber.isBlank()
                  ? "Неизвестный номер" : PhoneNumberManager.formatNumber(visibleCallerNumber);
            }
            title = Component.translatable("phone.call.active_player", callerLabel);
            status = Component.translatable("phone.call.connected");
            titleColor = 5636095;
         } else if (state == CallState.BUSY) {
            title = Component.translatable("phone.call.busy");
            status = Component.translatable("phone.call.busy_later");
            titleColor = 0xFFFF5555;
         } else {
            title = Component.translatable("phone.call.title");
            status = Component.empty();
         }

         try {
            PhoneGuiTextures.drawPhone(drawContext, frame, mouseX, mouseY);
         } catch (Throwable var19) {
         }

         try {
            PhoneGuiTextures.drawCenteredTrimmedText(
               drawContext, this.font, title, displayCenterX, displayTextY, Math.max(24, frame.displayWidth() - frame.px(4)), titleColor
            );
         } catch (Throwable var21) {
         }

         if (otherUuid != null) {
            String num = PhoneNumberManager.getDisplayNumberFor(otherUuid);
            Component numL = Component.literal(PhoneNumberManager.formatNumber(num));

            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.font, numL, infoCenterX, frame.y() + frame.px(49), infoMaxWidth, 16777215
               );
            } catch (Throwable var18) {
            }
         }

         if (state == CallState.ACTIVE) {
            boolean muted = VoicechatPluginImpl.isMicrophoneMuted();
            Component mt = Component.translatable(muted ? "phone.call.muted" : "phone.call.microphone_active");
            int mtc = muted ? 16737894 : 6750054;

            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.font, mt, infoCenterX, frame.y() + frame.px(66), infoMaxWidth, mtc
               );
            } catch (Throwable var17) {
            }
         }

         if (status.getString() != null && !status.getString().isEmpty()) {
            try {
               PhoneGuiTextures.drawCenteredTrimmedText(
                  drawContext, this.font, status, infoCenterX, frame.y() + frame.px(84), infoMaxWidth, 16777215
               );
            } catch (Throwable var16) {
            }
         }

         super.extractRenderState(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
         try {
            for (Button button : this.texturedButtons) {
               PhoneGuiTextures.drawButton(drawContext, this.font, button, mouseX, mouseY);
            }
         } catch (Throwable ignored) {
         }
      } catch (Throwable var20) {
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      PhoneClientActions.putAwayPhone();
      super.onClose();
   }
}



