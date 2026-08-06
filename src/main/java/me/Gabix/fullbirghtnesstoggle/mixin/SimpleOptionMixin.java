package me.Gabix.fullbirghtnesstoggle.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {

    @Shadow
    T value;

    @Inject(method = "setValue", at = @At("HEAD"), cancellable = true)
    private void forceCustomValue(T value, CallbackInfo ci) {
        if (value instanceof Double) {
            this.value = value;
            ci.cancel();
        }
    }
}