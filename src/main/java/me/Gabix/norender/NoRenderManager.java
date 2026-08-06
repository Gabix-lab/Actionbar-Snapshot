package me.Gabix.norender;

import me.Gabix.ActionbarStorageClient;
import me.Gabix.StorageConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class NoRenderManager {
    public static boolean enabled = false;
    private static int idleTicksCount = 0;
    private static double lastX, lastY, lastZ;
    private static float lastYaw, lastPitch;
    private static boolean wasWindowFocused = true;

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        boolean isWindowFocused = client.isWindowFocused();

        if (config.noRenderOnInactive) {
            if (!isWindowFocused && !enabled) {
                enabled = true;
                idleTicksCount = 0;
                client.player.sendMessage(Text.literal("§7[GabixMod] §fAblak inaktív - 3D Renderelés §cKIKAPCSOLVA"), true);
            } else if (isWindowFocused && enabled && !wasWindowFocused) {
                enabled = false;
                client.player.sendMessage(Text.literal("§7[GabixMod] §fAblak aktív - 3D Renderelés §aBEKAPCSOLVA"), true);
            }
        }

        if (!config.noRenderOnInactive && config.noRenderTimerEnabled) {
            handleIdleDetection(client, config);
        } else if (!config.noRenderTimerEnabled) {
            idleTicksCount = 0;
        }

        wasWindowFocused = isWindowFocused;
    }

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;
        idleTicksCount = 0;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("§7[GabixMod] §f3D Renderelés: " + (enabled ? "§cKIKAPCSOLVA" : "§aBEKAPCSOLVA")),
                    true);
        }
    }

    public static void toggleTimer(MinecraftClient client) {
        StorageConfig config = ActionbarStorageClient.getConfig();
        config.noRenderTimerEnabled = !config.noRenderTimerEnabled;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Időzítő: " +
                    (config.noRenderTimerEnabled ? "§aBE" : "§cKI")), true);
        }
        config.save();
    }

    public static void toggleInactive(MinecraftClient client) {
        StorageConfig config = ActionbarStorageClient.getConfig();
        config.noRenderOnInactive = !config.noRenderOnInactive;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Ablak Inaktívnál: " +
                    (config.noRenderOnInactive ? "§aBE" : "§cKI")), true);
        }
        config.save();
    }

    private static void handleIdleDetection(MinecraftClient client, StorageConfig config) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        boolean moved = hasPlayerMoved(client, player);
        if (moved) {
            idleTicksCount = 0;
            if (enabled) {
                enabled = false;
                player.sendMessage(Text.literal("§7[GabixMod] §fAktivitás érzékelve - 3D Renderelés §aBEKAPCSOLVA"), true);
            }
        } else {
            idleTicksCount++;
            int maxIdleTicks = ((config.noRenderMinutes * 60) + config.noRenderSeconds) * 20;
            if (maxIdleTicks > 0 && idleTicksCount >= maxIdleTicks && !enabled) {
                enabled = true;
                player.sendMessage(Text.literal("§7[GabixMod] §fInaktivitás (" + config.noRenderMinutes + "m " + config.noRenderSeconds + "s) - 3D Renderelés §cKIKAPCSOLVA"), true);
            }
        }
        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        lastYaw = player.getYaw();
        lastPitch = player.getPitch();
    }

    private static boolean hasPlayerMoved(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getX() != lastX || player.getY() != lastY || player.getZ() != lastZ) return true;
        if (player.getYaw() != lastYaw || player.getPitch() != lastPitch) return true;
        return client.options.forwardKey.isPressed() || client.options.backKey.isPressed() ||
                client.options.leftKey.isPressed() || client.options.rightKey.isPressed() ||
                client.options.jumpKey.isPressed() || client.options.sneakKey.isPressed() ||
                client.options.attackKey.isPressed() || client.options.useKey.isPressed();
    }
}