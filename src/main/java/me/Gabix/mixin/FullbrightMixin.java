package me.Gabix.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public class FullbrightMixin {

    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void getBrightness(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1.0f);
    }
}