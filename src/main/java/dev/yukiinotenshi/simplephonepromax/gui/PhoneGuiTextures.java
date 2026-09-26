package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class PhoneGuiTextures {
   static final int BASE_WIDTH = 195;
   static final int BASE_HEIGHT = 136;

   private static final int SOURCE_WIDTH = 256;
   private static final int SOURCE_HEIGHT = 256;
   private static final Identifier MAIN_PHONE = id("textures/gui/phone_pro_max/main_phone.png");
   private static final Identifier UNIVERSAL_BACKGROUND = id("textures/gui/phone_pro_max/universal_gui_background.png");
   private static final Identifier CONTACT_LIST = id("textures/gui/phone_pro_max/contact_list.png");
   private static final Identifier TAB_LIST = id("textures/gui/phone_pro_max/tab_list.png");
   private static final Identifier CONTACT_ITEM = id("textures/gui/phone_pro_max/contact_item.png");
   private static final Identifier CONTACT_ITEM_HIGHLIGHTED = id("textures/gui/phone_pro_max/contact_item_highlited.png");
   private static final Identifier TAB_ITEM = id("textures/gui/phone_pro_max/tab_item.png");
   private static final Identifier TAB_ITEM_HIGHLIGHTED = id("textures/gui/phone_pro_max/tab_item_highlited.png");
   private static final Identifier BUTTON = id("textures/gui/phone_pro_max/button.png");
   private static final Identifier BUTTON_HIGHLIGHTED = id("textures/gui/phone_pro_max/button_highlighted.png");
   private static final Identifier SMALL_BUTTON = id("textures/gui/phone_pro_max/icons/button.png");
   private static final Identifier SMALL_BUTTON_HIGHLIGHTED = id("textures/gui/phone_pro_max/icons/button_highlighted.png");
   private static final Identifier POPUP_BACKGROUND = id("textures/gui/phone_pro_max/incoming_call_popup/background.png");
   private static final Identifier POPUP_NOTES = id("textures/gui/phone_pro_max/incoming_call_popup/music_notes.png");
   private static final Identifier ACCEPT = id("textures/gui/phone_pro_max/icons/accept.png");
   private static final Identifier ACCEPT_HIGHLIGHTED = id("textures/gui/phone_pro_max/icons/accept_highlighted.png");
   private static final Identifier REJECT = id("textures/gui/phone_pro_max/icons/reject.png");
   private static final Identifier REJECT_HIGHLIGHTED = id("textures/gui/phone_pro_max/icons/reject_highlighted.png");
   private static final Identifier ONLINE = id("textures/gui/phone_pro_max/icons/online.png");
   private static final Identifier OFFLINE = id("textures/gui/phone_pro_max/icons/offline.png");
   // These are GUI-atlas sprite IDs. Minecraft reads their adjacent .mcmeta
   // files and advances the sprite animation itself.
   private static final Identifier FOX_HIGHLIGHTED = id("fox_highlighted");
   private static final Identifier FOX_SLEEPING = id("fox_sleeping");


   static void fullBackground(DrawContext context,int width,int height,int mouseX,int mouseY){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(context,width,height);context.fill(0,0,width,height,0x99000000);int size=layout(width,height).px(20);drawFoxMascot(context,width-size-16,12,size,mouseX,mouseY);}
   static void widgets(net.minecraft.client.gui.screen.Screen screen,DrawContext context,int mx,int my) {}
   private PhoneGuiTextures() {
   }

   private static Identifier id(String path) {
      return Identifier.of(SimpleVoiceCallClient.RESOURCE_ID, path);
   }

   static Frame layout(int screenWidth, int screenHeight) {
      float scale = Math.min((screenWidth - 18.0F) / BASE_WIDTH, (screenHeight - 42.0F) / BASE_HEIGHT);
      scale = Math.max(0.75F, Math.min(scale, 2.2F));
      int width = Math.round(BASE_WIDTH * scale);
      int height = Math.round(BASE_HEIGHT * scale);
      int x = (screenWidth - width) / 2;
      int y = Math.max(6, (screenHeight - height) / 2);
      return new Frame(x, y, width, height, scale);
   }

   static void drawPhone(DrawContext context, Frame frame, int mouseX, int mouseY) {
      drawPanel(context, MAIN_PHONE, frame);
      drawFoxMascot(context, frame, mouseX, mouseY);
   }

   static void drawUniversalBackground(DrawContext context, Frame frame, int mouseX, int mouseY) {
      drawPanel(context, UNIVERSAL_BACKGROUND, frame);
      drawFoxMascot(context, frame, mouseX, mouseY);
   }

   static void drawContactList(DrawContext context, Frame frame, int mouseX, int mouseY) {
      drawPanel(context, CONTACT_LIST, frame);
      drawFoxMascot(context, frame, mouseX, mouseY);
   }

   static void drawTabList(DrawContext context, Frame frame, int mouseX, int mouseY) {
      drawPanel(context, TAB_LIST, frame);
      drawFoxMascot(context, frame, mouseX, mouseY);
   }

   private static void drawFoxMascot(DrawContext context, Frame frame, int mouseX, int mouseY) {
      int size = frame.px(20);
      int x = frame.x() + frame.px(158);
      // Large GUI scales can place the panel near the top edge; keep the full
      // sprite inside the drawable screen instead of clipping its upper rows.
      int y = Math.max(0, frame.y() - frame.px(18));
      drawFoxMascot(context, x, y, size, mouseX, mouseY);
   }

   private static void drawFoxMascot(DrawContext context, int x, int y, int size, int mouseX, int mouseY) {
      boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
      Identifier texture = hovered ? FOX_HIGHLIGHTED : FOX_SLEEPING;
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, size, size);
   }

   private static void drawPanel(DrawContext context, Identifier texture, Frame frame) {
      context.drawTexture(
         RenderPipelines.GUI_TEXTURED,
         texture,
         frame.x(),
         frame.y(),
         0.0F,
         0.0F,
         frame.width(),
         frame.height(),
         BASE_WIDTH,
         BASE_HEIGHT,
         SOURCE_WIDTH,
         SOURCE_HEIGHT
      );
   }

   static void drawScreenWallpaper(DrawContext context, Frame frame) {
   }

   static void drawScreenShade(DrawContext context, Frame frame, int alpha) {
   }

   static void drawCenteredTrimmedText(DrawContext context, TextRenderer textRenderer, Text text, int centerX, int y, int maxWidth, int color) {
      String trimmed = textRenderer.trimToWidth(text.getString(), maxWidth);
      drawText(context, textRenderer, trimmed, centerX - textRenderer.getWidth(trimmed) / 2, y, color, true);
   }

   static void drawTrimmedText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int maxWidth, int color, boolean shadow) {
      drawText(context, textRenderer, textRenderer.trimToWidth(text, maxWidth), x, y, color, shadow);
   }

   static void drawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
      context.createNewRootLayer();
      context.drawText(textRenderer, text, x, y, opaque(color), shadow);
   }

   static int opaque(int color) {
      return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
   }

   static void drawButton(DrawContext context, TextRenderer textRenderer, ButtonWidget button, int mouseX, int mouseY) {
      // ButtonWidget already renders the correct Minecraft-style button.
      // The helper stays as a no-op so old screen code can keep one path.
   }

   static void drawButtonRect(
      DrawContext context, TextRenderer textRenderer, Text label, int x, int y, int width, int height, boolean hovered, boolean active
   ) {
      int fill = !active ? 0xFF555555 : hovered ? 0xFF9A9A9A : 0xFF777777;
      context.fill(x, y, x + width, y + height, 0xFF000000);
      context.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
      context.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFFFFFFF);
      context.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFFFFFFFF);
      context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF333333);
      context.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF333333);
      String text = textRenderer.trimToWidth(label.getString(), Math.max(4, width - 6));
      int color = active ? -1 : 0xFFA0A0A0;
      drawText(context, textRenderer, text, x + (width - textRenderer.getWidth(text)) / 2, y + (height - 8) / 2, color, true);
   }

   static void drawIncomingPopup(DrawContext context, Frame frame, int yOffset) {
      int width = frame.px(236);
      int height = frame.px(34);
      int x = frame.centerX() - width / 2;
      int y = frame.y() + frame.px(yOffset);
      context.drawTexture(RenderPipelines.GUI_TEXTURED, POPUP_BACKGROUND, x, y, 0.0F, 0.0F, width, height, 236, 34, 236, 34);
   }

   static void drawIncomingToast(DrawContext context, TextRenderer textRenderer, String callerName, long ringingStartedAtMs) {drawToast(context,textRenderer,"Входящий вызов",callerName,ringingStartedAtMs);}
   static void drawToast(DrawContext context,TextRenderer textRenderer,String heading,String callerName,long ringingStartedAtMs) {
      int screenW = context.getScaledWindowWidth();
      int popupW = 236;
      int popupH = 34;
      long now = System.currentTimeMillis();
      long elapsed = ringingStartedAtMs > 0L ? Math.max(0L, now - ringingStartedAtMs) : 0L;
      float t = Math.min(1.0F, elapsed / 420.0F);
      float overshoot = t < 0.72F ? t / 0.72F * 1.08F : 1.08F - (t - 0.72F) / 0.28F * 0.08F;
      int x = screenW - 8 - Math.round(popupW * overshoot);
      int y = 10;
      context.createNewRootLayer();
      context.drawTexture(RenderPipelines.GUI_TEXTURED, POPUP_BACKGROUND, x, y, 0.0F, 0.0F, popupW, popupH, popupW, popupH, popupW, popupH);
      int frame = (int)((now / 220L) % 4L);
      int notesX = x + popupW - 31;
      int notesY = y + 7;
      drawPixelNote(context, notesX, notesY + (frame == 1 ? 1 : 0), 1, 0xFFFFFF80);
      drawPixelNote(context, notesX + 10, notesY + 9 + (frame == 3 ? -1 : 0), 1, 0xFFE8E8E8);

      drawTrimmedText(context, textRenderer, heading, x + 14, y + 6, popupW - 60, 0xFFFFE070, true);
      drawTrimmedText(context, textRenderer, callerName != null ? callerName : "", x + 14, y + 18, popupW - 60, -1, true);
   }

   private static void drawPixelNote(DrawContext context, int x, int y, int scale, int color) {
      int s = Math.max(1, scale);
      int shadow = 0xAA000000;
      drawPixelNoteShape(context, x + s, y + s, s, shadow);
      drawPixelNoteShape(context, x, y, s, opaque(color));
   }

   private static void drawPixelNoteShape(DrawContext context, int x, int y, int s, int color) {
      context.fill(x + 5 * s, y, x + 7 * s, y + 9 * s, color);
      context.fill(x + 7 * s, y, x + 11 * s, y + 2 * s, color);
      context.fill(x + 9 * s, y + 2 * s, x + 12 * s, y + 4 * s, color);
      context.fill(x + 1 * s, y + 7 * s, x + 6 * s, y + 11 * s, color);
      context.fill(x, y + 8 * s, x + 7 * s, y + 10 * s, color);
   }

   static void drawContactItem(DrawContext context, Frame frame, int x, int y, int width, int height, boolean highlighted) {
      context.drawTexture(
         RenderPipelines.GUI_TEXTURED,
         highlighted ? CONTACT_ITEM_HIGHLIGHTED : CONTACT_ITEM,
         x,
         y,
         0.0F,
         0.0F,
         width,
         height,
         160,
         19,
         160,
         19
      );
   }

   static void drawTabItem(DrawContext context, Frame frame, int x, int y, int width, int height, boolean highlighted) {
      context.drawTexture(
         RenderPipelines.GUI_TEXTURED,
         highlighted ? TAB_ITEM_HIGHLIGHTED : TAB_ITEM,
         x,
         y,
         0.0F,
         0.0F,
         width,
         height,
         160,
         19,
         160,
         19
      );
   }

   static void drawOnlineIcon(DrawContext context, int x, int y, boolean online) {
      context.drawTexture(RenderPipelines.GUI_TEXTURED, online ? ONLINE : OFFLINE, x, y, 0.0F, 0.0F, 10, 8, 10, 8);
   }

   static final class Frame {
      private final int x;
      private final int y;
      private final int width;
      private final int height;
      private final float scale;

      Frame(int x, int y, int width, int height, float scale) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.scale = scale;
      }

      int x() {
         return this.x;
      }

      int y() {
         return this.y;
      }

      int width() {
         return this.width;
      }

      int height() {
         return this.height;
      }

      float scale() {
         return this.scale;
      }

      int centerX() {
         return this.x + this.width / 2;
      }

      int bottom() {
         return this.y + this.height;
      }

      int px(int base) {
         return Math.round(base * this.scale);
      }

      int screenX() {
         return this.x + this.px(7);
      }

      int screenY() {
         return this.y + this.px(18);
      }

      int screenWidth() {
         return this.px(181);
      }

      int screenHeight() {
         return this.px(111);
      }

      int screenRight() {
         return this.screenX() + this.screenWidth();
      }

      int screenBottom() {
         return this.screenY() + this.screenHeight();
      }

      int contentX() {
         return this.x + this.px(9);
      }

      int contentY() {
         return this.y + this.px(20);
      }

      int contentWidth() {
         return this.px(177);
      }

      int contentHeight() {
         return this.px(106);
      }

      int displayX() {
         return this.x + this.px(18);
      }

      int displayY() {
         return this.y + this.px(31);
      }

      int displayWidth() {
         return this.px(89);
      }

      int displayHeight() {
         return this.px(12);
      }

      int controlX() {
         return this.x + this.px(108);
      }

      int controlY() {
         return this.y + this.px(31);
      }

      int controlWidth() {
         return this.px(70);
      }

      int controlHeight() {
         return this.px(96);
      }
   }
}

