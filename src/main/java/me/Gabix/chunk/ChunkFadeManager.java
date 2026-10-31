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

public final class ChunkFadeManager {

    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");

    private static final Map<ChunkBuilder.BuiltChunk, Long> FADES = new ConcurrentHashMap<>();

    private static final ThreadLocal<ChunkBuilder.BuiltChunk> CURRENT_CHUNK = new ThreadLocal<>();

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

        state.mvp$setFadeEligible(false);
    }

    public static void onRendererReload() {
        FADES.clear();
        CURRENT_CHUNK.remove();
        fadeApplied = false;
        skipWindowStart = System.currentTimeMillis();
    }

    public static void onOriginChanged(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;
        FADES.remove(chunk);
        if (CURRENT_CHUNK.get() == chunk) CURRENT_CHUNK.remove();
    }

    public static void onBufferRequested(ChunkBuilder.BuiltChunk chunk, RenderLayer layer) {
        if (FADES.isEmpty()) return;
        if (SODIUM_LOADED || !initialized || chunk == null) return;
        if (!isFadeLayer(layer)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isOnThread()) return;

        CURRENT_CHUNK.set(chunk);
    }

    public static void beforeChunkDraw(RenderLayer layer) {
        if (FADES.isEmpty()) return;
        if (SODIUM_LOADED || !initialized) return;
        if (!isFadeLayer(layer)) return;

        ChunkBuilder.BuiltChunk chunk = CURRENT_CHUNK.get();
        if (chunk == null) return;

        float alpha = getFadeAlpha(chunk);
        if (alpha >= 0.999F) return;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        applyColorModulator(1.0F, 1.0F, 1.0F, alpha);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO);

        fadeApplied = true;
    }

    public static void afterChunkDraw() {
        if (!fadeApplied) return;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        applyColorModulator(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        fadeApplied = false;
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
        return t * t * (3.0F - 2.0F * t);
    }

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