package me.Gabix.norender.mixin;

import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    // Volume control is now handled directly in SoundSuppressor by adjusting game options
}
