package me.Gabix.norender.mixin;

import me.Gabix.norender.NoRenderEngine;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseInputMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseClick(long window, int button, int action, int mods, CallbackInfo ci) {
        NoRenderEngine.onUserInput();
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        NoRenderEngine.onUserInput();
    }
}