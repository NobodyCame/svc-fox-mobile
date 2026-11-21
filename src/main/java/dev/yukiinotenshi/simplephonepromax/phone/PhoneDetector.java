package dev.yukiinotenshi.simplephonepromax.phone;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PhoneDetector {
   public void tick(Minecraft client) {
   }

   public static boolean isPhone(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (stack.getItem() != Items.IRON_INGOT) {
         return false;
      } else {
         Component currentName = stack.getHoverName();
         Component defaultName = stack.getItem().getName(stack);
         if (currentName != null && !currentName.equals(defaultName)) {
            String name = stack.getHoverName().getString();
            return name.contains("Телефон") || name.contains("Phone") || name.contains("电话") || name.contains("Telefon") || name.contains("電話");
         } else {
            return false;
         }
      }
   }

   public static boolean hasPhone(Minecraft client) {
      if (client == null || client.player == null) {
         return false;
      }

      try {
         for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            if (isPhone(client.player.getInventory().getItem(i))) {
               return true;
            }
         }
      } catch (Throwable ignored) {
      }

      try {
         return isPhone(client.player.getMainHandItem()) || isPhone(client.player.getOffhandItem());
      } catch (Throwable ignored) {
         return false;
      }
   }
}



