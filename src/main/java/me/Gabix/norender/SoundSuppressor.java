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
        // Note: Volume reduction via game settings requires accessing Minecraft internals
        // that are obfuscated. The sound system works with master volume in game options.
        // To properly reduce volume when window is inactive, you would need access wideners
        // to modify soundVolume options directly, which adds complexity.
        // Current workaround: Complete suppression (stopAll) for full muting
    }

    public static boolean isVolumeReduced() {
        return volumeReduced;
    }

    public static float getVolumeMultiplier() {
        return volumeMultiplier;
    }
}


