package me.Gabix.soundreducer;

import net.minecraft.client.MinecraftClient;

public class SoundReducer {
    private static boolean suppressed = false;

    public static void onFocusChange(boolean focused) {
        MinecraftClient client = MinecraftClient.getInstance();

        suppressed = !focused;

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