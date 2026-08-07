package me.Gabix.norender;

import java.util.concurrent.locks.LockSupport;

public class FpsReducerLogic {
    private static volatile int targetFps = -1;
    private static long lastFrameTime = System.nanoTime();

    public static void setTargetFps(int fps) {
        targetFps = fps;
    }

    public static int getTargetFps() {
        return targetFps;
    }

    public static void reset() {
        targetFps = -1;
    }

    public static boolean isActive() {
        return targetFps > 0;
    }

    public static boolean shouldSkipFrame() {
        if (!isActive()) return false;

        long now = System.nanoTime();
        long frameDelayNanos = 1_000_000_000L / targetFps;
        long elapsed = now - lastFrameTime;

        if (elapsed < frameDelayNanos) {
            long parkNanos = frameDelayNanos - elapsed;
            if (parkNanos > 1_000_000L) {
                LockSupport.parkNanos(parkNanos);
            }
            return true;
        }

        lastFrameTime = now;
        return false;
    }
}