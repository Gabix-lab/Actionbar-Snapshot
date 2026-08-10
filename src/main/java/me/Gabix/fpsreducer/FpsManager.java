package me.Gabix.fpsreducer;

import me.Gabix.ActionbarStorageClient;
import me.Gabix.StorageConfig;
import me.Gabix.WindowFocusEvent;
import me.Gabix.WindowFocusListener;

public class FpsManager implements WindowFocusListener {
    private static boolean isFocused = true;
    private static long fpsIdleStartTime = 0;

    public static void init() {
        WindowFocusEvent.register(new FpsManager());
    }

    @Override
    public void onFocusGained() {
        FpsReducerLogic.reset();
        fpsIdleStartTime = 0;
        isFocused = true;
    }

    @Override
    public void onFocusLost() {
        isFocused = false;
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config.fpsLimitImmediateOnInactive) {
            FpsReducerLogic.setTargetFps(config.fpsLimitValue);
        } else if (config.fpsLimitOnInactive) {
            fpsIdleStartTime = System.currentTimeMillis();
        }
    }

    public static void tick() {
        if (isFocused) return;
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config.fpsLimitOnInactive && !config.fpsLimitImmediateOnInactive
                && fpsIdleStartTime > 0) {
            long waitMillis = (config.fpsLimitMinutes * 60L + config.fpsLimitSeconds) * 1000L;
            if (waitMillis == 0 || System.currentTimeMillis() - fpsIdleStartTime >= waitMillis) {
                FpsReducerLogic.setTargetFps(config.fpsLimitValue);
            }
        }
    }
}