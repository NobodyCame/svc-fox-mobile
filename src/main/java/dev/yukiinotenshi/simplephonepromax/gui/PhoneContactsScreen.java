package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class PhoneContactsScreen extends Screen {
   private final List<PhoneContactsScreen.Row> rows = new ArrayList<>();
   private final List<ButtonWidget> texturedButtons = new ArrayList<>();
   private double scrollAmount;
   private PhoneContactsScreen.Row pendingEdit;
   private int presenceTicks;
   private String searchText = "";
   private TextFieldWidget searchField;
   private int filterMode;
   private final boolean addToCallMode;

   public PhoneContactsScreen() {
      this(false);
   }

   public PhoneContactsScreen(boolean addToCallMode) {
      super(Text.translatable("phone.contacts.title"));
      this.addToCallMode = addToCallMode;
   }

   private static void markMet(UUID uuid, String name) {
      try {
         if (uuid != null) {
            SimpleVoiceCallClient.config.addMet(uuid.toString());
         }

         if (name != null) {
            SimpleVoiceCallClient.config.addMet(name);
         }
      } catch (Throwable var3) {
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

   protected void init() {
      try {
         super.init();
      } catch (Throwable var15) {
      }

      this.rows.clear();
      MinecraftClient mc = this.client;
      Set<String> addedNumbers = new HashSet<>();
      Set<UUID> onlineUuids = new HashSet<>();
      if (mc != null && mc.getNetworkHandler() != null) {
         for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            try {
               UUID pid = entry.getProfile().id();
               if (pid != null) {
                  onlineUuids.add(pid);
               }
            } catch (Throwable var14) {
            }
         }
      }

      if (mc != null && mc.world != null) {
         for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            try {
               onlineUuids.add(p.getUuid());
            } catch (Throwable var13) {
            }
         }
      }

      for (ModConfig.Contact c : SimpleVoiceCallClient.config.contacts) {
         if (!dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(c.server)) continue;
         try {
            String digits = PhoneNumberManager.onlyDigits(c.number);
            if (digits.isEmpty() || !addedNumbers.add(digits)) continue;
            UUID pid = PhoneNumberManager.findUuidByNumber(digits);
            if (pid == null) {
               String name = c.nickname != null && !c.nickname.isBlank() ? c.nickname : c.name != null && !c.name.isEmpty() ? c.name : Text.translatable("phone.contact.default_name").getString();
               this.rows.add(new PhoneContactsScreen.Row(digits, name, c.nickname, null, false, true));
            } else {
               markMet(pid, c.name);
               PhoneNumberManager.registerPlayer(pid, null);
               String num = digits.isEmpty() ? dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService.getPublicNumberFor(pid) : digits;

               String realName = c.name != null && !c.name.isEmpty() ? c.name : Text.translatable("phone.contact.default_name").getString();
               if (mc != null && mc.world != null) {
                  PlayerEntity p = mc.world.getPlayerByUuid(pid);
                  if (p != null) {
                     realName = p.getName().getString();
                  }
               }

               if(mc!=null&&mc.getNetworkHandler()!=null){var entry=mc.getNetworkHandler().getPlayerListEntry(pid);if(entry!=null)realName=entry.getProfile().name();}
               if(c.nickname==null&&!java.util.Objects.equals(c.name,realName))c.nickname=c.name;
               c.name=realName;
               boolean online = onlineUuids.contains(pid);
               String custom = this.findCustomNameFor(pid, num, realName);
               this.rows.add(new PhoneContactsScreen.Row(num, realName, custom, pid, online, true));
            }
         } catch (Throwable var16) {
         }
      }

      this.rebuildButtons();
   }

   private void addTexturedButton(ButtonWidget button) {
      this.texturedButtons.add(button);
      this.addDrawableChild(button);
   }

   private String findCustomNameFor(UUID uuid, String number, String fallback) {
      if (number != null) {
         String digits = PhoneNumberManager.onlyDigits(number);

         for (ModConfig.Contact c : SimpleVoiceCallClient.config.contacts) {
         if (!dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(c.server)) continue;
            String cd = PhoneNumberManager.onlyDigits(c.number);
            if (cd.equals(digits) && c.name != null && !c.name.isEmpty()) {
               return c.nickname!=null?c.nickname:c.name;
            }
         }
      }

      return fallback;
   }

   private void rebuildButtons() {
      this.clearChildren();
      this.texturedButtons.clear();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int searchX = listX(frame);
      int searchY = frame.y() + frame.px(22);
      int addW=Math.max(20,frame.px(25)),filterW=Math.max(30,frame.px(34)),searchW=listW(frame)-filterW-addW-frame.px(4);
      int searchH = Math.max(12, frame.px(14));
      this.searchField = new TextFieldWidget(this.textRenderer, searchX, searchY, searchW, searchH, Text.translatable("phone.search"));
      this.searchField.setMaxLength(80);
      this.searchField.setText(this.searchText);
      this.searchField.setPlaceholder(Text.translatable("phone.search.contacts"));
      this.searchField.setChangedListener(value -> {
         this.searchText = value != null ? value : "";
         this.clampScroll();
      });
      this.addDrawableChild(this.searchField);
      String filterLabel=switch(this.filterMode){case 1->"Онлайн";case 2->"★";default->"Все";};
      String filterHint=switch(this.filterMode){case 1->"Фильтр: игроки в сети";case 2->"Фильтр: избранные контакты";default->"Фильтр: все контакты";};
      this.addTexturedButton(ButtonWidget.builder(Text.literal(filterLabel),b->{this.filterMode=(this.filterMode+1)%3;this.scrollAmount=0;this.rebuildButtons();})
         .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(filterHint)))
         .size(filterW,searchH).position(searchX+searchW+frame.px(2),searchY).build());
      this.addTexturedButton(ButtonWidget.builder(Text.literal("+"),b->{if(this.client!=null)this.client.setScreen(new PhoneNickEditScreen(this,null,"","",""));})
         .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Добавить контакт по номеру")))
         .size(addW,searchH).position(searchX+searchW+filterW+frame.px(4),searchY).build());
      int backW = Math.max(68, frame.px(88));
      int backH = Math.max(12, frame.px(14));
      int backY = Math.min(this.height - backH - 4, frame.bottom() + Math.max(3, frame.px(3)));
      ButtonWidget backBtn = ButtonWidget.builder(Text.translatable("phone.back_to_phone"), b -> {
         if (this.client != null) {
            this.client.setScreen(this.addToCallMode ? new PhoneMainScreen(true) : new PhoneMainScreen());
         }
      }).size(backW, backH).position(frame.centerX() - backW / 2, backY).build();
      this.addTexturedButton(backBtn);
   }

   public void tick() {
      super.tick();
      if (++this.presenceTicks >= 20) {
         this.presenceTicks = 0;
         if (this.client != null && this.client.getNetworkHandler() != null) {
            Set<UUID> online = new HashSet<>();

            for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
               UUID uuid = entry.getProfile().id();
               if (uuid != null) {
                  online.add(uuid);
               }
            }

            boolean changed = false;

            for (PhoneContactsScreen.Row row : this.rows) {
               boolean value = row.uuid != null && online.contains(row.uuid);
               if (row.online != value) {
                  row.online = value;
                  changed = true;
               }
            }

            if (changed) {
               this.rebuildButtons();
            }
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int btnH = rowHeight(frame);
      int total = this.visibleRows().size() * btnH;
      int visible = Math.max(btnH, listBottom(frame) - listY(frame));
      int max = Math.max(0, total - visible);
      this.scrollAmount = Math.max(0.0, Math.min(max, this.scrollAmount - verticalAmount * btnH));
      return true;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.handleRowClick(mouseX, mouseY);
   }

   public boolean mouseClicked(Click click, boolean doubleClick) {
      return super.mouseClicked(click, doubleClick) || this.handleRowClick(click.x(), click.y());
   }

   private boolean handleRowClick(double mouseX, double mouseY) {
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int rowX = listX(frame);
      int rowY = listY(frame);
      int rowW = listW(frame);
      int rowH = rowHeight(frame);
      int actionGap = Math.max(2, frame.px(2));
      int actionW = Math.max(18, frame.px(18));
      int deleteX = rowX + rowW - frame.px(4) - actionW;
      int editX = deleteX - actionGap - actionW;
      int favoriteX = editX - actionGap - actionW;
      int visibleBottom = listBottom(frame);
      if (mouseY < rowY || mouseY >= visibleBottom) { return false; }
      int yOff = (int)(-this.scrollAmount);
      List<PhoneContactsScreen.Row> visibleRows = this.visibleRows();
      for (int i = 0; i < visibleRows.size(); i++) {
         int y = rowY + i * rowH + yOff;
         if (y + rowH < rowY) {
            continue;
         }

         if (y > visibleBottom) {
            break;
         }

         if (mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= y && mouseY <= y + rowH) {
            PhoneContactsScreen.Row row = visibleRows.get(i);
            if (mouseX >= deleteX && mouseX <= deleteX + actionW) {
               this.removeRow(row);
            } else if (mouseX >= editX && mouseX <= editX + actionW) {
               this.pendingEdit = row;
               if (this.client != null) {
                  this.client.setScreen(new PhoneNickEditScreen(this, row.uuid, row.number, row.realName, row.customName));
               }
            } else if (mouseX >= favoriteX && mouseX <= favoriteX + actionW) {
               String key = favoriteKey(row);
               if (!SimpleVoiceCallClient.config.favoriteContacts.remove(key)) SimpleVoiceCallClient.config.favoriteContacts.add(key);
               SimpleVoiceCallClient.config.save();
            } else {
               this.callRow(row);
            }
            return true;
         }
      }

      return false;
   }

   private int listX(PhoneGuiTextures.Frame frame) {
      return frame.x() + frame.px(18);
   }

   private int listY(PhoneGuiTextures.Frame frame) {
      return frame.y() + frame.px(42);
   }

   private int listW(PhoneGuiTextures.Frame frame) {
      return Math.min(frame.px(160),frame.width()-frame.px(28));
   }

   private int listBottom(PhoneGuiTextures.Frame frame) {
      return frame.y() + frame.px(124);
   }

   private void clampScroll() {
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int rowH = rowHeight(frame);
      int visible = Math.max(rowH, listBottom(frame) - listY(frame));
      int max = Math.max(0, this.visibleRows().size() * rowH - visible);
      this.scrollAmount = Math.max(0.0, Math.min(max, this.scrollAmount));
   }

   private int rowHeight(PhoneGuiTextures.Frame frame){return Math.max(22,frame.px(23));}

   private static String favoriteKey(Row row) { return dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current()+"/"+PhoneNumberManager.onlyDigits(row.number); }
   private List<PhoneContactsScreen.Row> visibleRows() {
      String query = this.searchText == null ? "" : this.searchText.trim().toLowerCase();
      List<PhoneContactsScreen.Row> out = new ArrayList<>();
      String digits = PhoneNumberManager.onlyDigits(query);
      for (PhoneContactsScreen.Row row : this.rows) {
         boolean favorite=SimpleVoiceCallClient.config.favoriteContacts.contains(favoriteKey(row));
         if(this.filterMode==1&&!row.online||this.filterMode==2&&!favorite)continue;
         String haystack = ((row.realName != null ? row.realName : "") + " " + (row.customName != null ? row.customName : "") + " " + PhoneNumberManager.formatNumber(row.number) + " " + row.number).toLowerCase();
         if (query.isEmpty()||haystack.contains(query) || (!digits.isEmpty() && PhoneNumberManager.onlyDigits(row.number).contains(digits))) {
            out.add(row);
         }
      }
      // Presence takes precedence so every offline entry stays below every online one.
      out.sort(java.util.Comparator.comparing((Row row)->!row.online)
         .thenComparing(row->!SimpleVoiceCallClient.config.favoriteContacts.contains(favoriteKey(row)))
         .thenComparing(row->Objects.toString(row.customName,row.realName),String.CASE_INSENSITIVE_ORDER));
      return out;
   }

   private void callRow(PhoneContactsScreen.Row row) {
      if (row == null) return;

      String name = row.customName != null && !row.customName.isEmpty() ? row.customName : row.realName;
      if (this.addToCallMode) {
         if (row.uuid == null || !row.online) return;
         if (!SimpleVoiceCallClient.callManager.isInActiveCall()) {
            toast(Text.translatable("phone.add_call.no_active_call"));
            if (this.client != null) {
               this.client.setScreen(new PhoneMainScreen());
            }
         } else if (ModNetworking.isPlayerInActiveCallGroup(row.uuid)) {
            toast(Text.translatable("phone.add_call.already_here", name));
            if (this.client != null) {
               this.client.setScreen(new ActiveCallScreen());
            }
         } else if (ModNetworking.inviteToActiveCall(row.uuid, name)) {
            toast(Text.translatable("phone.add_call.invite_sent", name));
            if (this.client != null) {
               this.client.setScreen(new ActiveCallScreen());
            }
         } else {
            toast(Text.translatable("phone.add_call.unavailable", name));
         }

         return;
      }

      if (SimpleVoiceCallClient.callManager.isInCall()) {
         return;
      }

      String contactNumber = PhoneNumberManager.onlyDigits(row.number);
      if (!PhoneNumberManager.isValidKnownNumber(contactNumber)) {
         toast(Text.literal("Сначала укажите номер контакта"));
         return;
      }
      if(this.client!=null)this.client.setScreen(new CallCheckScreen(contactNumber));
   }

   private void removeRow(PhoneContactsScreen.Row row) {
      if (row == null) {
         return;
      }

      String dig = PhoneNumberManager.onlyDigits(row.number);
      List<ModConfig.Contact> toRemove = new ArrayList<>();
      for (ModConfig.Contact contact : SimpleVoiceCallClient.config.contacts) {
         if (!dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(contact.server)) continue;
         boolean match = false;
         String cd = PhoneNumberManager.onlyDigits(contact.number);
         if (dig != null && !dig.isEmpty() && cd.equals(dig)) match = true;

         if (match) {
            toRemove.add(contact);
         }
      }

      SimpleVoiceCallClient.config.contacts.removeAll(toRemove);
      SimpleVoiceCallClient.config.save();
      if (this.client != null) {
         this.client.setScreen(new PhoneContactsScreen());
      }
   }

   public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTick) {
      try {
         WallpaperEngine.render(drawContext, this.width, this.height);
         PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
         PhoneGuiTextures.drawUniversalBackground(drawContext, frame, mouseX, mouseY);
         int cx = frame.centerX();
         Text title = Text.translatable("phone.contacts.title_count", new Object[]{this.visibleRows().size()});

         try {
            PhoneGuiTextures.drawCenteredTrimmedText(drawContext, this.textRenderer, title, cx, frame.y() + frame.px(7), frame.px(150), -1);
         } catch (Throwable var10) {
         }

         try {
            int rowX = listX(frame);
            int rowY = listY(frame);
            int rowW = listW(frame);
            int rowH = rowHeight(frame);
            int visibleBottom = listBottom(frame);
            int yOff = (int)(-this.scrollAmount);
            UUID myUuid = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
            drawContext.enableScissor(rowX, rowY, rowX + rowW, visibleBottom);
            int lastBottom = rowY;
            List<PhoneContactsScreen.Row> visibleRows = this.visibleRows();
            if(visibleRows.isEmpty()) {
               String empty=this.searchText.isBlank()?"Здесь пока нет контактов":"Ничего не найдено";
               PhoneGuiTextures.drawCenteredTrimmedText(drawContext,this.textRenderer,Text.literal(empty),frame.centerX(),rowY+frame.px(34),rowW-8,0xFFBFC6D0);
            }
            for (int i = 0; i < visibleRows.size(); i++) {
               PhoneContactsScreen.Row row = visibleRows.get(i);
               int y = rowY + i * rowH + yOff;
               if (y + rowH < rowY) {
                  continue;
               }

               if (y > visibleBottom) {
                  break;
               }

               boolean hovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= y && mouseY <= y + rowH;
               boolean isSelf = myUuid != null && row.uuid != null && row.uuid.equals(myUuid);
               boolean inPhoneCall = row.uuid != null && ModNetworking.isPlayerInPhoneCall(row.uuid);
               int actionGap = Math.max(2, frame.px(2));
               int actionW = Math.max(18, frame.px(18));
               int actionH = Math.min(rowH - 4, Math.max(14, frame.px(14)));
               int actionY = y + (rowH - actionH) / 2;
               int deleteX = rowX + rowW - frame.px(4) - actionW;
               int editX = deleteX - actionGap - actionW;
               int favoriteX = editX - actionGap - actionW;
               int textX = rowX + frame.px(17);
               int textY = y + frame.px(3);
               PhoneGuiTextures.drawContactItem(drawContext, frame, rowX, y, rowW, rowH, hovered);
               PhoneGuiTextures.drawOnlineIcon(drawContext, rowX + frame.px(4), textY, row.online);
               String displayName = row.customName != null && !row.customName.isEmpty() && !row.customName.equals(row.realName)
                  ? row.customName
                  : row.realName;
               String nameText=(isSelf?"Вы · ":"")+(inPhoneCall?"☎ ":"")+displayName;
               String numberLabel=row.number==null||row.number.isBlank()?"номер не добавлен":PhoneNumberManager.formatNumber(row.number);
               String statusText=numberLabel+" · "+(row.online?"в сети":"не в сети")+(inPhoneCall?" · разговор":"");
               PhoneGuiTextures.drawTrimmedText(
                  drawContext,
                  this.textRenderer,
                  nameText,
                  textX,
                  textY,
                  Math.max(18, favoriteX - textX - actionGap),
                  isSelf ? 0xFFFFE070 : SimpleVoiceCallClient.config.favoriteContacts.contains(favoriteKey(row)) ? 0xFFFFE070 : inPhoneCall ? 0xFF88FF88 : -1,
                  true
               );
               PhoneGuiTextures.drawTrimmedText(drawContext,this.textRenderer,statusText,textX,y+frame.px(13),Math.max(18,favoriteX-textX-actionGap),0xFFBFC6D0,false);
               PhoneGuiTextures.drawButtonRect(drawContext, this.textRenderer,
                  Text.literal(SimpleVoiceCallClient.config.favoriteContacts.contains(favoriteKey(row)) ? "★" : "☆"),
                  favoriteX, actionY, actionW, actionH, mouseX >= favoriteX && mouseX <= favoriteX + actionW && mouseY >= actionY && mouseY <= actionY + actionH, true);
               PhoneGuiTextures.drawButtonRect(
                  drawContext,
                  this.textRenderer,
                  Text.literal("✎"),
                  editX,
                  actionY,
                  actionW,
                  actionH,
                  mouseX >= editX && mouseX <= editX + actionW && mouseY >= actionY && mouseY <= actionY + actionH,
                  true
               );
               PhoneGuiTextures.drawButtonRect(
                  drawContext,
                  this.textRenderer,
                  Text.literal("×"),
                  deleteX,
                  actionY,
                  actionW,
                  actionH,
                  mouseX >= deleteX && mouseX <= deleteX + actionW && mouseY >= actionY && mouseY <= actionY + actionH,
                  true
               );
               lastBottom = Math.max(lastBottom, y + rowH);
            }
            drawContext.disableScissor();
         } catch (Throwable var9) {
            try {
               drawContext.disableScissor();
            } catch (Throwable ignored) {
            }
         }

         try {
            super.render(drawContext,mouseX,mouseY,partialTick);PhoneGuiTextures.widgets(this,drawContext,mouseX,mouseY);
            for (ButtonWidget button : this.texturedButtons) {
               PhoneGuiTextures.drawButton(drawContext, this.textRenderer, button, mouseX, mouseY);
            }
         } catch (Throwable ignored) {
         }
      } catch (Throwable var11) {
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

   private static class Row {
      String number;
      String realName;
      String customName;
      UUID uuid;
      boolean online;
      boolean isSavedContact;

      Row(String number, String realName, String customName, UUID uuid, boolean online, boolean isSavedContact) {
         this.number = number;
         this.realName = realName;
         this.customName = customName;
         this.uuid = uuid;
         this.online = online;
         this.isSavedContact = isSavedContact;
      }
   }
}



