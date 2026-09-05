package me.Gabix.mixin;

import me.Gabix.ActionbarStorageClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) {
            if (button == 0) {
                ActionbarStorageClient.getInstance().getFpsCpsCounter().registerLeftClick();
            } else if (button == 1) {
                ActionbarStorageClient.getInstance().getFpsCpsCounter().registerRightClick();
            }
        }
    }
}