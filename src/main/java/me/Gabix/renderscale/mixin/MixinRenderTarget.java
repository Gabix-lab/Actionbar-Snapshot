package me.Gabix.renderscale.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import me.Gabix.renderscale.RenderScale;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Framebuffer.class, priority = 99999)
public abstract class MixinRenderTarget {
    @Redirect(
            method = "setTexFilter",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texParameter(III)V")
    )
    private void onSetTexFilter(int target, int pname, int param) {
        if ((Object) this == RenderScale.renderTarget) {
            GlStateManager._texParameter(target, pname,
                    RenderScale.getConfig().getFilter() ? 9729 : 9728);
        } else {
            GlStateManager._texParameter(target, pname, param);
        }
    }
}