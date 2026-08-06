package me.Gabix.fullbirghtnesstoggle.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

public class ToggleEvent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static double originalGamma = -1.0;
    private static final double MAX_GAMMA = 100.0;
    private static boolean startupDone = false;

    public static void startupCheck() {
        if (startupDone) return;
        if (mc.player == null || mc.options == null) return;

        SimpleOption<Double> gammaOption = mc.options.getGamma();
        double currentGamma = gammaOption.getValue();

        if (originalGamma < 0.0) {
            originalGamma = currentGamma;
        }

        if (Math.abs(currentGamma - MAX_GAMMA) > 0.1) {
            gammaOption.setValue(MAX_GAMMA);
        }

        startupDone = true;
    }

    public static void onHotkeyPress() {
        if (mc.currentScreen == null && mc.options != null) {
            SimpleOption<Double> gammaOption = mc.options.getGamma();
            double currentGamma = gammaOption.getValue();

            if (originalGamma < 0.0) {
                originalGamma = currentGamma;
            }

            if (Math.abs(currentGamma - MAX_GAMMA) < 0.1) {
                gammaOption.setValue(originalGamma);
            } else {
                if (Math.abs(currentGamma - MAX_GAMMA) > 0.1) {
                    originalGamma = currentGamma;
                }
                gammaOption.setValue(MAX_GAMMA);
            }
        }
    }
}