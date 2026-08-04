package me.Gabix.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import me.Gabix.NoRenderManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.light.LightingProvider;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class NoRenderMixin {

    @Mixin(WorldRenderer.class)
    public static class LevelRendererMixin {

        @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;doLightUpdates()I"), require = 0)
        private boolean skipLightUpdates(LightingProvider instance) { return !NoRenderManager.enabled; }

        @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntities()Ljava/lang/Iterable;"), require = 0)
        private Iterable<Entity> hideEntities(Iterable<Entity> original) { return NoRenderManager.enabled ? Collections.emptyList() : original; }

        @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLorg/joml/Matrix4f;)V"), require = 0)
        private boolean hideBlocks(WorldRenderer instance, RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix) { return !NoRenderManager.enabled; }

        @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask$RenderData;getBlockEntities()Ljava/util/List;"), require = 0)
        private List<BlockEntity> hideBlockEntities(List<BlockEntity> original) { return NoRenderManager.enabled ? Collections.emptyList() : original; }

        @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;noCullingBlockEntities:Ljava/util/Set;", ordinal = 0), require = 0)
        private Set<BlockEntity> hideGlobalBlockEntities(Set<BlockEntity> original) { return NoRenderManager.enabled ? Collections.emptySet() : original; }

        @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"), require = 0)
        private boolean hideSky(WorldRenderer instance, MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) { return !NoRenderManager.enabled; }

        @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V"), require = 0)
        private boolean hideClouds(WorldRenderer instance, MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ) { return !NoRenderManager.enabled; }

        @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/client/render/Camera;F)V"), require = 0)
        private boolean hideParticles(ParticleManager instance, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f) { return !NoRenderManager.enabled; }
    }

    @Mixin(BackgroundRenderer.class)
    public static class FogMixin {
        @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
        private static void hideFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
            if (NoRenderManager.enabled && fogType == BackgroundRenderer.FogType.FOG_TERRAIN) {
                RenderSystem.setShaderFogStart(200000.0F);
                RenderSystem.setShaderFogEnd(200000.0F);
                ci.cancel();
            }
        }
    }
}