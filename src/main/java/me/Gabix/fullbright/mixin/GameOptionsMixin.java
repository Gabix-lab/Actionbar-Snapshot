package me.Gabix.fullbright.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Shadow
    private SimpleOption<Double> gamma;
    private double gammaOriginalValue = 0.0;
    private boolean gammaModified = false;

    @Inject(method = "write", at = @At("HEAD"))
    private void onWriteHead(CallbackInfo ci) {
        if (gamma != null) {
            gammaOriginalValue = gamma.getValue();
            if (gammaOriginalValue > 1.0) {
                gamma.setValue(1.0);
                gammaModified = true;
            }
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void onWriteReturn(CallbackInfo ci) {
        if (gamma != null && gammaModified) {
            gamma.setValue(gammaOriginalValue);
            gammaModified = false;
        }
    }
}