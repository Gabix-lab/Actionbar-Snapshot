package me.Gabix.fireoverlay.mixin;

import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class FireOverlayMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFire(CallbackInfo ci) {
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config != null && config.hideFireOverlay) {
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "renderFireOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"
            ),
            index = 1
    )
    private static float modifyFireHeight(float originalY) {
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config != null) {
            float heightFraction = config.fireOverlayHeight / 100.0f;
            return -1.0f + heightFraction;
        }
        return originalY;
    }
}