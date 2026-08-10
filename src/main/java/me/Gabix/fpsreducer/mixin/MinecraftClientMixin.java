package me.Gabix.fpsreducer.mixin;

import me.Gabix.fpsreducer.FpsReducerLogic;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    private native int getFramerateLimit();

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;getFramerateLimit()I"
            )
    )
    private int overrideFramerateLimit(MinecraftClient instance) {
        if (FpsReducerLogic.isActive()) {
            return FpsReducerLogic.getTargetFps();
        }
        return this.getFramerateLimit();
    }
}