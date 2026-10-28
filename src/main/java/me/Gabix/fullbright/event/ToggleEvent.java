package me.Gabix.fullbright.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ToggleEvent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final double MAX_GAMMA = 100.0;
    public static volatile boolean isSaving = false;
    private static boolean fullbrightActive = false;
    private static boolean startupDone = false;

    public static boolean isFullbrightEnabled() {
        return fullbrightActive;
    }

    public static void startupCheck() {
        if (startupDone) return;
        if (mc.player == null || mc.options == null) return;
        fullbrightActive = true;
        startupDone = true;
    }

    public static void onHotkeyPress() {
        if (mc.currentScreen == null && mc.options != null && mc.player != null) {
            fullbrightActive = !fullbrightActive;
            String status = fullbrightActive ? "§aON" : "§cOFF";
            mc.player.sendMessage(Text.literal("§7[GabixMod] §fFullbright: " + status), true);
        }
    }
}