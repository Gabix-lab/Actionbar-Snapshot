package me.Gabix.norender.mixin;

import me.Gabix.norender.SoundSuppressor;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void modifyVolume(CallbackInfo ci) {
        if (SoundSuppressor.isVolumeReduced()) {
            ci.cancel();
        }
    }
}