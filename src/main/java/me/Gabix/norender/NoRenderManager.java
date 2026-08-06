package me.Gabix.norender;

import me.Gabix.ActionbarStorageClient;
import me.Gabix.StorageConfig;
import me.Gabix.norender.mixin.WindowAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class NoRenderManager {
    public static boolean enabled = false;
    private static boolean wasWindowFocused = true;
    private static int originalFpsLimit = -1;
    private static boolean fpsLimitApplied = false;
    private static int fpsIdleTicks = 0;
    private static int noRenderIdleTicks = 0;
    private static double lastX, lastY, lastZ;
    private static float lastYaw, lastPitch;

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        boolean isWindowFocused = client.isWindowFocused();
        boolean playerMoved = hasPlayerMoved(client, client.player);

        handleFpsLimit(client, config, isWindowFocused, playerMoved);
        handleNoRenderTimer(client, config, isWindowFocused, playerMoved);

        if (config.noRenderOnInactive) {
            if (!isWindowFocused && !enabled) {
                enabled = true;
                client.player.sendMessage(Text.literal("§7[GabixMod] §fAblak inaktív - 3D Renderelés §cKIKAPCSOLVA"), true);
            } else if (isWindowFocused && enabled && !wasWindowFocused) {
                enabled = false;
                client.player.sendMessage(Text.literal("§7[GabixMod] §fAblak aktív - 3D Renderelés §aBEKAPCSOLVA"), true);
            }
        }

        lastX = client.player.getX();
        lastY = client.player.getY();
        lastZ = client.player.getZ();
        lastYaw = client.player.getYaw();
        lastPitch = client.player.getPitch();
        wasWindowFocused = isWindowFocused;
    }

    public static void onWindowFocusGained() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (fpsLimitApplied) {
                WindowAccessor windowAccessor = (WindowAccessor) (Object) client.getWindow();
                if (originalFpsLimit >= 0) {
                    windowAccessor.setFramerateLimit(originalFpsLimit);
                }
                fpsLimitApplied = false;
            }
            fpsIdleTicks = 0;
            noRenderIdleTicks = 0;
            if (enabled) {
                enabled = false;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[GabixMod] §fAblak aktív - 3D Renderelés §aBEKAPCSOLVA"), true);
                }
            }
        });
    }

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;
        noRenderIdleTicks = 0;
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

    private static void handleFpsLimit(MinecraftClient client, StorageConfig config, boolean isWindowFocused, boolean playerMoved) {
        WindowAccessor windowAccessor = (WindowAccessor) (Object) client.getWindow();

        if (!config.fpsLimitOnInactive && !config.fpsLimitImmediateOnInactive) {
            if (fpsLimitApplied) {
                if (originalFpsLimit >= 0) {
                    windowAccessor.setFramerateLimit(originalFpsLimit);
                }
                fpsLimitApplied = false;
            }
            fpsIdleTicks = 0;
            return;
        }

        if (config.fpsLimitImmediateOnInactive && !isWindowFocused) {
            if (!fpsLimitApplied) {
                originalFpsLimit = windowAccessor.getFramerateLimit();
                windowAccessor.setFramerateLimit(config.fpsLimitValue);
                fpsLimitApplied = true;
            }
            return;
        }

        if (config.fpsLimitOnInactive) {
            if (!playerMoved && isWindowFocused) {
                int maxIdleTicks = ((config.fpsLimitMinutes * 60) + config.fpsLimitSeconds) * 20;
                if (maxIdleTicks == 0 || fpsIdleTicks >= maxIdleTicks) {
                    if (!fpsLimitApplied) {
                        originalFpsLimit = windowAccessor.getFramerateLimit();
                        windowAccessor.setFramerateLimit(config.fpsLimitValue);
                        fpsLimitApplied = true;
                    }
                } else {
                    fpsIdleTicks++;
                }
            } else {
                if (fpsLimitApplied) {
                    windowAccessor.setFramerateLimit(originalFpsLimit);
                    fpsLimitApplied = false;
                }
                fpsIdleTicks = 0;
            }
        }
    }

    private static void handleNoRenderTimer(MinecraftClient client, StorageConfig config, boolean isWindowFocused, boolean playerMoved) {
        if (!config.noRenderTimerEnabled) {
            noRenderIdleTicks = 0;
            return;
        }

        if (!playerMoved && isWindowFocused) {
            int maxIdleTicks = ((config.noRenderMinutes * 60) + config.noRenderSeconds) * 20;
            if (maxIdleTicks == 0 || noRenderIdleTicks >= maxIdleTicks) {
                if (!enabled) {
                    enabled = true;
                    client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Időzítő: §c3D Renderelés KIKAPCSOLVA"), true);
                }
            } else {
                noRenderIdleTicks++;
            }
        } else {
            if (enabled) {
                enabled = false;
                client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Időzítő: §a3D Renderelés BEKAPCSOLVA"), true);
            }
            noRenderIdleTicks = 0;
        }
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