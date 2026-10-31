package me.Gabix.chunk.mixin;

import me.Gabix.chunk.ChunkFadeManager;
import me.Gabix.chunk.ChunkLoadOptimizer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame boundary + draw hook for the chunk features.
 *
 * <p>The dispatch happens at the very start of {@code WorldRenderer.render},
 * before updateChunks()/upload(), so deferred rebuilds can make progress in the
 * same frame. The draw hook wraps each {@code VertexBuffer.draw()} with the
 * fade shader colour.</p>
 *
 * <p>Every injection is optional ({@code require = 0}). Sodium replaces this
 * renderer; the mixin then simply never runs (or fails to find its target)
 * without crashing the game.</p>
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererChunkMixin {

    @Shadow
    public abstract ChunkBuilder getChunkBuilder();

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void mvp$beginChunkFrame(CallbackInfo ci) {
        ChunkLoadOptimizer.beginFrame(this.getChunkBuilder());
        ChunkFadeManager.beginFrame();
    }

    /**
     * Called after the upload future completed, i.e. when builtAt can start.
     * At this point the data is in the GPU buffer and the chunk becomes
     * renderable on the next renderable-chunk update.
     */
    @Inject(method = "addBuiltChunk", at = @At("HEAD"), require = 0)
    private void mvp$onChunkBuilt(ChunkBuilder.BuiltChunk chunk, CallbackInfo ci) {
        ChunkFadeManager.onChunkBuilt(chunk);
    }

    @Inject(method = "setWorld", at = @At("HEAD"), require = 0)
    private void mvp$onWorldChanged(ClientWorld world, CallbackInfo ci) {
        ChunkLoadOptimizer.clear();
        ChunkFadeManager.onRendererReload();
    }

    @Inject(method = "reload()V", at = @At("HEAD"), require = 0)
    private void mvp$onRendererReloaded(CallbackInfo ci) {
        ChunkLoadOptimizer.clear();
        ChunkFadeManager.onRendererReload();
    }

    @Inject(
            method = "renderLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/VertexBuffer;draw()V", shift = At.Shift.BEFORE),
            require = 0
    )
    private void mvp$beforeChunkDraw(RenderLayer renderLayer, MatrixStack matrices,
                                     double cameraX, double cameraY, double cameraZ,
                                     Matrix4f positionMatrix, CallbackInfo ci) {
        ChunkFadeManager.beforeChunkDraw(renderLayer);
    }

    @Inject(
            method = "renderLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/VertexBuffer;draw()V", shift = At.Shift.AFTER),
            require = 0
    )
    private void mvp$afterChunkDraw(RenderLayer renderLayer, MatrixStack matrices,
                                    double cameraX, double cameraY, double cameraZ,
                                    Matrix4f positionMatrix, CallbackInfo ci) {
        ChunkFadeManager.afterChunkDraw();
    }
}
