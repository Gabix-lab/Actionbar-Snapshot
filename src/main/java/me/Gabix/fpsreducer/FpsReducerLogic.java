package me.Gabix.fpsreducer;

public class FpsReducerLogic {
    private static volatile int targetFps = -1;

    public static void reset() {
        targetFps = -1;
    }

    public static void setTargetFps(int fps) {
        targetFps = fps;
    }

    public static int getTargetFps() {
        return targetFps;
    }

    public static boolean isActive() {
        return targetFps > 0;
    }
}