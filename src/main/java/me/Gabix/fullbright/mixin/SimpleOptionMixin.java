package me.Gabix.fullbright.mixin;

import me.Gabix.fullbright.event.ToggleEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
    @Shadow T value;

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void forceGammaValue(CallbackInfoReturnable<T> cir) {
        if (ToggleEvent.isSaving) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            SimpleOption<Double> gammaOption = client.options.getGamma();
            if ((Object) this == (Object) gammaOption && value instanceof Double) {
                if (ToggleEvent.isFullbrightEnabled()) {
                    cir.setReturnValue((T) Double.valueOf(ToggleEvent.MAX_GAMMA));
                }
            }
        }
    }

    @Inject(method = "setValue", at = @At("HEAD"), cancellable = true)
    private void clampGammaValue(T newValue, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            SimpleOption<Double> gammaOption = client.options.getGamma();
            if ((Object) this == (Object) gammaOption && newValue instanceof Double d) {
                if (d < 0.0 || d > 1.0) {
                    ci.cancel();
                }
            }
        }
    }
}