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
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;

public class NearbyPlayersScreen extends Screen {
   private final List<NearbyPlayersScreen.PlayerRow> players = new ArrayList<>();
   private final List<ButtonWidget> texturedButtons = new ArrayList<>();
   private double scrollAmount;
   private String searchText = "";
   private TextFieldWidget searchField;
   private int filterMode;
   private final boolean addToCallMode;

   public NearbyPlayersScreen() {
      this(false);
   }

   public NearbyPlayersScreen(boolean addToCallMode) {
      super(Text.translatable("phone.nearby.title"));
      this.addToCallMode = addToCallMode;
   }

   protected void init() {
      this.players.clear();
      if (this.client != null && this.client.getNetworkHandler() != null) {
         UUID own = this.client.player != null ? this.client.player.getUuid() : null;

         for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            UUID uuid = entry.getProfile().id();
            String name = entry.getProfile().name();
            if (uuid != null && name != null && !uuid.equals(own)) {
               this.players.add(new NearbyPlayersScreen.PlayerRow(name, uuid));
            }
         }
      }

      this.players.sort(Comparator.comparing(NearbyPlayersScreen.PlayerRow::name, String.CASE_INSENSITIVE_ORDER));
      this.rebuildButtons();
   }

   private void addTexturedButton(ButtonWidget button) {
      this.texturedButtons.add(button);
      this.addDrawableChild(button);
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

   private void rebuildButtons() {
      this.clearChildren();
      this.texturedButtons.clear();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int searchX = listX(frame);
      int searchY = frame.y() + frame.px(22);
      int filterW=Math.max(36,frame.px(44)),searchW=listW(frame)-filterW-frame.px(2);
      int searchH = Math.max(12, frame.px(14));
      this.searchField = new TextFieldWidget(this.textRenderer, searchX, searchY, searchW, searchH, Text.translatable("phone.search"));
      this.searchField.setMaxLength(80);
      this.searchField.setText(this.searchText);
      this.searchField.setPlaceholder(Text.translatable("phone.search.nearby"));
      this.searchField.setChangedListener(value -> {
         this.searchText = value != null ? value : "";
         this.clampScroll();
      });
      this.addDrawableChild(this.searchField);
      String filterLabel=switch(this.filterMode){case 1->"Конт.";case 2->"Вызов";default->"Все";};
      String filterHint=switch(this.filterMode){case 1->"Фильтр: только контакты";case 2->"Фильтр: игроки в звонке";default->"Фильтр: все игроки";};
      this.addTexturedButton(ButtonWidget.builder(Text.literal(filterLabel),b->{this.filterMode=(this.filterMode+1)%3;this.scrollAmount=0;this.rebuildButtons();})
         .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(filterHint)))
         .size(filterW,searchH).position(searchX+searchW+frame.px(2),searchY).build());
      int backW = Math.max(68, frame.px(88));
      int backH = Math.max(12, frame.px(14));
      int backY = Math.min(this.height - backH - 4, frame.bottom() + Math.max(3, frame.px(3)));
      this.addTexturedButton(ButtonWidget.builder(Text.translatable("phone.settings.back"), button -> {
         if (this.client != null) {
            this.client.setScreen(this.addToCallMode ? new PhoneMainScreen(true) : new PhoneMainScreen());
         }
      }).size(backW, backH).position(frame.centerX() - backW / 2, backY).build());
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

   public boolean mouseClicked(Click click, boolean doubleClick) {
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
               this.client.setScreen(new CallCheckScreen(row.uuid(), row.name()));
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
         boolean added=SimpleVoiceCallClient.config.hasContact(row.uuid().toString());
         boolean inCall=ModNetworking.isPlayerInPhoneCall(row.uuid());
         if(this.filterMode==1&&!added||this.filterMode==2&&!inCall)continue;
         String number = PhoneNumberManager.getDisplayNumberFor(row.uuid());
         String haystack = (row.name() + " " + PhoneNumberManager.formatNumber(number) + " " + number).toLowerCase();
         if (query.isEmpty()||haystack.contains(query) || (!digits.isEmpty() && PhoneNumberManager.onlyDigits(number).contains(digits))) {
            out.add(row);
         }
      }

      return out;
   }

   private void addPlayer(NearbyPlayersScreen.PlayerRow row) {
      if (row == null || SimpleVoiceCallClient.config.hasContact(row.uuid().toString())) {
         return;
      }

      String number = PhoneNumberManager.getDisplayNumberFor(row.uuid());
      SimpleVoiceCallClient.config.addContact(new ModConfig.Contact(row.name(), number, row.uuid().toString()));
      this.rebuildButtons();
   }

   private void invitePlayer(NearbyPlayersScreen.PlayerRow row) {
      if (row == null) {
         return;
      }

      if (!SimpleVoiceCallClient.callManager.isInActiveCall()) {
         toast(Text.translatable("phone.add_call.no_active_call"));
         if (this.client != null) {
            this.client.setScreen(new PhoneMainScreen());
         }
      } else if (ModNetworking.isPlayerInActiveCallGroup(row.uuid())) {
         toast(Text.translatable("phone.add_call.already_here", row.name()));
         if (this.client != null) {
            this.client.setScreen(new ActiveCallScreen());
         }
      } else if (ModNetworking.inviteToActiveCall(row.uuid(), row.name())) {
         toast(Text.translatable("phone.add_call.invite_sent", row.name()));
         if (this.client != null) {
            this.client.setScreen(new ActiveCallScreen());
         }
      } else {
         toast(Text.translatable("phone.add_call.unavailable", row.name()));
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

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      WallpaperEngine.render(context, this.width, this.height);
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      PhoneGuiTextures.drawUniversalBackground(context, frame, mouseX, mouseY);
      PhoneGuiTextures.drawCenteredTrimmedText(context, this.textRenderer, this.title, frame.centerX(), frame.y() + frame.px(7), frame.px(150), -1);

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
            PhoneGuiTextures.drawCenteredTrimmedText(context,this.textRenderer,Text.literal(empty),frame.centerX(),rowY+frame.px(34),rowW-8,0xFFBFC6D0);
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
            boolean added = SimpleVoiceCallClient.config.hasContact(row.uuid().toString());
            boolean blocked = SimpleVoiceCallClient.config.isBlocked(row.uuid());
            int textY = y + frame.px(3);
            int actionW = Math.max(18, frame.px(18));
            int actionGap = Math.max(2, frame.px(2));
            int actionH = Math.min(rowH - 4, Math.max(14, frame.px(14)));
            int actionY = y + (rowH - actionH) / 2;
            int blockX = rowX + rowW - frame.px(4) - actionW;
            int addX = blockX - actionGap - actionW;
            boolean inPhoneCall = ModNetworking.isPlayerInPhoneCall(row.uuid());
            // Contact membership is shown in the subtitle/check action; it should not tint the whole row.
            PhoneGuiTextures.drawTabItem(context, frame, rowX, y, rowW, rowH, hovered);
            PhoneGuiTextures.drawOnlineIcon(context, rowX + frame.px(4), textY, true);
            String number = PhoneNumberManager.getDisplayNumberFor(row.uuid());
            String text = (inPhoneCall ? "☎ " : "") + row.name();
            PhoneGuiTextures.drawTrimmedText(
               context,
               this.textRenderer,
               text,
               rowX + frame.px(17),
               textY,
               Math.max(20, addX - rowX - frame.px(20)),
               inPhoneCall ? 0xFF88FF88 : -1,
               true
            );
            String subtitle=PhoneNumberManager.formatNumber(number)+(added?" · контакт":" · рядом")+(inPhoneCall?" · разговор":"");
            PhoneGuiTextures.drawTrimmedText(context,this.textRenderer,subtitle,rowX+frame.px(17),y+frame.px(13),Math.max(20,addX-rowX-frame.px(20)),0xFFBFC6D0,false);
            PhoneGuiTextures.drawButtonRect(
               context,
               this.textRenderer,
               Text.literal(added ? "✓" : "+"),
               addX,
               actionY,
               actionW,
               actionH,
               mouseX >= addX && mouseX <= addX + actionW && mouseY >= actionY && mouseY <= actionY + actionH,
               !added
            );
            PhoneGuiTextures.drawButtonRect(
               context,
               this.textRenderer,
               Text.literal(blocked ? "✓" : "×"),
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

      super.render(context,mouseX,mouseY,delta);PhoneGuiTextures.widgets(this,context,mouseX,mouseY);
      for (ButtonWidget button : this.texturedButtons) {
         PhoneGuiTextures.drawButton(context, this.textRenderer, button, mouseX, mouseY);
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

   private record PlayerRow(String name, UUID uuid) {
   }
}



