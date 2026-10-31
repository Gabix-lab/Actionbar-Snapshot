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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChunkLoadOptimizer {

    private static final int MIN_BUDGET = 1;
    private static final int MAX_BUDGET = 8;
    private static final int MAX_PENDING = 512;

    private static volatile int occlusionAccessorState = 0;
    private static final double OCCLUSION_MIN_DISTANCE_SQ = 24.0 * 24.0;
    private static final double LOD_DISTANCE_SQ = 128.0 * 128.0;
    private static final long LOD_INTERVAL_MS = 200L;
    private static final double BEHIND_MIN_DISTANCE_SQ = 48.0 * 48.0;
    private static final double BEHIND_DOT_THRESHOLD = -0.30;

    private static final Map<ChunkBuilder.BuiltChunk, PendingChunk> PENDING = new LinkedHashMap<>();
    private static final List<PendingChunk> CANDIDATES = new ArrayList<>();
    private static final Map<ChunkBuilder.BuiltChunk, Long> OCCLUSION_CACHE = new IdentityHashMap<>();

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
        OCCLUSION_CACHE.clear();
        cameraPos = Vec3d.ZERO;
        cameraForward = Vec3d.ZERO;
        nextSequence = 0L;
        errorLogged = false;
        pendingOverflowWarned = false;
        occlusionAccessorState = 0;
        dispatching = false;
    }

    public static boolean shouldDefer(ChunkBuilder.BuiltChunk chunk) {
        if (!initialized || chunk == null) return false;
        if (dispatching) return false;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) return false;
        if (!config.chunkLoadOptimization && !config.chunkOcclusionCulling && !config.chunkLodEnabled) {
            return false;
        }

        if (chunk.needsImportantRebuild()) return false;

        if (PENDING.size() >= MAX_PENDING) {
            if (!pendingOverflowWarned) {
                pendingOverflowWarned = true;
                System.err.println("[MVP] Chunk optimizer queue overflow (" + MAX_PENDING
                        + "), reverting to vanilla scheduling.");
            }
            return false;
        }

        boolean defer = config.chunkLoadOptimization;
        if (!defer && (config.chunkOcclusionCulling || config.chunkLodEnabled)) {
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

    public static void beginFrame(ChunkBuilder chunkBuilder) {
        if (!initialized) return;

        cameraPos = cameraPosition();
        cameraForward = cameraForward();
        if (chunkBuilder == null) {
            PENDING.clear();
            return;
        }

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
            pending.distanceSq = distanceSq(pending.chunk, cameraPos);
            if (!isEligible(pending.chunk, config, cameraPos, now, pending.distanceSq)) continue;
            CANDIDATES.add(pending);
        }

        if (CANDIDATES.isEmpty()) return;

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

        ChunkRendererRegionBuilder regionBuilder = new ChunkRendererRegionBuilder();
        for (int i = 0; i < budget; i++) {
            PendingChunk pending = CANDIDATES.get(i);
            if (!PENDING.containsKey(pending.chunk)) continue;
            dispatch(pending, chunkBuilder, regionBuilder);
        }

        CANDIDATES.clear();
    }

    public static void onImmediateRebuild(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;
        PENDING.remove(chunk);
        OCCLUSION_CACHE.remove(chunk);
        ChunkBuildState state = state(chunk);
        if (state != null) state.mvp$setLastBuildAttempt(System.currentTimeMillis());
    }

    public static void onOriginChanged(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;
        PENDING.remove(chunk);
        OCCLUSION_CACHE.remove(chunk);
    }

    public static void clear() {
        PENDING.clear();
        CANDIDATES.clear();
        OCCLUSION_CACHE.clear();
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
            pending.chunk.scheduleRebuild(chunkBuilder, regionBuilder);
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
        if (isBehindCamera(chunk, camera, cameraForward, distanceSq)) {
            return false;
        }

        if (config.chunkOcclusionCulling && distanceSq > OCCLUSION_MIN_DISTANCE_SQ) {
            ChunkData data = chunk.getData();
            if (data != null && !data.isEmpty() && isFullyOccludedCached(chunk, data)) {
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
        return data != null && !data.isEmpty() && isFullyOccludedCached(chunk, data);
    }

    private static boolean isFullyOccludedCached(ChunkBuilder.BuiltChunk chunk, ChunkData data) {
        ChunkBuildState state = state(chunk);
        long builtAt = state != null ? state.mvp$getBuiltAt() : 0L;

        Long cachedAt = OCCLUSION_CACHE.get(chunk);
        if (cachedAt != null && cachedAt == builtAt) {
            return true;
        }

        if (isFullyOccluded(data)) {
            OCCLUSION_CACHE.put(chunk, builtAt);
            return true;
        }
        OCCLUSION_CACHE.remove(chunk);
        return false;
    }

    private static boolean isFullyOccluded(ChunkData data) {
        if (occlusionAccessorState == 2) {
            return isFullyOccludedFallback(data);
        }
        if (occlusionAccessorState == 1) {
            ChunkOcclusionData graph = ((ChunkOcclusionMixin) (Object) data).mvp$getOcclusionGraph();
            return hasNoVisiblePair(graph);
        }
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