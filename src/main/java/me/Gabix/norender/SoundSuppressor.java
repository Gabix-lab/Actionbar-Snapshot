package me.Gabix.norender;

import net.minecraft.client.MinecraftClient;

public class SoundSuppressor {
    private static boolean suppressed = false;
    private static boolean volumeReduced = false;
    private static float volumeMultiplier = 1.0f;

    public static void setSuppress(MinecraftClient client, boolean suppress) {
        if (suppressed == suppress) return;
        suppressed = suppress;
        if (client != null && client.getSoundManager() != null) {
            if (suppress) {
                client.getSoundManager().stopAll();
            }
        }
    }

    public static boolean isSuppressed() {
        return suppressed;
    }

    public static void setVolumeReduced(boolean reduced, float multiplier) {
        volumeReduced = reduced;
        volumeMultiplier = multiplier;
    }

    public static boolean isVolumeReduced() {
        return volumeReduced;
    }

    public static float getVolumeMultiplier() {
        return volumeMultiplier;
    }
}