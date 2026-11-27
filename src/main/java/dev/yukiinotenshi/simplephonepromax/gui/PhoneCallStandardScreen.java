package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Chooses which number directory the dial pad resolves; voice calls always use Fox Mobile. */
public final class PhoneCallStandardScreen extends Screen {
   private final Screen parent;
   public PhoneCallStandardScreen(Screen parent) { super(Component.translatable("phone.standard.title")); this.parent = parent; }
   private boolean compact() { return height < 175; }
   private int buttonHeight() { return compact() ? 14 : 20; }
   private int backY() { return height - buttonHeight() - 4; }
   private int top() { return compact() ? 29 : Math.max(32, (height - 174) / 2); }
   private int contentWidth() { return Math.min(420, width - 24); }

   @Override protected void init() {
      int w = contentWidth(), x = (width - w) / 2, y = top(), h = buttonHeight();
      int leftW = compact() ? (w - 6) / 2 : w;
      int legacyX = compact() ? x + leftW + 6 : x;
      int legacyY = compact() ? y : y + 49;
      addRenderableWidget(Button.builder(option("fox"), b -> select("fox")).bounds(x, y, leftW, h).build());
      addRenderableWidget(Button.builder(option("legacy"), b -> select("legacy"))
         .bounds(legacyX, legacyY, compact() ? w - leftW - 6 : w, h).build());
      addRenderableWidget(Button.builder(Component.translatable("phone.settings.back"), b -> minecraft.setScreenAndShow(parent))
         .bounds(x, backY(), w, h).build());
   }

   private Component option(String mode) {
      boolean selected = mode.equals("legacy") == CallStandard.isLegacy();
      return Component.literal((selected ? "✓ " : "") + Component.translatable("phone.standard." + mode + "_label").getString());
   }

   private void select(String mode) {
      if (!CallStandard.select(mode) && !CallStandard.canSwitch()) {
         dev.yukiinotenshi.simplephonepromax.network.CallControlService.message("phone.standard.busy");
      }
      minecraft.setScreenAndShow(new PhoneCallStandardScreen(parent));
   }

   @Override public void extractRenderState(GuiGraphicsExtractor c, int mouseX, int mouseY, float delta) {
      PhoneGuiTextures.fullBackground(c, width, height, mouseX, mouseY);
      int y = top(), w = contentWidth(), x = (width - w) / 2;
      PhoneGuiTextures.drawCenteredTrimmedText(c, font, title, width / 2, y - 25, w, 0xFFFFFFFF);
      PhoneGuiTextures.drawCenteredTrimmedText(c, font,
         Component.translatable("phone.standard.active", CallStandard.label(CallStandard.isLegacy() ? "legacy" : "fox")),
         width / 2, y - 13, w, 0xFFB8D6EE);
      int statusY = Math.min(backY() - 2 * font.lineHeight - 4, compact() ? top() + 68 : top() + 105);
      int leftW = compact() ? (w - 6) / 2 : w;
      int descY = y + buttonHeight() + 3;
      int legacyTextY = compact() ? descY : y + 72;
      PhoneGuiTextures.drawWrappedLines(c, font, Component.translatable("phone.standard.fox_info"), x, descY, leftW,
         Math.max(0, ((compact() ? statusY : y + 49) - descY) / font.lineHeight), 0xFFFFFFFF);
      PhoneGuiTextures.drawWrappedLines(c, font, Component.translatable("phone.standard.legacy_info"),
         compact() ? x + leftW + 6 : x, legacyTextY, compact() ? w - leftW - 6 : w,
         Math.max(0, (statusY - legacyTextY) / font.lineHeight), 0xFFFFFFFF);
      String status = CallStandard.canSwitch() ? "phone.standard.applied_info" : "phone.standard.busy";
      PhoneGuiTextures.drawWrappedLines(c, font, Component.translatable(status), x, statusY, w, 2, 0xFFFFD07A);
      super.extractRenderState(c, mouseX, mouseY, delta);
      PhoneGuiTextures.widgets(this, c, mouseX, mouseY);
   }

   @Override public void onClose() { minecraft.setScreenAndShow(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
