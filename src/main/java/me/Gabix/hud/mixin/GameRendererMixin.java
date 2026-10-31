package me.Gabix.hud.mixin;

import me.Gabix.hud.CustomHudRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    private static final MatrixStack HUD_MATRIX = new MatrixStack();

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/toast/ToastManager;draw(Lnet/minecraft/client/util/math/MatrixStack;)V",
            shift = At.Shift.AFTER))
    private void mvp$renderCustomHudOnTop(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        while (!HUD_MATRIX.isEmpty()) HUD_MATRIX.pop();
        HUD_MATRIX.push();
        try {
            CustomHudRenderer.render(HUD_MATRIX, tickDelta);
        } finally {
            while (!HUD_MATRIX.isEmpty()) HUD_MATRIX.pop();
        }
    }
}