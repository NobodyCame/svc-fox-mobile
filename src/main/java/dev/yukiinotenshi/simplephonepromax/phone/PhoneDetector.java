package dev.yukiinotenshi.simplephonepromax.phone;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class PhoneDetector {
   public void tick(MinecraftClient client) {
   }

   public static boolean isPhone(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (stack.getItem() != Items.IRON_INGOT) {
         return false;
      } else {
         Text currentName = stack.getName();
         Text defaultName = stack.getItem().getName(stack);
         if (currentName != null && !currentName.equals(defaultName)) {
            String name = stack.getName().getString();
            return name.contains("Телефон") || name.contains("Phone") || name.contains("电话") || name.contains("Telefon") || name.contains("電話");
         } else {
            return false;
         }
      }
   }

   public static boolean hasPhone(MinecraftClient client) {
      if (client == null || client.player == null) {
         return false;
      }

      try {
         for (int i = 0; i < client.player.getInventory().size(); i++) {
            if (isPhone(client.player.getInventory().getStack(i))) {
               return true;
            }
         }
      } catch (Throwable ignored) {
      }

      try {
         return isPhone(client.player.getMainHandStack()) || isPhone(client.player.getOffHandStack());
      } catch (Throwable ignored) {
         return false;
      }
   }
}



