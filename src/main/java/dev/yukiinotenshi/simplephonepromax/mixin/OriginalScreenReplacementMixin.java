package dev.yukiinotenshi.simplephonepromax.mixin;

import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import dev.yukiinotenshi.simplephonepromax.gui.ActiveCallScreen;
import dev.yukiinotenshi.simplephonepromax.gui.IncomingCallScreen;
import dev.yukiinotenshi.simplephonepromax.gui.PhoneMainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class OriginalScreenReplacementMixin {
   @Inject(method="setScreen",at=@At("HEAD"),cancellable=true)
   private void useProMaxInterface(Screen screen,CallbackInfo ci) {
      if(screen==null||!screen.getClass().getName().startsWith("com.yogurt278990.simplevoicecall.gui."))return;
      ci.cancel();
      if(!CallStandard.isLegacy())return;
      String name=screen.getClass().getSimpleName();
      Screen replacement=switch(name) {
         case "IncomingCallScreen" -> new IncomingCallScreen();
         case "ActiveCallScreen" -> new ActiveCallScreen();
         default -> new PhoneMainScreen();
      };
      ((MinecraftClient)(Object)this).setScreen(replacement);
   }
}

