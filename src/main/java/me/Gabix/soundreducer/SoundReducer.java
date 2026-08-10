package me.Gabix.soundreducer;

import net.minecraft.client.MinecraftClient;

public class SoundReducer {
    private static boolean suppressed = false;

    public static void init() {
    }

    public static void onFocusChange(boolean focused) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (focused) {
            suppressed = false;
        } else {
            suppressed = true;
        }

        if (client != null && client.getSoundManager() != null) {
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