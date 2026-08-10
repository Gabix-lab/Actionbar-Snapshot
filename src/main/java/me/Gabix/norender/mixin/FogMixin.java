package me.Gabix.norender.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.Gabix.norender.NoRenderEngine;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class FogMixin {

    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void hideFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (NoRenderEngine.enabled && fogType == BackgroundRenderer.FogType.FOG_TERRAIN) {
            RenderSystem.setShaderFogStart(200000.0F);
            RenderSystem.setShaderFogEnd(200000.0F);
            ci.cancel();
        }
    }
}