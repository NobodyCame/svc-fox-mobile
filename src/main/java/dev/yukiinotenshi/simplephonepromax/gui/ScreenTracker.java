package dev.yukiinotenshi.simplephonepromax.gui;

import net.minecraft.client.gui.screens.Screen;

/** Tracks the active client screen for asynchronous UI updates. */
public final class ScreenTracker {
   private static volatile Screen current;

   private ScreenTracker() {}

   public static Screen current() {
      return current;
   }

   public static boolean isCurrent(Screen screen) {
      return current == screen;
   }

   public static void setCurrent(Screen screen) {
      current = screen;
   }
}
