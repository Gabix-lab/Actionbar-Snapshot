package me.Gabix.norender.mixin;

import me.Gabix.norender.FpsReducerLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(boolean tick, CallbackInfo ci) {
        if (FpsReducerLogic.shouldSkipFrame()) {
            ci.cancel();
        }
    }
}