package dev.yukiinotenshi.simplephonepromax.mixin;

import dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenTrackerMixin {
   @Inject(method = "setScreenAndShow", at = @At("TAIL"))
   private void foxMobile$trackScreen(Screen screen, CallbackInfo ci) {
      ScreenTracker.setCurrent(screen);
   }
}
