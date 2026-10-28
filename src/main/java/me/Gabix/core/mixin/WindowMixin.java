package me.Gabix.core.mixin;

import me.Gabix.core.InactiveResolutionScale;
import me.Gabix.core.WindowAccess;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowAccess {
    @Shadow private int framebufferWidth;
    @Shadow private int framebufferHeight;
    @Shadow private double scaleFactor;

    @Override
    public int mvp$getRealFramebufferWidth() {
        return this.framebufferWidth;
    }

    @Override
    public int mvp$getRealFramebufferHeight() {
        return this.framebufferHeight;
    }

    @Override
    public double mvp$getRealScaleFactor() {
        return this.scaleFactor;
    }

    @Inject(method = "getFramebufferWidth", at = @At("RETURN"), cancellable = true)
    private void mvp$getFramebufferWidth(CallbackInfoReturnable<Integer> cir) {
        if (InactiveResolutionScale.isActive()) {
            cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() * InactiveResolutionScale.getFactor())));
        }
    }

    @Inject(method = "getFramebufferHeight", at = @At("RETURN"), cancellable = true)
    private void mvp$getFramebufferHeight(CallbackInfoReturnable<Integer> cir) {
        if (InactiveResolutionScale.isActive()) {
            cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() * InactiveResolutionScale.getFactor())));
        }
    }

    @Inject(method = "getScaleFactor", at = @At("RETURN"), cancellable = true)
    private void mvp$getScaleFactor(CallbackInfoReturnable<Double> cir) {
        if (InactiveResolutionScale.isActive()) {
            cir.setReturnValue(cir.getReturnValue() * InactiveResolutionScale.getFactor());
        }
    }
}
