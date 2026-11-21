package dev.yukiinotenshi.simplephonepromax.mixin;

import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
   @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
   private void interactItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      ItemStack stack = player.getItemInHand(hand);
      if (PhoneDetector.isPhone(stack)) {
         PhoneClientActions.openPhone(Minecraft.getInstance(), true);
         cir.setReturnValue(InteractionResult.SUCCESS);
      }
   }
}



