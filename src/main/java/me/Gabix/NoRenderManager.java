package me.Gabix;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class NoRenderManager {
    public static boolean enabled = false;
    private static int idleTicksCount = 0;
    private static double lastX, lastY, lastZ;
    private static float lastYaw, lastPitch;
    private static boolean wasWindowFocused = true;

    public static void init(StorageConfig config) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

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

            while (ActionbarStorageClient.noRenderKeyBinding.wasPressed()) {
                toggleNoRender(client);
            }

            if (!config.noRenderOnInactive && config.noRenderTimerEnabled) {
                handleIdleDetection(client, config);
            } else if (!config.noRenderTimerEnabled) {
                idleTicksCount = 0;
            }

            wasWindowFocused = isWindowFocused;
        });
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
        if (client.options.forwardKey.isPressed() || client.options.backKey.isPressed() ||
                client.options.leftKey.isPressed() || client.options.rightKey.isPressed() ||
                client.options.jumpKey.isPressed() || client.options.sneakKey.isPressed() ||
                client.options.attackKey.isPressed() || client.options.useKey.isPressed()) {
            return true;
        }
        return false;
    }

    private static void toggleNoRender(MinecraftClient client) {
        enabled = !enabled;
        idleTicksCount = 0;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("§7[GabixMod] §f3D Renderelés: " + (enabled ? "§cKIKAPCSOLVA" : "§aBEKAPCSOLVA")),
                    true
            );
        }
    }
}