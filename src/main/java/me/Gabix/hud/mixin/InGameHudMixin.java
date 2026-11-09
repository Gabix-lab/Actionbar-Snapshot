package me.Gabix.hud.mixin;

import me.Gabix.hud.CustomHudRenderer;
//? if >=1.20 {
/*import net.minecraft.client.gui.DrawContext;
*///?}
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.20 {
/*import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(InGameHud.class)
public class InGameHudMixin {

    //? if >=1.20 {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void mvp$renderCustomHud(DrawContext context, float tickDelta, CallbackInfo ci) {
        CustomHudRenderer.render(context, tickDelta);
    }
    *///?}
}
