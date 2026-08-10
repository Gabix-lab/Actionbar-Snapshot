package me.Gabix.soundreducer.mixin;

import me.Gabix.soundreducer.SoundReducer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "onWindowFocusChanged", at = @At("RETURN"))
    private void onFocusChange(boolean focused, CallbackInfo ci) {
        SoundReducer.onFocusChange(focused);
    }
}