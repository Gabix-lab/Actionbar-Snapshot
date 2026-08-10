package me.Gabix.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void reducePlacementDelay(CallbackInfo ci) {
        if (this.itemUseCooldown > 3) {
            this.itemUseCooldown = 3;
        }
    }
}