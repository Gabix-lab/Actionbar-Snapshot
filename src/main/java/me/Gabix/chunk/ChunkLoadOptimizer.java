package me.Gabix.chunk;

import me.Gabix.chunk.mixin.ChunkOcclusionMixin;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.ChunkData;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schedules vanilla async chunk rebuilds through a small, render-thread-only
 * queue instead of letting {@code WorldRenderer.updateChunks()} submit every
 * dirty chunk in one frame.
 *
 * <p>Only {@code BuiltChunk.scheduleRebuild(ChunkBuilder, ChunkRendererRegionBuilder)}
 * is intercepted. Important rebuilds (block changes near the player) use
 * {@code ChunkBuilder.rebuild(...)} and therefore bypass this queue completely.</p>
 *
 * <p>All methods must be called from the render thread. The queue is not
 * synchronized on purpose: that keeps the hot path allocation- and lock-free.</p>
 */
public final class ChunkLoadOptimizer {

    private static final int MIN_BUDGET = 1;
    private static final int MAX_BUDGET = 8;

    /**
     * Hard safety cap. If the queue ever grows beyond this, new rebuilds are
     * submitted immediately (vanilla behaviour) instead of being deferred.
     * Kept small so a broken/stuck queue cannot hold thousands of chunks.
     */
    private static final int MAX_PENDING = 512;

    /** Occlusion accessor availability: 0 = unknown, 1 = working, 2 = broken. */
    private static volatile int occlusionAccessorState = 0;

    /** Do not use the occlusion graph to postpone chunks this close to the camera. */
    private static final double OCCLUSION_MIN_DISTANCE_SQ = 24.0 * 24.0;

    /** LOD only throttles rebuilds beyond this distance (in blocks). */
    private static final double LOD_DISTANCE_SQ = 128.0 * 128.0;

    /** Minimum interval between two far rebuilds when LOD is enabled. */
    private static final long LOD_INTERVAL_MS = 200L;

    /** Chunks farther than this may wait while they are behind the camera. */
    private static final double BEHIND_MIN_DISTANCE_SQ = 48.0 * 48.0;

    /** cos(~107 degrees); lower values mean the chunk is clearly behind the view. */
    private static final double BEHIND_DOT_THRESHOLD = -0.30;

    private static final Map<ChunkBuilder.BuiltChunk, PendingChunk> PENDING = new LinkedHashMap<>();
    private static final List<PendingChunk> CANDIDATES = new ArrayList<>();

    /**
     * Set while the optimizer itself calls scheduleRebuild. The mixin must let
     * that call through instead of re-deferring the chunk.
     *
     * <p>Optimizer dispatch runs only on the render thread, so a plain volatile
     * flag is enough and avoids a ThreadLocal lookup on every queued rebuild.</p>
     */
    private static volatile boolean dispatching = false;

    private static long nextSequence;
    private static Vec3d cameraPos = Vec3d.ZERO;
    private static Vec3d cameraForward = Vec3d.ZERO;
    private static boolean initialized;
    private static boolean errorLogged;
    private static boolean pendingOverflowWarned;

    private ChunkLoadOptimizer() {
    }

    public static void init() {
        initialized = true;
        PENDING.clear();
        CANDIDATES.clear();
        cameraPos = Vec3d.ZERO;
        cameraForward = Vec3d.ZERO;
        nextSequence = 0L;
        errorLogged = false;
        pendingOverflowWarned = false;
        occlusionAccessorState = 0;
        dispatching = false;
    }

    /**
     * Called from {@code BuiltChunk.scheduleRebuild(...)} HEAD.
     *
     * @return true when the vanilla call should be cancelled and the chunk kept
     *         in the optimizer queue
     */
    public static boolean shouldDefer(ChunkBuilder.BuiltChunk chunk) {
        if (!initialized || chunk == null) return false;
        if (dispatching) return false;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) return false;
        if (!config.chunkLoadOptimization && !config.chunkOcclusionCulling && !config.chunkLodEnabled) {
            return false;
        }

        // Important rebuilds are never allowed to wait behind the budget.
        if (chunk.needsImportantRebuild()) return false;

        if (PENDING.size() >= MAX_PENDING) {
            // Only warn once: a full queue would otherwise spam the log every frame.
            if (!pendingOverflowWarned) {
                pendingOverflowWarned = true;
                System.err.println("[MVP] Chunk optimizer queue overflow (" + MAX_PENDING
                        + "), reverting to vanilla scheduling.");
            }
            return false;
        }

        boolean defer = config.chunkLoadOptimization;
        if (!defer && (config.chunkOcclusionCulling || config.chunkLodEnabled)) {
            // One distance calculation for both optional checks.
            double distanceSq = distanceSq(chunk, cameraPos);
            if (config.chunkOcclusionCulling) {
                defer = isOccluded(chunk, distanceSq);
            }
            if (!defer && config.chunkLodEnabled) {
                defer = isLodThrottled(chunk, distanceSq, System.currentTimeMillis());
            }
        }
        if (!defer) return false;

        if (!PENDING.containsKey(chunk)) {
            PENDING.put(chunk, new PendingChunk(chunk, nextSequence++));
        }
        return true;
    }

    /**
     * Frame boundary hook. Dispatches at most the configured number of rebuild
     * requests, nearest chunks first when distance priority is enabled.
     */
    public static void beginFrame(ChunkBuilder chunkBuilder) {
        if (!initialized) return;

        cameraPos = cameraPosition();
        cameraForward = cameraForward();
        if (chunkBuilder == null) {
            PENDING.clear();
            return;
        }

        // Match setupTerrain's camera position so tasks dispatched at the start
        // of this frame use current (not previous-frame) chunk-face culling.
        chunkBuilder.setCameraPosition(cameraPos);

        if (PENDING.isEmpty()) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) {
            PENDING.clear();
            return;
        }

        long now = System.currentTimeMillis();
        CANDIDATES.clear();

        for (PendingChunk pending : PENDING.values()) {
            // Calculate distance once and pass it down to avoid the duplicate
            // distanceSq computation in isEligible/isOccluded/isLodThrottled.
            pending.distanceSq = distanceSq(pending.chunk, cameraPos);
            if (!isEligible(pending.chunk, config, cameraPos, now, pending.distanceSq)) continue;
            CANDIDATES.add(pending);
        }

        if (CANDIDATES.isEmpty()) return;

        // When the budget feature is off (only occlusion/LOD active), drain all
        // eligible chunks. Otherwise the slider limits this frame's submissions.
        int budget = config.chunkLoadOptimization
                ? Math.max(MIN_BUDGET, Math.min(MAX_BUDGET, config.chunkBuildBudget))
                : CANDIDATES.size();
        budget = Math.min(budget, CANDIDATES.size());

        if (config.chunkDistancePriority && budget < CANDIDATES.size()) {
            CANDIDATES.sort((a, b) -> {
                int byDistance = Double.compare(a.distanceSq, b.distanceSq);
                return byDistance != 0 ? byDistance : Long.compare(a.sequence, b.sequence);
            });
        }

        if (budget <= 0) return;

        // Allocate only when there is actually something to dispatch. The
        // builder is reused for every chunk submitted in this frame.
        ChunkRendererRegionBuilder regionBuilder = new ChunkRendererRegionBuilder();
        for (int i = 0; i < budget; i++) {
            PendingChunk pending = CANDIDATES.get(i);
            if (!PENDING.containsKey(pending.chunk)) continue;
            dispatch(pending, chunkBuilder, regionBuilder);
        }

        CANDIDATES.clear();
    }

    /** Called when vanilla performs an immediate (synchronous) rebuild. */
    public static void onImmediateRebuild(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;
        PENDING.remove(chunk);
        ChunkBuildState state = state(chunk);
        if (state != null) state.mvp$setLastBuildAttempt(System.currentTimeMillis());
    }

    /** A built chunk is being recycled for another position; drop stale queue entries. */
    public static void onOriginChanged(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;
        PENDING.remove(chunk);
    }

    /** World change / renderer reload. */
    public static void clear() {
        PENDING.clear();
        CANDIDATES.clear();
        cameraPos = Vec3d.ZERO;
        cameraForward = Vec3d.ZERO;
        pendingOverflowWarned = false;
    }

    private static void dispatch(PendingChunk pending, ChunkBuilder chunkBuilder,
                                 ChunkRendererRegionBuilder regionBuilder) {
        PENDING.remove(pending.chunk);
        ChunkBuildState state = state(pending.chunk);
        if (state != null) state.mvp$setLastBuildAttempt(System.currentTimeMillis());

        dispatching = true;
        try {
            // Goes through the normal vanilla path (createRebuildTask + send).
            // The mixin sees dispatching and lets this call through.
            pending.chunk.scheduleRebuild(chunkBuilder, regionBuilder);
            // Mainstream vanilla does this in updateChunks right after scheduling.
            // Without it the chunk would be picked up again on the next frame.
            pending.chunk.cancelRebuild();
        } catch (Throwable throwable) {
            if (!errorLogged) {
                errorLogged = true;
                System.err.println("[MVP] Deferred chunk rebuild failed: " + throwable);
                throwable.printStackTrace();
            }
        } finally {
            dispatching = false;
        }
    }

    private static boolean isEligible(ChunkBuilder.BuiltChunk chunk, StorageConfig config,
                                      Vec3d camera, long now, double distanceSq) {
        // Never burn build time on far chunks that are currently behind the
        // camera. They stay queued and are dispatched as soon as the view turns.
        if (isBehindCamera(chunk, camera, cameraForward, distanceSq)) {
            return false;
        }

        // Fully enclosed chunks are still rendered from their old buffers; only
        // their (mostly invisible) rebuild is postponed until the player is close.
        if (config.chunkOcclusionCulling && distanceSq > OCCLUSION_MIN_DISTANCE_SQ) {
            ChunkData data = chunk.getData();
            if (data != null && !data.isEmpty() && isFullyOccluded(data)) {
                return false;
            }
        }

        if (config.chunkLodEnabled && distanceSq > LOD_DISTANCE_SQ) {
            ChunkBuildState state = state(chunk);
            if (state != null) {
                long lastAttempt = state.mvp$getLastBuildAttempt();
                if (lastAttempt == 0L) lastAttempt = state.mvp$getBuiltAt();
                if (lastAttempt != 0L && now >= lastAttempt && now - lastAttempt < LOD_INTERVAL_MS) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isOccluded(ChunkBuilder.BuiltChunk chunk, double distanceSq) {
        if (distanceSq <= OCCLUSION_MIN_DISTANCE_SQ) return false;
        ChunkData data = chunk.getData();
        return data != null && !data.isEmpty() && isFullyOccluded(data);
    }

    /**
     * Checks all 6x6 direction pairs. A non-empty chunk whose graph has no
     * visible pair is fully closed, so its outer faces are already in the
     * existing GPU buffer and a deferred rebuild cannot become visible before
     * the player moves closer.
     *
     * <p>The accessor is probed once: after a failure the optimizer permanently
     * uses the public fallback instead of paying for a try/catch on every chunk.</p>
     */
    private static boolean isFullyOccluded(ChunkData data) {
        if (occlusionAccessorState == 2) {
            // Accessor mixin not available: use the public API only.
            return isFullyOccludedFallback(data);
        }
        if (occlusionAccessorState == 1) {
            ChunkOcclusionData graph = ((ChunkOcclusionMixin) (Object) data).mvp$getOcclusionGraph();
            return hasNoVisiblePair(graph);
        }
        // First call: try the accessor, cache whether it works.
        try {
            ChunkOcclusionData graph = ((ChunkOcclusionMixin) (Object) data).mvp$getOcclusionGraph();
            occlusionAccessorState = 1;
            return hasNoVisiblePair(graph);
        } catch (Throwable t) {
            occlusionAccessorState = 2;
            return isFullyOccludedFallback(data);
        }
    }

    private static boolean hasNoVisiblePair(ChunkOcclusionData graph) {
        for (Direction from : Direction.values()) {
            for (Direction to : Direction.values()) {
                if (graph.isVisibleThrough(from, to)) return false;
            }
        }
        return true;
    }

    private static boolean isFullyOccludedFallback(ChunkData data) {
        for (Direction from : Direction.values()) {
            for (Direction to : Direction.values()) {
                if (data.isVisibleThrough(from, to)) return false;
            }
        }
        return true;
    }

    private static boolean isLodThrottled(ChunkBuilder.BuiltChunk chunk, double distanceSq, long now) {
        if (distanceSq <= LOD_DISTANCE_SQ) return false;

        ChunkBuildState state = state(chunk);
        if (state == null) return false;
        long lastAttempt = state.mvp$getLastBuildAttempt();
        if (lastAttempt == 0L) lastAttempt = state.mvp$getBuiltAt();
        return lastAttempt != 0L && now >= lastAttempt && now - lastAttempt < LOD_INTERVAL_MS;
    }

    private static boolean isBehindCamera(ChunkBuilder.BuiltChunk chunk, Vec3d camera, Vec3d forward,
                                          double distanceSq) {
        if (forward.lengthSquared() < 1.0E-6) return false;

        if (distanceSq <= BEHIND_MIN_DISTANCE_SQ) return false;

        BlockPos origin = chunk.getOrigin();
        double dx = origin.getX() + 8.0 - camera.x;
        double dy = origin.getY() + 8.0 - camera.y;
        double dz = origin.getZ() + 8.0 - camera.z;
        double dot = dx * forward.x + dy * forward.y + dz * forward.z;
        return dot < BEHIND_DOT_THRESHOLD * Math.sqrt(distanceSq);
    }

    private static ChunkBuildState state(ChunkBuilder.BuiltChunk chunk) {
        return chunk instanceof ChunkBuildState ? (ChunkBuildState) chunk : null;
    }

    private static double distanceSq(ChunkBuilder.BuiltChunk chunk, Vec3d camera) {
        BlockPos origin = chunk.getOrigin();
        double dx = origin.getX() + 8.0 - camera.x;
        double dy = origin.getY() + 8.0 - camera.y;
        double dz = origin.getZ() + 8.0 - camera.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vec3d cameraPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.gameRenderer != null && client.gameRenderer.getCamera() != null) {
            return client.gameRenderer.getCamera().getPos();
        }
        if (client != null && client.player != null) {
            return client.player.getPos();
        }
        return Vec3d.ZERO;
    }

    private static Vec3d cameraForward() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.gameRenderer != null && client.gameRenderer.getCamera() != null) {
            return Vec3d.fromPolar(client.gameRenderer.getCamera().getPitch(),
                    client.gameRenderer.getCamera().getYaw());
        }
        return Vec3d.ZERO;
    }

    private static final class PendingChunk {
        private final ChunkBuilder.BuiltChunk chunk;
        private final long sequence;
        private double distanceSq;

        private PendingChunk(ChunkBuilder.BuiltChunk chunk, long sequence) {
            this.chunk = chunk;
            this.sequence = sequence;
        }
    }
}
