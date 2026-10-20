package me.Gabix.fpsreducer;

import me.Gabix.api.WindowFocusEvent;
import me.Gabix.api.WindowFocusListener;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;

import java.util.concurrent.atomic.AtomicBoolean;

public class FpsManager implements WindowFocusListener {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static boolean isFocused = true;
    private static long fpsIdleStartTime = 0;

    public static void init() {
        if (initialized.compareAndSet(false, true)) {
            WindowFocusEvent.register(new FpsManager());
        }
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
        if (config == null) return;
        if (!config.fpsLimitOnInactive) return;

        if (config.fpsLimitImmediateOnInactive) {
            FpsReducerLogic.setTargetFps(config.fpsLimitValue);
        } else {
            fpsIdleStartTime = System.currentTimeMillis();
        }
    }

    public static void tick() {
        if (isFocused) return;
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) return;
        if (config.fpsLimitOnInactive && !config.fpsLimitImmediateOnInactive
                && fpsIdleStartTime > 0) {
            long waitMillis = (config.fpsLimitMinutes * 60L + config.fpsLimitSeconds) * 1000L;
            if (waitMillis == 0 || System.currentTimeMillis() - fpsIdleStartTime >= waitMillis) {
                FpsReducerLogic.setTargetFps(config.fpsLimitValue);
            }
        }
    }
}