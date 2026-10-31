package me.Gabix.chunk;

/**
 * Per-{@code ChunkBuilder.BuiltChunk} state added by {@code BuiltChunkMixin}.
 *
 * <p>The mixin implements this interface, so every live built chunk can be
 * queried without reflection. Mixin fields are volatile because the build
 * completion callback can be observed from render-thread code while the
 * worker thread is finishing a task.</p>
 */
public interface ChunkBuildState {

    /** Wall-clock time (ms) when the chunk's GPU upload completed, or 0 if never built. */
    long mvp$getBuiltAt();

    void mvp$setBuiltAt(long value);

    /** True until this built chunk has produced its first successful build at its current origin. */
    boolean mvp$isFadeEligible();

    void mvp$setFadeEligible(boolean value);

    /** Wall-clock time (ms) when a rebuild was last dispatched (sync or async), or 0. */
    long mvp$getLastBuildAttempt();

    void mvp$setLastBuildAttempt(long value);
}
