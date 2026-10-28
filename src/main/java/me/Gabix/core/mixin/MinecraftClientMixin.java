package me.Gabix.core.mixin;

import me.Gabix.core.InactiveResolutionScale;
import me.Gabix.core.WindowAccess;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void mvp$updateInactiveResolution(boolean tick, CallbackInfo ci) {
        InactiveResolutionScale.update(MinecraftClient.getInstance());
    }

    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"),
            index = 0
    )
    private int mvp$drawWidth(int width) {
        if (!InactiveResolutionScale.isActive()) return width;
        return ((WindowAccess) (Object) MinecraftClient.getInstance().getWindow()).mvp$getRealFramebufferWidth();
    }

    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"),
            index = 1
    )
    private int mvp$drawHeight(int height) {
        if (!InactiveResolutionScale.isActive()) return height;
        return ((WindowAccess) (Object) MinecraftClient.getInstance().getWindow()).mvp$getRealFramebufferHeight();
    }
}
