package me.Gabix.renderscale.mixin;

import me.Gabix.renderscale.RenderScale;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "close", at = @At("HEAD"))
    private void renderScale$closeLegacyShaders(CallbackInfo ci) {
        if (RenderScale.getInstance() != null) RenderScale.getInstance().close();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderScale$updateDynamicScale(CallbackInfo ci) {
        if (RenderScale.getInstance() != null) RenderScale.getInstance().updateDynamicScale();
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void takeOver(CallbackInfo ci) {
        RenderScale.getInstance().setShouldScale(true);
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void handBack(CallbackInfo ci) {
        RenderScale.getInstance().setShouldScale(false);
    }
}