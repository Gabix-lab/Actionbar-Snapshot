package me.Gabix.renderscale.mixin.accessors;

import me.Gabix.renderscale.accessors.MainRenderTargetSetter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftAccessor implements MainRenderTargetSetter {
    @Mutable @Shadow @Final private Framebuffer framebuffer;

    @Override
    public void renderScale$setMainRenderTarget(Framebuffer fb) {
        this.framebuffer = fb;
    }
}