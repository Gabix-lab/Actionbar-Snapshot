package me.Gabix.fullbright.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

public class ToggleEvent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static double originalGamma = -1.0;
    public static final double MAX_GAMMA = 100.0;
    public static volatile boolean isSaving = false;
    private static boolean fullbrightActive = false;
    private static boolean startupDone = false;

    public static boolean isFullbrightEnabled() {
        return fullbrightActive;
    }

    public static double getSafeGamma() {
        double g = originalGamma;
        if (g < 0.0 || g > 1.0) g = 0.5;
        return g;
    }

    public static void startupCheck() {
        if (startupDone) return;
        if (mc.player == null || mc.options == null) return;

        SimpleOption<Double> gammaOption = mc.options.getGamma();
        double currentGamma = gammaOption.getValue();

        if (currentGamma > 1.0 || currentGamma < 0.0) {
            originalGamma = 0.5;
        } else {
            originalGamma = currentGamma;
        }

        fullbrightActive = true;
        gammaOption.setValue(MAX_GAMMA);
        startupDone = true;
    }

    public static void onHotkeyPress() {
        if (mc.currentScreen == null && mc.options != null && mc.player != null) {
            SimpleOption<Double> gammaOption = mc.options.getGamma();
            double currentGamma = gammaOption.getValue();

            if (originalGamma < 0.0) {
                originalGamma = currentGamma;
            }

            if (fullbrightActive) {
                gammaOption.setValue(originalGamma);
                fullbrightActive = false;
            } else {
                originalGamma = Math.abs(currentGamma - MAX_GAMMA) > 0.1 ? currentGamma : originalGamma;
                gammaOption.setValue(MAX_GAMMA);
                fullbrightActive = true;
            }

            String status = fullbrightActive ? "§aON" : "§cOFF";
            mc.player.sendMessage(Text.literal("§7[GabixMod] §fFullbright: " + status), true);
        }
    }
}