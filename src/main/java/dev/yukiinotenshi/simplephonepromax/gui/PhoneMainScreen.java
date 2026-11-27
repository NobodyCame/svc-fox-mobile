package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class PhoneMainScreen extends Screen {
   private StringBuilder currentNumber = new StringBuilder();
   private final List<Button> texturedButtons = new ArrayList<>();
   private final boolean addToCallMode;

   public PhoneMainScreen() {
      this(false);
   }

   public PhoneMainScreen(boolean addToCallMode) {
      super(Component.translatable("phone.screen.title"));
      this.addToCallMode = addToCallMode;
      PhoneNumberManager.load();
   }

   private static UUID callGetIdVia(Object o) {
      try {
         return (UUID)o.getClass().getMethod("getId").invoke(o);
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static String callGetNameVia(Object o) {
      try {
         return (String)o.getClass().getMethod("getName").invoke(o);
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static Object tryCallGetter(Object e, String... names) {
      if (e == null) {
         return null;
      }

      for (Class<?> c = e.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (String name : names) {
            try {
               return c.getMethod(name).invoke(e);
            } catch (Throwable var12) {
            }
         }
      }

      for (String name : names) {
         try {
            for (Class<?> c2 = e.getClass(); c2 != null && c2 != Object.class; c2 = c2.getSuperclass()) {
               try {
                  Field f = c2.getDeclaredField(name);
                  f.setAccessible(true);
                  return f.get(e);
               } catch (Throwable var10) {
               }
            }
         } catch (Throwable var11) {
         }
      }

      return null;
   }

   private static UUID getEntryId(Object e) {
      return e instanceof PlayerInfo entry ? entry.getProfile().id() : null;
   }
   private static String getEntryName(Object e) {
      return e instanceof PlayerInfo entry ? entry.getProfile().name() : null;
   }

   private void registerAllPlayers() {
      if (this.minecraft != null && this.minecraft.getConnection() != null) {
         try {
            for (PlayerInfo entry : this.minecraft.getConnection().getOnlinePlayers()) {
               try {
                  UUID pid = getEntryId(entry);
                  if (pid != null) {
                     PhoneNumberManager.registerPlayer(pid, null);
                     ModNetworking.requestPlayerPresence(pid);
                  }
               } catch (Throwable var5) {
               }
            }
         } catch (Throwable var7) {
         }
      }

      if (this.minecraft != null && this.minecraft.level != null) {
         try {
            for (AbstractClientPlayer p : this.minecraft.level.players()) {
               try {
                  PhoneNumberManager.registerPlayer(p.getUUID(), null);
                  ModNetworking.requestPlayerPresence(p.getUUID());
               } catch (Throwable var4) {
               }
            }
         } catch (Throwable var6) {
         }
      }
   }

   private static void toast(Component message) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.player != null && message != null) {
            client.player.sendSystemMessage(message);
         }
      } catch (Throwable ignored) {
      }
   }

   private void addTexturedButton(Button button) {
      this.texturedButtons.add(button);
      this.addRenderableWidget(button);
   }

   protected void init() {
      try {
         super.init();
      } catch (Throwable var24) {
      }

      try {
         this.registerAllPlayers();
      } catch (Throwable var23) {
      }

      this.texturedButtons.clear();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.dialerLayout(this.width, this.height);
      int extraY=Math.min(this.height-23,frame.bottom()+6), extraW=(frame.width()-8)/3;
      this.addRenderableWidget(Button.builder(Component.literal("Приватный звонок"),b->this.minecraft.setScreenAndShow(new PrivateCallScreen())).bounds(frame.x(),extraY,extraW,20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Организации"),b->this.minecraft.setScreenAndShow(new OrganizationScreen())).bounds(frame.x()+extraW+4,extraY,extraW,20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Просьбы перезвонить"),b->this.minecraft.setScreenAndShow(new CallbackScreen())).bounds(frame.x()+2*(extraW+4),extraY,extraW,20).build());

      int btnW = Math.max(9, frame.px(16));
      int gapX = Math.max(2, frame.px(3));
      int gapY = Math.max(1, frame.px(3));
      int btnH = Math.min(Math.max(9, frame.px(13)),
         Math.max(1, (frame.px(115) - frame.px(49) - 3 * gapY - 2) / 4));
      int startX = frame.x() + frame.px(18);
      int keypadTop = frame.y() + frame.px(49);
      String[][] layout = new String[][]{{"1", "2", "3", "⌫"}, {"4", "5", "6", "*"}, {"7", "8", "9", "#"}, {"", "0", "", ""}};

      for (int row = 0; row < layout.length; row++) {
         for (int col = 0; col < layout[row].length; col++) {
            String digit = layout[row][col];
            if (digit.isEmpty()) {
               continue;
            }

            boolean isDigit = digit.equals("1")
               || digit.equals("2")
               || digit.equals("3")
               || digit.equals("4")
               || digit.equals("5")
               || digit.equals("6")
               || digit.equals("7")
               || digit.equals("8")
               || digit.equals("9")
               || digit.equals("0");
            boolean useDigit = isDigit;
            int x = startX + col * (btnW + gapX);
            int y = keypadTop + row * (btnH + gapY);
            Button btn = Button.builder(Component.literal(digit), b -> {
               if (useDigit) {
                  this.appendDigitInternal(digit);
               } else if ("⌫".equals(digit)) {
                  this.backspaceInternal();
               }
            }).size(btnW, btnH).pos(x, y).build();
            this.addTexturedButton(btn);
         }
      }

      if(!this.addToCallMode){String last=SimpleVoiceCallClient.config.lastDialedNumbers.get(dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current());
      var redial=Button.builder(Component.literal("Повтор"),b->this.minecraft.setScreenAndShow(new CallCheckScreen(last))).bounds(startX,keypadTop+3*(btnH+gapY),btnW,btnH).build();redial.active=last!=null&&!last.isBlank();this.addTexturedButton(redial);}
      int sideX = frame.x() + frame.px(110);
      int sideY = frame.y() + frame.px(31);
      int sideW = Math.max(1, frame.px(68));
      int sideGap = Math.max(1, frame.px(2));
      int sideH = Math.min(Math.max(9, frame.px(12)),
         Math.max(1, (frame.height() - frame.px(31) - 6 * sideGap - 2) / 7));
      int sideRow = 0;
      Button callBtn = Button.builder(Component.translatable(this.addToCallMode ? "phone.add_call.dial" : "phone.screen.call"), b -> this.makeCall())
         .size(sideW, sideH)
         .pos(sideX, sideY + sideRow++ * (sideH + sideGap))
         .build();
      this.addTexturedButton(callBtn);
      if (!this.addToCallMode) {
         Button historyBtn = Button.builder(Component.translatable("phone.history.title"), b -> {
            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new CallHistoryScreen());
            }
         }).size(sideW, sideH).pos(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
         this.addTexturedButton(historyBtn);
      }
      Button contactsBtn = Button.builder(Component.translatable("phone.contacts.title"), b -> {
         if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new PhoneContactsScreen(this.addToCallMode));
         }
      }).size(sideW, sideH).pos(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
      this.addTexturedButton(contactsBtn);
      boolean inCall = SimpleVoiceCallClient.callManager.isInCall();
      Button nearbyBtn = Button.builder(Component.translatable("phone.nearby.title"), b -> {
         if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new NearbyPlayersScreen(this.addToCallMode));
         }
      }).size(sideW, sideH).pos(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
      this.addTexturedButton(nearbyBtn);
      if(!inCall&&!this.addToCallMode){
         Button mailbox=Button.builder(Component.literal("Голосовые"),b->{if(this.minecraft!=null)this.minecraft.setScreenAndShow(new VoicemailScreen(null));})
            .size(sideW,sideH).pos(sideX,sideY+sideRow++*(sideH+sideGap)).build();
         this.addTexturedButton(mailbox);
      }
      if (inCall || this.addToCallMode) {
         Button currCallBtn = Button.builder(Component.translatable(this.addToCallMode ? "phone.add_call.back_to_call" : "phone.screen.current_call"), b -> {
            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new ActiveCallScreen());
            }
         }).size(sideW, sideH).pos(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
         this.addTexturedButton(currCallBtn);
      }

      if (!inCall && !this.addToCallMode) {
         Button settingsButton = Button.builder(Component.translatable("phone.screen.settings"), b -> {
            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new PhoneSettingsScreen());
            }
         }).size(sideW, sideH).pos(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
         this.addTexturedButton(settingsButton);
      }
      if (!this.addToCallMode) {
         Button closeButton = Button.builder(Component.translatable("phone.screen.close"), b -> this.onClose())
            .size(sideW, sideH)
            .pos(sideX, sideY + sideRow * (sideH + sideGap))
            .build();
         this.addTexturedButton(closeButton);
      }
   }

   private void appendDigitInternal(String digit) {
      if (digit != null) {
         String d = PhoneNumberManager.onlyDigits(digit);
         if (!d.isEmpty()) {
            PhoneNumberManager.Region r = PhoneNumberManager.getCurrentRegion();
            int maxLen = 15; // E.164 supports calling codes up to three digits.
            if (this.currentNumber.length() < maxLen) {
               this.currentNumber.append(d);
            }
         }
      }
   }

   private void backspaceInternal() {
      if (this.currentNumber.length() > 0) {
         this.currentNumber.deleteCharAt(this.currentNumber.length() - 1);
      }
   }

   private static double ex(Entity e) {
      try {
         try {
            return (Double)e.getClass().getMethod("getX").invoke(e);
         } catch (Throwable var3) {
            try {
               return e.getX();
            } catch (Throwable var2) {
            }
         }
      } catch (Throwable var4) {
      }

      return 0.0;
   }

   private static double ey(Entity e) {
      try {
         try {
            return (Double)e.getClass().getMethod("getY").invoke(e);
         } catch (Throwable var3) {
            try {
               return e.getY();
            } catch (Throwable var2) {
            }
         }
      } catch (Throwable var4) {
      }

      return 0.0;
   }

   private static double ez(Entity e) {
      try {
         try {
            return (Double)e.getClass().getMethod("getZ").invoke(e);
         } catch (Throwable var3) {
            try {
               return e.getZ();
            } catch (Throwable var2) {
            }
         }
      } catch (Throwable var4) {
      }

      return 0.0;
   }

   private void makeCall() {
      if (this.currentNumber.length() == 0) {
         toast(Component.translatable("phone.message.enter_number"));
      } else if (SimpleVoiceCallClient.callManager.isInCall() && !this.addToCallMode) {
         String otherName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
         toast(
            Component.translatable(
               "phone.message.in_call_with", new Object[]{otherName != null ? otherName : Component.translatable("phone.message.someone")}
            )
         );
      } else {
         if(dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()&&PhoneNumberManager.isValidShortCode(this.currentNumber.toString())){toast(Component.translatable("phone.standard.short_fox"));return;}
         if(!this.addToCallMode){this.minecraft.setScreenAndShow(new CallCheckScreen(this.currentNumber.toString()));return;}
         this.registerAllPlayers();
         String dialed=this.currentNumber.toString();
         dev.yukiinotenshi.simplephonepromax.phone.PhoneDialer.resolve(dialed).thenAccept(resolved -> {
            Minecraft client=Minecraft.getInstance();
            if(client==null)return;
            client.execute(()->{
               if(!dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this)||client.player==null||!dialed.equals(this.currentNumber.toString()))return;
               if(resolved==null){if(dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.queued()){toast(Component.literal(dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.status()));return;}dev.yukiinotenshi.simplephonepromax.sound.OperatorSounds.unavailable();toast(Component.translatable("phone.message.subscriber_not_found",PhoneNumberManager.formatNumber(dialed)));return;}
               this.startResolvedCall(resolved,PhoneNumberManager.formatNumber(dialed),PhoneNumberManager.onlyDigits(dialed));
            });
         }).exceptionally(error->{Minecraft.getInstance().execute(()->{dev.yukiinotenshi.simplephonepromax.sound.OperatorSounds.unavailable();toast(Component.literal("Не удалось проверить номер у оператора"));});return null;});
      }
   }

   private void startResolvedCall(UUID targetUuid, String targetName, String targetNumberRaw) {
      if (targetUuid == null || this.minecraft == null || this.minecraft.player == null) {
         return;
      }
      // A resolved phone number is sufficient for an ordinary legacy call.
      // Contacts/met-player bookkeeping is not a capability or permission check.
      if (this.minecraft.getConnection() != null) {
         for (PlayerInfo entry : this.minecraft.getConnection().getOnlinePlayers()) {
            if (targetUuid.equals(getEntryId(entry))) {
               String onlineName = getEntryName(entry);
               if (onlineName != null) targetName = onlineName;
               break;
            }
         }
      }

      if (this.addToCallMode) {
         if (!SimpleVoiceCallClient.callManager.isInActiveCall()) {
            toast(Component.translatable("phone.add_call.no_active_call"));
            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new PhoneMainScreen());
            }
            return;
         }

         if (ModNetworking.isPlayerInActiveCallGroup(targetUuid)) {
            toast(Component.translatable("phone.add_call.already_here", targetName));
            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new ActiveCallScreen());
            }
         } else if (ModNetworking.inviteToActiveCall(targetUuid, targetName)) {
            toast(Component.translatable("phone.add_call.invite_sent", targetName));
            if (this.minecraft != null) {
               this.minecraft.setScreenAndShow(new ActiveCallScreen());
            }
         } else {
            toast(Component.translatable("phone.add_call.unavailable", targetName));
         }
         return;
      }

      SimpleVoiceCallClient.LOGGER.info("Phone dial {} resolved to {} ({}) using {}", targetNumberRaw, targetUuid, targetName,
         dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()?"Fox Mobile / Legacy-номер":"Fox Mobile");
      boolean started = dev.yukiinotenshi.simplephonepromax.phone.PhoneDialer.call(targetUuid, targetName, targetNumberRaw);

      if ((started || SimpleVoiceCallClient.callManager.getState() == CallState.BUSY) && this.minecraft != null) {
         this.minecraft.setScreenAndShow(new ActiveCallScreen());
      }
   }

   public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTick) {
      try {
         WallpaperEngine.render(drawContext, this.width, this.height);
      } catch (Throwable var28) {
      }

      try {
         PhoneGuiTextures.Frame frame = PhoneGuiTextures.dialerLayout(this.width, this.height);
         int centerX = frame.centerX();
         Component title = Component.translatable(this.addToCallMode ? "phone.add_call.title" : "phone.screen.phone");

         try {
            PhoneGuiTextures.drawPhone(drawContext, frame, mouseX, mouseY);
         } catch (Throwable var26) {
         }

         try {
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext, this.font, title, centerX, frame.y() + frame.px(8), frame.contentWidth(), 16777215);
         } catch (Throwable var25) {
         }

         try {
            int displayW = frame.displayWidth();
            int displayH = frame.displayHeight();
            int displayX = frame.displayX();
            int displayY = frame.displayY();
            Component dText;
            int displayColor;
            if (this.currentNumber.length() == 0) {
               dText = Component.literal("+");
               displayColor = -11154347;
            } else {
               dText = Component.literal(PhoneNumberManager.formatNumber(this.currentNumber.toString()));
               displayColor = -16711834;
            }

            PhoneGuiTextures.drawFittedRightText(drawContext, this.font, dText.getString(),
               displayX + displayW - 2, displayY + displayH / 2, Math.max(1, displayW - 4), displayColor);
         } catch (Throwable var23) {
            Throwable t = var23;

            try {
               Component tmsg = Component.translatable("phone.error.render_details");
               PhoneGuiTextures.drawText(drawContext, this.font, tmsg.getString(), 20, 80, -39322, true);
               Component tmsg2 = Component.translatable(
                  "phone.error.details",
                  new Object[]{t.getClass().getSimpleName(), t.getMessage() != null ? t.getMessage().substring(0, Math.min(80, t.getMessage().length())) : ""}
               );
               PhoneGuiTextures.drawText(drawContext, this.font, tmsg2.getString(), 20, 100, 16777215, true);
            } catch (Throwable var19) {
            }
         }

         try {
            String legacy=PhoneNumberManager.getMyNumber();
            String backend=this.minecraft.player==null?null:dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService.getNumberFor(this.minecraft.player.getUUID());
            // Keep the number labels below the keypad, inside its left column.
            int numberCenter=frame.x()+frame.px(62),numberWidth=frame.px(88);
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext,this.font,Component.literal("Legacy: "+PhoneNumberManager.formatNumber(legacy)),numberCenter,frame.y()+frame.px(115),numberWidth,0xFF88FF88);
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext,this.font,Component.literal("Оператор: "+(backend==null||backend.isBlank()?"не получен":PhoneNumberManager.formatNumber(backend))),numberCenter,frame.y()+frame.px(124),numberWidth,0xFF88FF88);
         } catch (Throwable var24) {
         }

         try {
            super.extractRenderState(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
         } catch (Throwable var29) {
         }

         try {
            for (Button button : this.texturedButtons) {
               PhoneGuiTextures.drawButton(drawContext, this.font, button, mouseX, mouseY);
            }
         } catch (Throwable ignored) {
         }
      } catch (Throwable var27) {
         Throwable tt = var27;

         try {
            drawContext.fill(0, 0, this.width, this.height, -16777216);
            Component terr = Component.translatable("phone.error.critical_render");
            PhoneGuiTextures.drawText(drawContext, this.font, terr.getString(), 20, 80, -65536, true);
            Component terr2 = Component.translatable("phone.error.cause", new Object[]{tt.getClass().getSimpleName()});
            PhoneGuiTextures.drawText(drawContext, this.font, terr2.getString(), 20, 100, 16777215, true);
         } catch (Throwable var18) {
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      if (this.addToCallMode && this.minecraft != null && SimpleVoiceCallClient.callManager.isInActiveCall()) {
         this.minecraft.setScreenAndShow(new ActiveCallScreen());
      } else {
         PhoneClientActions.putAwayPhone();
         super.onClose();
      }
   }
}



