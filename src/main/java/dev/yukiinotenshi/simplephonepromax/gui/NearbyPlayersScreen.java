package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class NearbyPlayersScreen extends Screen {
   private final List<NearbyPlayersScreen.PlayerRow> players = new ArrayList<>();
   private final List<Button> texturedButtons = new ArrayList<>();
   private double scrollAmount;
   private String searchText = "";
   private EditBox searchField;
   private int filterMode;
   private final boolean addToCallMode;

   public NearbyPlayersScreen() {
      this(false);
   }

   public NearbyPlayersScreen(boolean addToCallMode) {
      super(Component.translatable("phone.nearby.title"));
      this.addToCallMode = addToCallMode;
   }

   protected void init() {
      this.players.clear();
      if (this.minecraft != null && this.minecraft.getConnection() != null) {
         UUID own = this.minecraft.player != null ? this.minecraft.player.getUUID() : null;

         for (PlayerInfo entry : this.minecraft.getConnection().getOnlinePlayers()) {
            UUID uuid = entry.getProfile().id();
            String name = entry.getProfile().name();
            if (uuid != null && name != null && !uuid.equals(own)) {
               this.players.add(new NearbyPlayersScreen.PlayerRow(name, uuid));
            }
         }
      }

      dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService.refreshPublicDirectoryAsync(this.minecraft);
      this.players.sort(Comparator.comparing(NearbyPlayersScreen.PlayerRow::name, String.CASE_INSENSITIVE_ORDER));
      this.rebuildButtons();
   }

   public void refreshDirectoryView() { if (this.minecraft != null && dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this)) this.rebuildButtons(); }

   private void addTexturedButton(Button button) {
      this.texturedButtons.add(button);
      this.addRenderableWidget(button);
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

   private void rebuildButtons() {
      this.clearWidgets();
      this.texturedButtons.clear();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int searchX = listX(frame);
      int searchY = frame.y() + frame.px(22);
      int filterW=Math.max(36,frame.px(44)),searchW=listW(frame)-filterW-frame.px(2);
      int searchH = Math.max(12, frame.px(14));
      this.searchField = new EditBox(this.font, searchX, searchY, searchW, searchH, Component.translatable("phone.search"));
      this.searchField.setMaxLength(80);
      this.searchField.setValue(this.searchText);
      this.searchField.setHint(Component.translatable("phone.search.nearby"));
      this.searchField.setResponder(value -> {
         this.searchText = value != null ? value : "";
         this.clampScroll();
      });
      this.addRenderableWidget(this.searchField);
      String filterLabel=switch(this.filterMode){case 1->"Конт.";case 2->"Вызов";default->"Все";};
      String filterHint=switch(this.filterMode){case 1->"Фильтр: только контакты";case 2->"Фильтр: игроки в звонке";default->"Фильтр: все игроки";};
      this.addTexturedButton(Button.builder(Component.literal(filterLabel),b->{this.filterMode=(this.filterMode+1)%3;this.scrollAmount=0;this.rebuildButtons();})
         .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(filterHint)))
         .size(filterW,searchH).pos(searchX+searchW+frame.px(2),searchY).build());
      int backW = Math.max(68, frame.px(88));
      int backH = Math.max(12, frame.px(14));
      int backY = Math.min(this.height - backH - 4, frame.bottom() + Math.max(3, frame.px(3)));
      this.addTexturedButton(Button.builder(Component.translatable("phone.settings.back"), button -> {
         if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.addToCallMode ? new PhoneMainScreen(true) : new PhoneMainScreen());
         }
      }).size(backW, backH).pos(frame.centerX() - backW / 2, backY).build());
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int rowH = rowHeight(frame);
      int max = Math.max(0, this.visiblePlayers().size() * rowH - Math.max(rowH, listBottom(frame) - listY(frame)));
      this.scrollAmount = Math.max(0.0, Math.min(max, this.scrollAmount - verticalAmount * rowH));
      return true;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.handleRowClick(mouseX, mouseY);
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
      return super.mouseClicked(click, doubleClick) || this.handleRowClick(click.x(), click.y());
   }

   private boolean handleRowClick(double mouseX, double mouseY) {
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int rowX = listX(frame);
      int rowY = listY(frame);
      int rowW = listW(frame);
      int rowH = rowHeight(frame);
      int visibleBottom = listBottom(frame);
      int actionW = Math.max(18, frame.px(18));
      int actionGap = Math.max(2, frame.px(2));
      int blockX = rowX + rowW - frame.px(4) - actionW;
      int addX = blockX - actionGap - actionW;
      if (mouseY < rowY || mouseY >= visibleBottom) { return false; }
      int yOff = (int)(-this.scrollAmount);
      List<NearbyPlayersScreen.PlayerRow> visiblePlayers = this.visiblePlayers();
      for (int i = 0; i < visiblePlayers.size(); i++) {
         int y = rowY + i * rowH + yOff;
         if (y + rowH < rowY) {
            continue;
         }

         if (y > visibleBottom) {
            break;
         }

         if (mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= y && mouseY <= y + rowH) {
            NearbyPlayersScreen.PlayerRow row = visiblePlayers.get(i);
            if (mouseX >= blockX && mouseX <= blockX + actionW) {
               this.toggleBlocked(row);
            } else if (this.addToCallMode) {
               this.invitePlayer(row);
            } else if (mouseX >= addX && mouseX <= addX + actionW) {
               this.addPlayer(row);
            } else {
               if (this.canCallFromDirectory(row)) {
                  this.minecraft.setScreenAndShow(new CallCheckScreen(row.uuid(), row.name()));
               } else {
                  toast(Component.literal("Номер скрыт. Добавьте его в контакты или наберите вручную."));
               }
            }
            return true;
         }
      }

      return false;
   }

   private boolean canCallFromDirectory(NearbyPlayersScreen.PlayerRow row) {
      return row != null && PhoneNumberManager.isValidKnownNumber(PhoneNumberManager.getVisibleNumberFor(row.uuid()));
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

   private int rowHeight(PhoneGuiTextures.Frame frame){return Math.max(22,frame.px(23));}

   private int listBottom(PhoneGuiTextures.Frame frame) {
      return frame.y() + frame.px(124);
   }

   private void clampScroll() {
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int rowH = rowHeight(frame);
      int max = Math.max(0, this.visiblePlayers().size() * rowH - Math.max(rowH, listBottom(frame) - listY(frame)));
      this.scrollAmount = Math.max(0.0, Math.min(max, this.scrollAmount));
   }

   private List<NearbyPlayersScreen.PlayerRow> visiblePlayers() {
      String query = this.searchText == null ? "" : this.searchText.trim().toLowerCase();
      String digits = PhoneNumberManager.onlyDigits(query);
      List<NearbyPlayersScreen.PlayerRow> out = new ArrayList<>();
      for (NearbyPlayersScreen.PlayerRow row : this.players) {
         boolean added=SimpleVoiceCallClient.config.hasContact(PhoneNumberManager.getVisibleNumberFor(row.uuid()));
         boolean inCall=ModNetworking.isPlayerInPhoneCall(row.uuid());
         if(this.filterMode==1&&!added||this.filterMode==2&&!inCall)continue;
         String number = PhoneNumberManager.getVisibleNumberFor(row.uuid());
         String haystack = (row.name() + " " + PhoneNumberManager.formatNumber(number) + " " + number).toLowerCase();
         if (query.isEmpty()||haystack.contains(query) || (!digits.isEmpty() && PhoneNumberManager.onlyDigits(number).contains(digits))) {
            out.add(row);
         }
      }

      return out;
   }

   private void addPlayer(NearbyPlayersScreen.PlayerRow row) {
      if (row == null || SimpleVoiceCallClient.config.hasContact(PhoneNumberManager.getVisibleNumberFor(row.uuid()))) {
         return;
      }

      String number = PhoneNumberManager.getVisibleNumberFor(row.uuid());
      if (this.minecraft != null) this.minecraft.setScreenAndShow(new PhoneNickEditScreen(this, row.uuid(), number, row.name(), ""));
   }

   private void invitePlayer(NearbyPlayersScreen.PlayerRow row) {
      if (row == null) {
         return;
      }

      if (!SimpleVoiceCallClient.callManager.isInActiveCall()) {
         toast(Component.translatable("phone.add_call.no_active_call"));
         if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new PhoneMainScreen());
         }
      } else if (ModNetworking.isPlayerInActiveCallGroup(row.uuid())) {
         toast(Component.translatable("phone.add_call.already_here", row.name()));
         if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new ActiveCallScreen());
         }
      } else if (ModNetworking.inviteToActiveCall(row.uuid(), row.name())) {
         toast(Component.translatable("phone.add_call.invite_sent", row.name()));
         if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new ActiveCallScreen());
         }
      } else {
         toast(Component.translatable("phone.add_call.unavailable", row.name()));
      }
   }

   private void toggleBlocked(NearbyPlayersScreen.PlayerRow row) {
      if (row == null) {
         return;
      }

      if (SimpleVoiceCallClient.config.isBlocked(row.uuid())) {
         SimpleVoiceCallClient.config.removeBlocked(row.uuid().toString());
      } else {
         SimpleVoiceCallClient.config.addBlocked(row.uuid(), row.name());
      }

      this.rebuildButtons();
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      WallpaperEngine.render(context, this.width, this.height);
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      PhoneGuiTextures.drawUniversalBackground(context, frame, mouseX, mouseY);
      PhoneGuiTextures.drawCenteredTrimmedText(context, this.font, this.title, frame.centerX(), frame.y() + frame.px(7), frame.px(150), -1);

      int rowX = listX(frame);
      int rowY = listY(frame);
      int rowW = listW(frame);
      int rowH = rowHeight(frame);
      int visibleBottom = listBottom(frame);
      int yOff = (int)(-this.scrollAmount);
      try {
         context.enableScissor(rowX, rowY, rowX + rowW, visibleBottom);
         int lastBottom = rowY;
         List<NearbyPlayersScreen.PlayerRow> visiblePlayers = this.visiblePlayers();
         if(visiblePlayers.isEmpty()) {
            String empty=this.searchText.isBlank()?"Подходящих игроков нет":"Ничего не найдено";
            PhoneGuiTextures.drawCenteredTrimmedText(context,this.font,Component.literal(empty),frame.centerX(),rowY+frame.px(34),rowW-8,0xFFBFC6D0);
         }
         for (int i = 0; i < visiblePlayers.size(); i++) {
            NearbyPlayersScreen.PlayerRow row = visiblePlayers.get(i);
            int y = rowY + i * rowH + yOff;
            if (y + rowH < rowY) {
               continue;
            }

            if (y > visibleBottom) {
               break;
            }

            boolean hovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= y && mouseY <= y + rowH;
            boolean added = SimpleVoiceCallClient.config.hasContact(PhoneNumberManager.getVisibleNumberFor(row.uuid()));
            boolean blocked = SimpleVoiceCallClient.config.isBlocked(row.uuid());
            int textY = y + frame.px(3);
            int actionW = Math.max(18, frame.px(18));
            int actionGap = Math.max(2, frame.px(2));
            int actionH = Math.min(rowH - 4, Math.max(14, frame.px(14)));
            int actionY = y + (rowH - actionH) / 2;
            int blockX = rowX + rowW - frame.px(4) - actionW;
            int addX = blockX - actionGap - actionW;
            boolean inPhoneCall = ModNetworking.isPlayerInPhoneCall(row.uuid());
            boolean canCallFromDirectory = this.addToCallMode || this.canCallFromDirectory(row);
            // Contact membership is shown in the subtitle/check action; it should not tint the whole row.
            PhoneGuiTextures.drawTabItem(context, frame, rowX, y, rowW, rowH, hovered);
            PhoneGuiTextures.drawOnlineIcon(context, rowX + frame.px(4), textY, true);
            String number = PhoneNumberManager.getVisibleNumberFor(row.uuid());
            String text = (inPhoneCall ? "☎ " : "") + row.name();
            PhoneGuiTextures.drawTrimmedText(
               context,
               this.font,
               text,
               rowX + frame.px(17),
               textY,
               Math.max(20, addX - rowX - frame.px(20)),
               inPhoneCall ? 0xFF88FF88 : canCallFromDirectory ? -1 : 0xFF9A9A9A,
               true
            );
            String numberLabel=number==null?"номер скрыт · только набор/контакт":PhoneNumberManager.formatNumber(number);
            String subtitle=numberLabel+(added?" · контакт":" · рядом")+(inPhoneCall?" · разговор":"");
            PhoneGuiTextures.drawTrimmedText(context,this.font,subtitle,rowX+frame.px(17),y+frame.px(13),Math.max(20,addX-rowX-frame.px(20)),0xFFBFC6D0,false);
            PhoneGuiTextures.drawButtonRect(
               context,
               this.font,
               Component.literal(added ? "✓" : "+"),
               addX,
               actionY,
               actionW,
               actionH,
               mouseX >= addX && mouseX <= addX + actionW && mouseY >= actionY && mouseY <= actionY + actionH,
               !added
            );
            PhoneGuiTextures.drawButtonRect(
               context,
               this.font,
               Component.literal(blocked ? "✓" : "×"),
               blockX,
               actionY,
               actionW,
               actionH,
               mouseX >= blockX && mouseX <= blockX + actionW && mouseY >= actionY && mouseY <= actionY + actionH,
               true
            );
            lastBottom = Math.max(lastBottom, y + rowH);
         }
         context.disableScissor();
      } catch (Throwable ignored) {
         try {
            context.disableScissor();
         } catch (Throwable ignored2) {
         }
      }

      super.extractRenderState(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
      for (Button button : this.texturedButtons) {
         PhoneGuiTextures.drawButton(context, this.font, button, mouseX, mouseY);
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

   private record PlayerRow(String name, UUID uuid) {
   }
}



