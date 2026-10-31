package me.Gabix.chunk.mixin;

import me.Gabix.chunk.ChunkLoadOptimizer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bookkeeping for the synchronous rebuild path.
 *
 * <p>Important rebuilds and chunks close to the camera are compiled
 * immediately by {@code WorldRenderer.updateChunks} via
 * {@code ChunkBuilder.rebuild(...)}. They must never wait in the optimizer
 * queue, so any pending entry is removed here and the LOD timestamp updated.</p>
 */
@Mixin(ChunkBuilder.class)
public abstract class ChunkBuilderMixin {

    @Inject(method = "rebuild", at = @At("HEAD"), require = 0)
    private void mvp$onImmediateRebuild(ChunkBuilder.BuiltChunk chunk,
                                        ChunkRendererRegionBuilder regionBuilder,
                                        CallbackInfo ci) {
        ChunkLoadOptimizer.onImmediateRebuild(chunk);
    }
}
