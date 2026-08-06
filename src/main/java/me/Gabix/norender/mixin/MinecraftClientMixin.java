package me.Gabix.norender.mixin;

import me.Gabix.norender.NoRenderManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "onWindowFocusChanged", at = @At("HEAD"))
    private void onWindowFocusChanged(boolean focused, CallbackInfo ci) {
        if (focused) {
            NoRenderManager.onWindowFocusGained();
        }
    }
}