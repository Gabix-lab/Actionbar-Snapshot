package me.Gabix.soundreducer.mixin;

import me.Gabix.soundreducer.SoundReducer;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Inject(method = "getSoundVolume", at = @At("HEAD"), cancellable = true)
    private void onGetSoundVolume(SoundCategory category, CallbackInfoReturnable<Float> cir) {
        if (SoundReducer.isSuppressed()) {
            cir.setReturnValue(0.0f);
        }
    }
}