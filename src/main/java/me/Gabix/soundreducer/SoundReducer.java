package me.Gabix.soundreducer;

import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import net.minecraft.client.MinecraftClient;

public class SoundReducer {
    private static volatile boolean suppressed = false;

    public static void onFocusChange(boolean focused) {
        MinecraftClient client = MinecraftClient.getInstance();
        StorageConfig config = ActionbarStorageClient.getConfig();

        if (config != null && config.muteOnInactive) {
            suppressed = !focused;
        } else {
            suppressed = false;
        }

        if (client != null && client.getSoundManager() != null && client.gameRenderer != null) {
            client.getSoundManager().updateListenerPosition(client.gameRenderer.getCamera());
            if (suppressed) {
                client.getSoundManager().stopAll();
            }
        }
    }

    public static boolean isSuppressed() {
        return suppressed;
    }
}