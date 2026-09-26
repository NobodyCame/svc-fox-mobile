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
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class PhoneMainScreen extends Screen {
   private StringBuilder currentNumber = new StringBuilder();
   private final List<ButtonWidget> texturedButtons = new ArrayList<>();
   private final boolean addToCallMode;

   public PhoneMainScreen() {
      this(false);
   }

   public PhoneMainScreen(boolean addToCallMode) {
      super(Text.translatable("phone.screen.title"));
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
      return e instanceof PlayerListEntry entry ? entry.getProfile().id() : null;
   }
   private static String getEntryName(Object e) {
      return e instanceof PlayerListEntry entry ? entry.getProfile().name() : null;
   }

   private void registerAllPlayers() {
      if (this.client != null && this.client.getNetworkHandler() != null) {
         try {
            for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
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

      if (this.client != null && this.client.world != null) {
         try {
            for (AbstractClientPlayerEntity p : this.client.world.getPlayers()) {
               try {
                  PhoneNumberManager.registerPlayer(p.getUuid(), null);
                  ModNetworking.requestPlayerPresence(p.getUuid());
               } catch (Throwable var4) {
               }
            }
         } catch (Throwable var6) {
         }
      }
   }

   private static void toast(Text message) {
      try {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.player != null && message != null) {
            client.player.sendMessage(message, false);
         }
      } catch (Throwable ignored) {
      }
   }

   private void addTexturedButton(ButtonWidget button) {
      this.texturedButtons.add(button);
      this.addDrawableChild(button);
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
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int extraY=Math.min(this.height-23,frame.bottom()+6), extraW=(frame.width()-8)/3;
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Приватный звонок"),b->this.client.setScreen(new PrivateCallScreen())).dimensions(frame.x(),extraY,extraW,20).build()).active=!dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy();
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Организации"),b->this.client.setScreen(new OrganizationScreen())).dimensions(frame.x()+extraW+4,extraY,extraW,20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Просьбы перезвонить"),b->this.client.setScreen(new CallbackScreen())).dimensions(frame.x()+2*(extraW+4),extraY,extraW,20).build());

      int btnW = Math.max(14, frame.px(16));
      int btnH = Math.max(12, frame.px(13));
      int gapX = Math.max(2, frame.px(3));
      int gapY = Math.max(2, frame.px(3));
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
            ButtonWidget btn = ButtonWidget.builder(Text.literal(digit), b -> {
               if (useDigit) {
                  this.appendDigitInternal(digit);
               } else if ("⌫".equals(digit)) {
                  this.backspaceInternal();
               }
            }).size(btnW, btnH).position(x, y).build();
            this.addTexturedButton(btn);
         }
      }

      if(!this.addToCallMode){String last=SimpleVoiceCallClient.config.lastDialedNumbers.get(dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current());
      var redial=ButtonWidget.builder(Text.literal("Повтор"),b->this.client.setScreen(new CallCheckScreen(last))).dimensions(startX,keypadTop+3*(btnH+gapY),btnW,btnH).build();redial.active=last!=null&&!last.isBlank();this.addTexturedButton(redial);}
      int sideX = frame.x() + frame.px(110);
      int sideY = frame.y() + frame.px(31);
      int sideW = Math.max(50, frame.px(68));
      int sideH = Math.max(11, frame.px(12));
      int sideGap = Math.max(2, frame.px(2));
      int sideRow = 0;
      ButtonWidget callBtn = ButtonWidget.builder(Text.translatable(this.addToCallMode ? "phone.add_call.dial" : "phone.screen.call"), b -> this.makeCall())
         .size(sideW, sideH)
         .position(sideX, sideY + sideRow++ * (sideH + sideGap))
         .build();
      this.addTexturedButton(callBtn);
      if (!this.addToCallMode) {
         ButtonWidget historyBtn = ButtonWidget.builder(Text.translatable("phone.history.title"), b -> {
            if (this.client != null) {
               this.client.setScreen(new CallHistoryScreen());
            }
         }).size(sideW, sideH).position(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
         this.addTexturedButton(historyBtn);
      }
      ButtonWidget contactsBtn = ButtonWidget.builder(Text.translatable("phone.contacts.title"), b -> {
         if (this.client != null) {
            this.client.setScreen(new PhoneContactsScreen(this.addToCallMode));
         }
      }).size(sideW, sideH).position(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
      this.addTexturedButton(contactsBtn);
      boolean inCall = SimpleVoiceCallClient.callManager.isInCall();
      ButtonWidget nearbyBtn = ButtonWidget.builder(Text.translatable("phone.nearby.title"), b -> {
         if (this.client != null) {
            this.client.setScreen(new NearbyPlayersScreen(this.addToCallMode));
         }
      }).size(sideW, sideH).position(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
      this.addTexturedButton(nearbyBtn);
      if(!inCall&&!this.addToCallMode){
         ButtonWidget mailbox=ButtonWidget.builder(Text.literal("Голосовые"),b->{if(this.client!=null)this.client.setScreen(new VoicemailScreen(null));})
            .size(sideW,sideH).position(sideX,sideY+sideRow++*(sideH+sideGap)).build();
         this.addTexturedButton(mailbox);
      }
      if (inCall || this.addToCallMode) {
         ButtonWidget currCallBtn = ButtonWidget.builder(Text.translatable(this.addToCallMode ? "phone.add_call.back_to_call" : "phone.screen.current_call"), b -> {
            if (this.client != null) {
               this.client.setScreen(new ActiveCallScreen());
            }
         }).size(sideW, sideH).position(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
         this.addTexturedButton(currCallBtn);
      }

      if (!inCall && !this.addToCallMode) {
         ButtonWidget settingsButton = ButtonWidget.builder(Text.translatable("phone.screen.settings"), b -> {
            if (this.client != null) {
               this.client.setScreen(new PhoneSettingsScreen());
            }
         }).size(sideW, sideH).position(sideX, sideY + sideRow++ * (sideH + sideGap)).build();
         this.addTexturedButton(settingsButton);
      }
      if (!this.addToCallMode) {
         ButtonWidget closeButton = ButtonWidget.builder(Text.translatable("phone.screen.close"), b -> this.close())
            .size(sideW, sideH)
            .position(sideX, sideY + sideRow * (sideH + sideGap))
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
         toast(Text.translatable("phone.message.enter_number"));
      } else if (SimpleVoiceCallClient.callManager.isInCall() && !this.addToCallMode) {
         String otherName = SimpleVoiceCallClient.callManager.getOtherPlayerName();
         toast(
            Text.translatable(
               "phone.message.in_call_with", new Object[]{otherName != null ? otherName : Text.translatable("phone.message.someone")}
            )
         );
      } else {
         if(dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()&&PhoneNumberManager.isValidShortCode(this.currentNumber.toString())){toast(Text.translatable("phone.standard.short_fox"));return;}
         if(!this.addToCallMode){this.client.setScreen(new CallCheckScreen(this.currentNumber.toString()));return;}
         this.registerAllPlayers();
         String dialed=this.currentNumber.toString();
         dev.yukiinotenshi.simplephonepromax.phone.PhoneDialer.resolve(dialed).thenAccept(resolved -> {
            MinecraftClient client=MinecraftClient.getInstance();
            if(client==null)return;
            client.execute(()->{
               if(client.currentScreen!=this||client.player==null||!dialed.equals(this.currentNumber.toString()))return;
               if(resolved==null){if(dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.queued()){toast(Text.literal(dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.status()));return;}dev.yukiinotenshi.simplephonepromax.sound.OperatorSounds.unavailable();toast(Text.translatable("phone.message.subscriber_not_found",PhoneNumberManager.formatNumber(dialed)));return;}
               this.startResolvedCall(resolved,PhoneNumberManager.formatNumber(dialed),PhoneNumberManager.onlyDigits(dialed));
            });
         }).exceptionally(error->{MinecraftClient.getInstance().execute(()->{dev.yukiinotenshi.simplephonepromax.sound.OperatorSounds.unavailable();toast(Text.literal("Не удалось проверить номер у оператора"));});return null;});
      }
   }

   private void startResolvedCall(UUID targetUuid, String targetName, String targetNumberRaw) {
      if (targetUuid == null || this.client == null || this.client.player == null) {
         return;
      }
      // A resolved phone number is sufficient for an ordinary legacy call.
      // Contacts/met-player bookkeeping is not a capability or permission check.
      if (this.client.getNetworkHandler() != null) {
         for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            if (targetUuid.equals(getEntryId(entry))) {
               String onlineName = getEntryName(entry);
               if (onlineName != null) targetName = onlineName;
               break;
            }
         }
      }

      if (this.addToCallMode) {
         if (!SimpleVoiceCallClient.callManager.isInActiveCall()) {
            toast(Text.translatable("phone.add_call.no_active_call"));
            if (this.client != null) {
               this.client.setScreen(new PhoneMainScreen());
            }
            return;
         }

         if (ModNetworking.isPlayerInActiveCallGroup(targetUuid)) {
            toast(Text.translatable("phone.add_call.already_here", targetName));
            if (this.client != null) {
               this.client.setScreen(new ActiveCallScreen());
            }
         } else if (ModNetworking.inviteToActiveCall(targetUuid, targetName)) {
            toast(Text.translatable("phone.add_call.invite_sent", targetName));
            if (this.client != null) {
               this.client.setScreen(new ActiveCallScreen());
            }
         } else {
            toast(Text.translatable("phone.add_call.unavailable", targetName));
         }
         return;
      }

      SimpleVoiceCallClient.LOGGER.info("Phone dial {} resolved to {} ({}) using {}", targetNumberRaw, targetUuid, targetName,
         dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()?"Legacy":"Fox Mobile");
      boolean started = dev.yukiinotenshi.simplephonepromax.phone.PhoneDialer.call(targetUuid, targetName, targetNumberRaw);

      if ((started || SimpleVoiceCallClient.callManager.getState() == CallState.BUSY) && this.client != null) {
         this.client.setScreen(new ActiveCallScreen());
      }
   }

   public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTick) {
      try {
         WallpaperEngine.render(drawContext, this.width, this.height);
      } catch (Throwable var28) {
      }

      try {
         PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
         int centerX = frame.centerX();
         Text title = Text.translatable(this.addToCallMode ? "phone.add_call.title" : "phone.screen.phone");

         try {
            PhoneGuiTextures.drawPhone(drawContext, frame, mouseX, mouseY);
         } catch (Throwable var26) {
         }

         try {
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext, this.textRenderer, title, centerX, frame.y() + frame.px(8), frame.contentWidth(), 16777215);
         } catch (Throwable var25) {
         }

         try {
            int displayW = frame.displayWidth();
            int displayH = frame.displayHeight();
            int displayX = frame.displayX();
            int displayY = frame.displayY();
            Text dText;
            int displayColor;
            if (this.currentNumber.length() == 0) {
               dText = Text.literal("+");
               displayColor = -11154347;
            } else {
               dText = Text.literal(PhoneNumberManager.formatNumber(this.currentNumber.toString()));
               displayColor = -16711834;
            }

            String visibleNumber = this.textRenderer.trimToWidth(dText.getString(), Math.max(4, displayW - 4));
            int baseW = this.textRenderer.getWidth(visibleNumber);
            int ty = displayY + (displayH - 8) / 2;
            int tx = displayX + displayW - baseW - Math.max(2, frame.px(2));

            try {
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, visibleNumber, tx, ty, displayColor, false);
            } catch (Throwable t) {
               try {
                  PhoneGuiTextures.drawText(drawContext, this.textRenderer, visibleNumber, tx, ty, displayColor, false);
               } catch (Throwable var20) {
               }
            }
         } catch (Throwable var23) {
            Throwable t = var23;

            try {
               Text tmsg = Text.translatable("phone.error.render_details");
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, tmsg.getString(), 20, 80, -39322, true);
               Text tmsg2 = Text.translatable(
                  "phone.error.details",
                  new Object[]{t.getClass().getSimpleName(), t.getMessage() != null ? t.getMessage().substring(0, Math.min(80, t.getMessage().length())) : ""}
               );
               PhoneGuiTextures.drawText(drawContext, this.textRenderer, tmsg2.getString(), 20, 100, 16777215, true);
            } catch (Throwable var19) {
            }
         }

         try {
            String legacy=PhoneNumberManager.getMyNumber();
            String backend=this.client.player==null?null:dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService.getNumberFor(this.client.player.getUuid());
            // Keep the number labels below the keypad, inside its left column.
            int numberCenter=frame.x()+frame.px(62),numberWidth=frame.px(88);
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext,this.textRenderer,Text.literal("Legacy: "+PhoneNumberManager.formatNumber(legacy)),numberCenter,frame.y()+frame.px(115),numberWidth,0xFF88FF88);
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext,this.textRenderer,Text.literal("Оператор: "+(backend==null||backend.isBlank()?"не получен":PhoneNumberManager.formatNumber(backend))),numberCenter,frame.y()+frame.px(124),numberWidth,0xFF88FF88);
         } catch (Throwable var24) {
         }

         try {
            super.render(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
         } catch (Throwable var29) {
         }

         try {
            for (ButtonWidget button : this.texturedButtons) {
               PhoneGuiTextures.drawButton(drawContext, this.textRenderer, button, mouseX, mouseY);
            }
         } catch (Throwable ignored) {
         }
      } catch (Throwable var27) {
         Throwable tt = var27;

         try {
            drawContext.fill(0, 0, this.width, this.height, -16777216);
            Text terr = Text.translatable("phone.error.critical_render");
            PhoneGuiTextures.drawText(drawContext, this.textRenderer, terr.getString(), 20, 80, -65536, true);
            Text terr2 = Text.translatable("phone.error.cause", new Object[]{tt.getClass().getSimpleName()});
            PhoneGuiTextures.drawText(drawContext, this.textRenderer, terr2.getString(), 20, 100, 16777215, true);
         } catch (Throwable var18) {
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      if (this.addToCallMode && this.client != null && SimpleVoiceCallClient.callManager.isInActiveCall()) {
         this.client.setScreen(new ActiveCallScreen());
      } else {
         PhoneClientActions.putAwayPhone();
         super.close();
      }
   }
}



