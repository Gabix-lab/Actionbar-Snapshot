package me.Gabix.chunk.mixin;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the package-private occlusion graph of a built chunk.
 *
 * <p>Vanilla only checks visibility between adjacent chunks during its BFS.
 * The optimizer uses the raw 6x6 graph to detect chunks that are completely
 * enclosed by solid blocks; those chunks can keep their existing GPU buffers
 * and be rebuilt later, when the player gets close.</p>
 */
@Mixin(ChunkBuilder.ChunkData.class)
public interface ChunkOcclusionMixin {

    @Accessor("occlusionGraph")
    ChunkOcclusionData mvp$getOcclusionGraph();
}
