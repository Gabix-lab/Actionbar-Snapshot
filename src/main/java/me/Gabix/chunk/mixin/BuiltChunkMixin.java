package me.Gabix.chunk.mixin;

import me.Gabix.chunk.ChunkBuildState;
import me.Gabix.chunk.ChunkFadeManager;
import me.Gabix.chunk.ChunkLoadOptimizer;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds fade/LOD bookkeeping to vanilla built chunks and reroutes the async
 * rebuild path through {@link ChunkLoadOptimizer}.
 *
 * <p>All injections use {@code require = 0}: if another renderer (e.g. Sodium)
 * changes these methods, the mod must still start and simply lose the feature.</p>
 */
@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class BuiltChunkMixin implements ChunkBuildState {

    /**
     * Wall-clock timestamp of the last completed GPU upload. Deliberately
     * placed on the vanilla class (not a side map) so recycled chunk objects
     * cannot produce stale fade data.
     */
    @Unique
    private volatile long builtAt;

    @Unique
    private volatile long mvp$lastBuildAttempt;

    /**
     * True until this built chunk has been built once at its current origin.
     * setOrigin resets it, so a freshly moved/reused chunk fades again, while
     * ordinary block-update rebuilds do not (unless the user disables
     * "only new chunks").
     */
    @Unique
    private volatile boolean mvp$fadeEligible = true;

    @Inject(method = "setOrigin", at = @At("TAIL"), require = 0)
    private void mvp$resetChunkState(int x, int y, int z, CallbackInfo ci) {
        this.builtAt = 0L;
        this.mvp$lastBuildAttempt = 0L;
        this.mvp$fadeEligible = true;

        // The same object is recycled for a different world position.
        ChunkBuilder.BuiltChunk self = (ChunkBuilder.BuiltChunk) (Object) this;
        ChunkLoadOptimizer.onOriginChanged(self);
        ChunkFadeManager.onOriginChanged(self);
    }

    /**
     * Captures which chunk is currently being bound. WorldRenderer.renderLayer
     * calls getBuffer immediately before the draw; the draw hook then uses this
     * value to apply the per-chunk fade alpha.
     */
    @Inject(method = "getBuffer", at = @At("HEAD"), require = 0)
    private void mvp$captureFadeChunk(RenderLayer layer, CallbackInfoReturnable<VertexBuffer> cir) {
        ChunkFadeManager.onBufferRequested((ChunkBuilder.BuiltChunk) (Object) this, layer);
    }

    /**
     * Vanilla calls this for every non-synchronous rebuild from
     * WorldRenderer.updateChunks. Cancelling here leaves the chunk queued in the
     * optimizer; updateChunks' following cancelRebuild() clears the vanilla
     * dirty flag exactly as it does after a normal schedule.
     */
    @Inject(
            method = "scheduleRebuild(Lnet/minecraft/client/render/chunk/ChunkBuilder;Lnet/minecraft/client/render/chunk/ChunkRendererRegionBuilder;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void mvp$deferRebuild(ChunkBuilder chunkBuilder, ChunkRendererRegionBuilder regionBuilder,
                                  CallbackInfo ci) {
        if (ChunkLoadOptimizer.shouldDefer((ChunkBuilder.BuiltChunk) (Object) this)) {
            ci.cancel();
        }
    }

    @Override
    public long mvp$getBuiltAt() {
        return this.builtAt;
    }

    @Override
    public void mvp$setBuiltAt(long value) {
        this.builtAt = value;
    }

    @Override
    public boolean mvp$isFadeEligible() {
        return this.mvp$fadeEligible;
    }

    @Override
    public void mvp$setFadeEligible(boolean value) {
        this.mvp$fadeEligible = value;
    }

    @Override
    public long mvp$getLastBuildAttempt() {
        return this.mvp$lastBuildAttempt;
    }

    @Override
    public void mvp$setLastBuildAttempt(long value) {
        this.mvp$lastBuildAttempt = value;
    }
}
