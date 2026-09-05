package me.Gabix.fullbrightnesstoggle.mixin;

import me.Gabix.fullbrightnesstoggle.event.ToggleEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {

    @Shadow
    T value;

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void forceGammaValue(CallbackInfoReturnable<T> cir) {
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
}