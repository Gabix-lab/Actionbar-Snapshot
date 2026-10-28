package me.Gabix.renderscale;

import me.Gabix.core.InactiveResolutionScale;
import me.Gabix.renderscale.accessors.MainRenderTargetSetter;
import me.Gabix.renderscale.config.RenderScaleConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

public class RenderScale {
    public static Platform PLATFORM;
    public static LegacyFsrRenderer legacyFsr;
    public static Framebuffer renderTarget;

    private static RenderScale instance;
    private static RenderScaleConfig config;

    private boolean scalingActive = false;
    private Framebuffer nativeTarget;
    private int nativeWidth;
    private int nativeHeight;
    private double currentScaleFactor = 1.0;
    private String currentMode = "native";
    private long lastSaveTime = 0;
    private long lastScaleChange = 0;
    private int zeroFpsFrames = 0;

    private static final long SAVE_INTERVAL_MS = 2000;
    private static final long SCALE_CHANGE_INTERVAL_MS = 500;

    public static void init() {
        if (PLATFORM == null) PLATFORM = new FabricPlatform();
        if (instance == null) instance = new RenderScale();
        if (legacyFsr == null) legacyFsr = new LegacyFsrRenderer();
        if (config == null) config = RenderScaleConfig.load();
    }

    public static RenderScale getInstance() {
        if (instance == null) init();
        return instance;
    }

    public static RenderScaleConfig getConfig() {
        if (config == null) config = RenderScaleConfig.load();
        return config;
    }

    public void setShouldScale(boolean v) {
        if (v) takeOver();
        else handBack();
    }

    private void takeOver() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        if (InactiveResolutionScale.isActive()) {
            currentMode = "native";
            scalingActive = false;
            return;
        }

        if (legacyFsr != null && legacyFsr.isFailed()) {
            currentMode = "native";
            scalingActive = false;
            return;
        }

        RenderScaleConfig cfg = getConfig();
        float factor = cfg.getScale();
        currentScaleFactor = factor;

        Framebuffer current = client.getFramebuffer();
        if (current != renderTarget) {
            nativeTarget = current;
        }
        if (nativeTarget != null) {
            nativeWidth = nativeTarget.viewportWidth;
            nativeHeight = nativeTarget.viewportHeight;
        }

        if (factor == 1.0f && !needsUpscale(cfg)) {
            scalingActive = false;
            currentMode = "native";
            return;
        }

        int scaledW = Math.max(1, (int) (nativeWidth * factor));
        int scaledH = Math.max(1, (int) (nativeHeight * factor));

        try {
            if (renderTarget == null
                    || renderTarget.viewportWidth != scaledW
                    || renderTarget.viewportHeight != scaledH) {
                if (renderTarget != null) renderTarget.delete();
                renderTarget = new SimpleFramebuffer(scaledW, scaledH, true, MinecraftClient.IS_SYSTEM_MAC);
            }

            if (factor < 1.0f && cfg.fsr) currentMode = "FSR1";
            else if (factor < 3.0f) currentMode = "RGSS";
            else currentMode = "SGSS";

            renderTarget.beginWrite(true);
            ((MainRenderTargetSetter) client).renderScale$setMainRenderTarget(renderTarget);
            scalingActive = true;
        } catch (Throwable t) {
            currentMode = "native";
            scalingActive = false;
            try {
                if (nativeTarget != null) nativeTarget.beginWrite(true);
            } catch (Throwable ignored) {
            }
        }
    }

    private boolean needsUpscale(RenderScaleConfig cfg) {
        return cfg.fsr || cfg.forceBilinear
                || cfg.getDownscaleFilter() != RenderScaleConfig.DownscaleFilter.AUTO;
    }

    private void handBack() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !scalingActive || nativeTarget == null) {
            scalingActive = false;
            return;
        }

        try {
            legacyFsr.blit(renderTarget, nativeTarget, getConfig());
        } catch (Throwable ignored) {
        } finally {
            try {
                nativeTarget.beginWrite(true);
            } catch (Throwable ignored) {
            }
            try {
                ((MainRenderTargetSetter) client).renderScale$setMainRenderTarget(nativeTarget);
            } catch (Throwable ignored) {
            }
            scalingActive = false;
        }
    }

    public void onResolutionChanged() {
        if (renderTarget != null) {
            renderTarget.delete();
            renderTarget = null;
        }
        if (legacyFsr != null) legacyFsr.close();
        if (nativeTarget != null) {
            nativeWidth = nativeTarget.viewportWidth;
            nativeHeight = nativeTarget.viewportHeight;
        }
    }

    public void resizeRenderTarget() {
        onResolutionChanged();
    }

    public void updateDynamicScale() {
        RenderScaleConfig cfg = getConfig();
        if (cfg == null || cfg.targetFrameRate <= 0) return;
        if (InactiveResolutionScale.isActive()) return;
        if (IrisCompatibility.isShaderPackInUse()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        int currentFps = client.getCurrentFps();
        if (currentFps <= 0) {
            zeroFpsFrames++;
            if (zeroFpsFrames < 60) return;
        } else {
            zeroFpsFrames = 0;
        }

        int target = cfg.targetFrameRate;
        double minScale = cfg.getMinimumScale();
        double current = getRenderScaleFactor();

        double newScale = current;
        if (currentFps < target * 0.9) newScale = Math.max(minScale, current - 0.02);
        else if (currentFps > target * 1.05) newScale = Math.min(cfg.getScale(), current + 0.02);

        if (Math.abs(newScale - current) > 0.001) {
            long now = System.currentTimeMillis();
            if (now - lastScaleChange < SCALE_CHANGE_INTERVAL_MS) return;
            lastScaleChange = now;

            cfg.scale = (float) newScale;
            if (now - lastSaveTime > SAVE_INTERVAL_MS) {
                cfg.save();
                lastSaveTime = now;
            }
            onResolutionChanged();
        }
    }

    public double getRenderScaleFactor() {
        return currentScaleFactor;
    }

    public String getScalingMode() {
        return currentMode;
    }

    public void close() {
        if (legacyFsr != null) legacyFsr.close();
        if (renderTarget != null) {
            renderTarget.delete();
            renderTarget = null;
        }
    }
}