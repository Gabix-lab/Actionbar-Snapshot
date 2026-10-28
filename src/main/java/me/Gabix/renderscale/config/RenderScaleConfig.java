package me.Gabix.renderscale.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.Gabix.renderscale.IrisCompatibility;
import me.Gabix.renderscale.RenderScale;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class RenderScaleConfig {
    public float scale = 1.0f;
    public int targetFrameRate = 0;
    public Aggression aggressionLevel = Aggression.NORMAL;
    public float minimumScale = 0.1f;
    public boolean fsr = false;
    public float irisScale = -1.0f;
    public boolean forceBilinear = false;
    public DownscaleFilter downscaleFilter = DownscaleFilter.AUTO;
    public SharpeningMode sharpeningMode = SharpeningMode.OFF;
    public int sharpeningStrength = 35;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = FabricLoader.getInstance().getConfigDir().resolve("renderscale.json").toFile();

    public static RenderScaleConfig load() {
        if (!FILE.exists()) {
            RenderScaleConfig c = new RenderScaleConfig();
            c.save();
            return c;
        }
        try (FileReader r = new FileReader(FILE)) {
            RenderScaleConfig c = GSON.fromJson(r, RenderScaleConfig.class);
            if (c == null) c = new RenderScaleConfig();
            if (c.aggressionLevel == null) c.aggressionLevel = Aggression.NORMAL;
            if (c.sharpeningMode == null) c.sharpeningMode = SharpeningMode.OFF;
            if (c.downscaleFilter == null) c.downscaleFilter = DownscaleFilter.AUTO;
            return c;
        } catch (Exception e) {
            return new RenderScaleConfig();
        }
    }

    public void save() {
        try (FileWriter w = new FileWriter(FILE)) {
            GSON.toJson(this, w);
        } catch (Exception ignored) {}
    }

    public float getScale() {
        float safe = Float.isFinite(scale) ? Math.max(0.01f, scale) : 1.0f;
        return IrisCompatibility.isShaderPackInUse()
                && Float.isFinite(irisScale) && irisScale > 0.0f ? irisScale : safe;
    }

    public boolean isDynamicScaleEnabled() {
        return targetFrameRate > 0 && !IrisCompatibility.isShaderPackInUse();
    }

    public double getMinimumScale() {
        return Math.min(getScale(), Math.max(0.01f, minimumScale));
    }

    public boolean getFilter() {
        double eff = RenderScale.getInstance() == null
                ? getScale() : RenderScale.getInstance().getRenderScaleFactor();
        return fsr || forceBilinear
                || getDownscaleFilter() == DownscaleFilter.BILINEAR
                || eff != 1.0;
    }

    public DownscaleFilter getDownscaleFilter() {
        return downscaleFilter == null ? DownscaleFilter.AUTO : downscaleFilter;
    }

    public SharpeningMode getSharpeningMode() {
        return sharpeningMode == null ? SharpeningMode.OFF : sharpeningMode;
    }

    public enum Aggression {
        CALM(25), NORMAL(50), AGGRESSIVE(80), EXTREME(100);
        public final int strength;
        Aggression(int s) { this.strength = s; }
    }

    public enum DownscaleFilter {
        AUTO, BILINEAR, RGSS, SGSS;
        public DownscaleFilter resolve(double s) {
            if (this == AUTO && s > 1.0) return s >= 3.0 ? SGSS : RGSS;
            return this;
        }
    }

    public enum SharpeningMode {
        OFF, RCAS, RCAS_DENOISE
    }
}