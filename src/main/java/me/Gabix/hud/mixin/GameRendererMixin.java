package me.Gabix.hud.mixin;

import me.Gabix.hud.CustomHudRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A custom HUD-et a teljes GUI (vanilla HUD, képernyők, toastok) után rajzolja ki,
 * így az minden más elem felett jelenik meg.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/toast/ToastManager;draw(Lnet/minecraft/client/util/math/MatrixStack;)V",
            shift = At.Shift.AFTER))
    private void mvp$renderCustomHudOnTop(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        CustomHudRenderer.render(new MatrixStack(), tickDelta);
    }
}
