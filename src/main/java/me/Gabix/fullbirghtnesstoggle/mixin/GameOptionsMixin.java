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

    @Inject(method = "write", at = @At("HEAD"))
    private void onWrite(CallbackInfo ci) {
        if (gamma != null && gamma.getValue() > 1.0) {
            gamma.setValue(1.0);
        }
    }
}