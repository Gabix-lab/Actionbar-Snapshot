package me.Gabix.renderscale.mixin;

import me.Gabix.renderscale.RenderScale;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(DebugHud.class)
public abstract class MixinDebugScreenOverlay {
    @Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true)
    private void renderScale$addScale(CallbackInfoReturnable<List<String>> cir) {
        RenderScale rs = RenderScale.getInstance();
        if (rs != null && cir.getReturnValue() != null) {
            List<String> lines = new ArrayList<>(cir.getReturnValue());
            lines.add(Math.min(2, lines.size()), String.format(Locale.ROOT,
                    "Render scale: %.1f%% (%s)",
                    rs.getRenderScaleFactor() * 100.0, rs.getScalingMode()));
            cir.setReturnValue(lines);
        }
    }
}