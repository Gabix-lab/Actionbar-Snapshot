package me.Gabix.chunk;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smooth per-chunk fade-in for newly built terrain.
 *
 * <p>Fade state is tracked per {@code BuiltChunk}. Rendering works by setting
 * {@link RenderSystem#setShaderColor(float, float, float, float)} and updating
 * the already bound {@code ColorModulator} uniform right before the chunk's
 * {@code VertexBuffer.draw()} call. Only the opaque/cutout block layers are
 * blended; depth testing and depth writes are deliberately left untouched.</p>
 *
 * <p><b>Sodium:</b> Sodium owns terrain rendering and does not use vanilla's
 * {@code WorldRenderer.renderLayer}. Fade must never touch its GL state, so all
 * fade entry points become no-ops when Sodium is detected.</p>
 */
public final class ChunkFadeManager {

    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");

    /**
     * Concurrent: {@code WorldRenderer.addBuiltChunk} may complete on a worker
     * thread for chunks without visible geometry, while the render thread
     * iterates/prunes this map during the frame and draw hooks. A plain HashMap
     * caused a ConcurrentModificationException here.
     */
    private static final Map<ChunkBuilder.BuiltChunk, Long> FADES = new ConcurrentHashMap<>();

    /** Set by BuiltChunk.getBuffer on the render thread and consumed by the draw hook. */
    private static final ThreadLocal<ChunkBuilder.BuiltChunk> CURRENT_CHUNK = new ThreadLocal<>();

    /**
     * True between beforeChunkDraw and afterChunkDraw while GL state needs
     * restoring. Draw hooks only run on the render thread, so a plain field is
     * enough and avoids the ThreadLocal lookup per chunk draw.
     */
    private static boolean fadeApplied = false;

    private static boolean initialized;
    private static long skipWindowStart = System.currentTimeMillis();

    private ChunkFadeManager() {
    }

    public static void init() {
        initialized = true;
        FADES.clear();
        CURRENT_CHUNK.remove();
        fadeApplied = false;
        skipWindowStart = System.currentTimeMillis();

        if (SODIUM_LOADED) {
            System.out.println("[MVP] Sodium detected: vanilla chunk fade-in is disabled.");
        }
    }

    /** Frame boundary hook; also prunes fades of chunks that never reached the screen. */
    public static void beginFrame() {
        CURRENT_CHUNK.remove();
        fadeApplied = false;

        if (SODIUM_LOADED || !initialized) {
            if (!FADES.isEmpty()) FADES.clear();
            return;
        }

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null || !config.chunkFadeInEnabled) {
            if (!FADES.isEmpty()) FADES.clear();
            return;
        }

        if (FADES.isEmpty()) return;

        long now = System.currentTimeMillis();
        long maxAge = Math.max(50, Math.min(1000, config.chunkFadeInDurationMs)) + 1000L;
        FADES.entrySet().removeIf(entry -> now - entry.getValue() > maxAge);
    }

    /**
     * Called after the chunk's GPU upload finished (WorldRenderer.addBuiltChunk).
     * builtAt is recorded even on Sodium so other features (LOD) keep working.
     */
    public static void onChunkBuilt(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;

        ChunkBuildState state = state(chunk);
        if (state == null) return;

        long now = System.currentTimeMillis();
        state.mvp$setBuiltAt(now);

        if (SODIUM_LOADED || !initialized) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null || !config.chunkFadeInEnabled) {
            FADES.remove(chunk);
            // Consume eligibility so enabling fade later does not make already
            // loaded chunks fade on their next block update.
            state.mvp$setFadeEligible(false);
            return;
        }

        boolean eligible = !config.chunkFadeInOnlyNewChunks || state.mvp$isFadeEligible();
        boolean skipInitial = config.chunkFadeInUseSkip
                && config.chunkFadeInSkipMs > 0
                && now - skipWindowStart < config.chunkFadeInSkipMs;

        if (eligible && !skipInitial) {
            FADES.put(chunk, now);
        } else {
            FADES.remove(chunk);
        }

        // A chunk is "new" only for its first successful build at this origin.
        state.mvp$setFadeEligible(false);
    }

    /** World change / renderer reload resets the initial fade-skip window. */
    public static void onRendererReload() {
        FADES.clear();
        CURRENT_CHUNK.remove();
        fadeApplied = false;
        skipWindowStart = System.currentTimeMillis();
    }

    /** BuiltChunk is recycled for a different origin; forget its fade state. */
    public static void onOriginChanged(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;
        FADES.remove(chunk);
        if (CURRENT_CHUNK.get() == chunk) CURRENT_CHUNK.remove();
    }

    /**
     * Called from BuiltChunk.getBuffer HEAD. The draw hook needs to know which
     * chunk was bound; looking up a local variable in the render loop would be
     * fragile across mappings, so we capture it here instead.
     */
    public static void onBufferRequested(ChunkBuilder.BuiltChunk chunk, RenderLayer layer) {
        if (SODIUM_LOADED || !initialized || chunk == null) return;
        if (!isFadeLayer(layer)) return;

        // getBuffer is also called by worker threads while completing uploads.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isOnThread()) return;

        CURRENT_CHUNK.set(chunk);
    }

    /** Immediately before VertexBuffer.draw() inside WorldRenderer.renderLayer. */
    public static void beforeChunkDraw(RenderLayer layer) {
        if (SODIUM_LOADED || !initialized) return;
        if (!isFadeLayer(layer)) return;

        ChunkBuilder.BuiltChunk chunk = CURRENT_CHUNK.get();
        if (chunk == null) return;

        float alpha = getFadeAlpha(chunk);
        if (alpha >= 0.999F) return;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        applyColorModulator(1.0F, 1.0F, 1.0F, alpha);

        // Only opaque/cutout layers get blending. Translucent layers already
        // blend themselves and are intentionally left alone.
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO);

        fadeApplied = true;
    }

    /** Immediately after VertexBuffer.draw(); always restores the global shader colour. */
    public static void afterChunkDraw() {
        if (fadeApplied) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            applyColorModulator(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            // defaultBlendFunc() intentionally omitted: blend is disabled above,
            // so resetting the factors here would be a redundant GL call.
            fadeApplied = false;
        }

        CURRENT_CHUNK.remove();
    }

    private static float getFadeAlpha(ChunkBuilder.BuiltChunk chunk) {
        Long start = FADES.get(chunk);
        if (start == null) return 1.0F;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null || !config.chunkFadeInEnabled) {
            FADES.remove(chunk);
            return 1.0F;
        }

        long duration = Math.max(50, Math.min(1000, config.chunkFadeInDurationMs));
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed >= duration) {
            FADES.remove(chunk);
            return 1.0F;
        }
        if (elapsed <= 0L) return 0.0F;

        float t = Math.min(1.0F, elapsed / (float) duration);
        // Smoothstep keeps the first/last frames from looking like a hard cut.
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * renderLayer uploads the ColorModulator uniform once, before the chunk
     * loop. RenderSystem.setShaderColor alone would therefore only affect the
     * *next* layer, not the current draw, so the bound uniform is updated too.
     */
    private static void applyColorModulator(float red, float green, float blue, float alpha) {
        ShaderProgram shader = RenderSystem.getShader();
        if (shader != null && shader.colorModulator != null) {
            shader.colorModulator.set(red, green, blue, alpha);
            shader.colorModulator.upload();
        }
    }

    private static boolean isFadeLayer(RenderLayer layer) {
        return layer == RenderLayer.getSolid()
                || layer == RenderLayer.getCutoutMipped()
                || layer == RenderLayer.getCutout();
    }

    private static ChunkBuildState state(ChunkBuilder.BuiltChunk chunk) {
        return chunk instanceof ChunkBuildState ? (ChunkBuildState) chunk : null;
    }
}
