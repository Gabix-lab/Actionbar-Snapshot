package me.Gabix.renderscale.mixin;

import me.Gabix.renderscale.RenderScale;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostEffectProcessor.class)
public abstract class MixinPostChain {

    @Shadow @Final private String name;
    @Shadow private Matrix4f orthoMatrix;

    @Unique
    private double renderScale$invScale() {
        if (name == null || !name.contains("entity_outline")) return 1.0;
        return 1.0 / RenderScale.getInstance().getRenderScaleFactor();
    }

    @Redirect(
            method = "setupDimensions",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;resize(IIZ)V"),
            require = 0
    )
    private void renderScale$resize(Framebuffer instance, int width, int height, boolean clearError) {
        double inv = renderScale$invScale();
        instance.resize((int) (width / inv), (int) (height / inv), clearError);
    }

    @Inject(method = "setupDimensions", at = @At("TAIL"), require = 0)
    private void renderScale$scaleOrtho(int width, int height, CallbackInfo ci) {
        double inv = renderScale$invScale();
        if (inv != 1.0 && this.orthoMatrix != null) {
            this.orthoMatrix.scale((float) inv, (float) inv, 1.0f);
        }
    }
}