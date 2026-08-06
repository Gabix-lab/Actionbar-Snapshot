package me.Gabix.fullbirghtnesstoggle.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Shadow private SimpleOption<Double> gamma;
    private double gammaOriginalValue = 0.0;

    @Inject(method = "write", at = @At("HEAD"))
    private void onWriteHead(CallbackInfo ci) {
        if (gamma != null) {
            gammaOriginalValue = gamma.getValue();
            if (gammaOriginalValue > 1.0) {
                gamma.setValue(1.0);
            }
        }
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void onWriteTail(CallbackInfo ci) {
        if (gamma != null && gammaOriginalValue > 1.0) {
            gamma.setValue(gammaOriginalValue);
        }
    }
}