package dev.yukiinotenshi.simplephonepromax.mixin;

import dev.yukiinotenshi.simplephonepromax.phone.PhoneClientActions;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDetector;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
   @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
   private void interactItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      ItemStack stack = player.getStackInHand(hand);
      if (PhoneDetector.isPhone(stack)) {
         PhoneClientActions.openPhone(MinecraftClient.getInstance(), true);
         cir.setReturnValue(ActionResult.SUCCESS);
      }
   }
}



