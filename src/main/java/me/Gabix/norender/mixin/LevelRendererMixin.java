package me.Gabix.norender.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import me.Gabix.norender.NoRenderEngine;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.light.LightingProvider;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mixin(WorldRenderer.class)
public class LevelRendererMixin {

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;doLightUpdates()I"), require = 0)
    private boolean skipLightUpdates(LightingProvider instance) {
        return !NoRenderEngine.enabled;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntities()Ljava/lang/Iterable;"), require = 0)
    private Iterable<Entity> hideEntities(Iterable<Entity> original) {
        return NoRenderEngine.enabled ? Collections.emptyList() : original;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLorg/joml/Matrix4f;)V"), require = 0)
    private boolean hideBlocks(WorldRenderer instance, RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix) {
        if (NoRenderEngine.enabled) return false;
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config != null && config.hideBlockTextures) {
            if (renderLayer == RenderLayer.getSolid()
                    || renderLayer == RenderLayer.getCutoutMipped()
                    || renderLayer == RenderLayer.getCutout()
                    || renderLayer == RenderLayer.getTranslucent()) {
                return false;
            }
        }
        return true;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask$RenderData;getBlockEntities()Ljava/util/List;"), require = 0)
    private List<BlockEntity> hideBlockEntities(List<BlockEntity> original) {
        return NoRenderEngine.enabled ? Collections.emptyList() : original;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;noCullingBlockEntities:Ljava/util/Set;", ordinal = 0), require = 0)
    private Set<BlockEntity> hideGlobalBlockEntities(Set<BlockEntity> original) {
        return NoRenderEngine.enabled ? Collections.emptySet() : original;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"), require = 0)
    private boolean hideSky(WorldRenderer instance, MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        return !NoRenderEngine.enabled;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V"), require = 0)
    private boolean hideClouds(WorldRenderer instance, MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        return !NoRenderEngine.enabled;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/client/render/Camera;F)V"), require = 0)
    private boolean hideParticles(ParticleManager instance, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f) {
        if (NoRenderEngine.enabled) return false;
        StorageConfig config = ActionbarStorageClient.getConfig();
        return config == null || !config.hideAllParticles;
    }
}