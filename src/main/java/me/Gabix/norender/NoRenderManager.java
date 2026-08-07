package me.Gabix.norender;

import me.Gabix.ActionbarStorageClient;
import me.Gabix.StorageConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class NoRenderManager {
    public static boolean enabled = false;
    private static boolean isFocused = true;
    private static long fpsIdleStartTime = 0;
    private static int noRenderIdleTicks = 0;
    private static double lastX, lastY, lastZ;
    private static float lastYaw, lastPitch;
    private static boolean soundSuppressed = false;

    public static void onWindowFocusChanged(boolean focused) {
        isFocused = focused;
        MinecraftClient client = MinecraftClient.getInstance();
        StorageConfig config = ActionbarStorageClient.getConfig();

        if (focused) {
            // Immediately reset FPS limit when window gains focus for responsive uncapping
            resetFpsLimit();
            fpsIdleStartTime = 0;
            noRenderIdleTicks = 0;
            SoundSuppressor.setVolumeReduced(false, 1.0f);

            if (enabled) {
                enabled = false;
                setSoundSuppressed(client, false);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[GabixMod] §fAblak aktív - 3D Renderelés §aBEKAPCSOLVA"), true);
                }
            }
        } else {
            if (config.fpsLimitImmediateOnInactive) {
                applyFpsLimit(config.fpsLimitValue);
            } else if (config.fpsLimitOnInactive) {
                fpsIdleStartTime = System.currentTimeMillis();
            }

            float volume = config.inactiveSoundVolume;
            SoundSuppressor.setVolumeReduced(volume < 1.0f, volume);
            
            // Apply sound suppression/muting based on volume setting
            if (volume <= 0.0f && !enabled && client.getSoundManager() != null) {
                // Completely mute all sounds if volume is 0
                client.getSoundManager().stopAll();
            }

            if (config.noRenderOnInactive && !enabled) {
                enabled = true;
                setSoundSuppressed(client, true);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[GabixMod] §fAblak inaktív - 3D Renderelés §cKIKAPCSOLVA"), true);
                }
            }
        }
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();

        if (!isFocused && config.fpsLimitOnInactive && !config.fpsLimitImmediateOnInactive && fpsIdleStartTime > 0) {
            long waitMillis = (config.fpsLimitMinutes * 60L + config.fpsLimitSeconds) * 1000L;
            if (waitMillis == 0 || System.currentTimeMillis() - fpsIdleStartTime >= waitMillis) {
                applyFpsLimit(config.fpsLimitValue);
            }
        }

        handleNoRenderTimer(client, config, isFocused, hasPlayerMoved(client, player));

        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        lastYaw = player.getYaw();
        lastPitch = player.getPitch();
    }

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;
        noRenderIdleTicks = 0;
        setSoundSuppressed(client, enabled);
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

    private static void applyFpsLimit(int limit) {
        if (FpsReducerLogic.getTargetFps() != limit) {
            FpsReducerLogic.setTargetFps(limit);
        }
    }

    private static void resetFpsLimit() {
        if (FpsReducerLogic.isActive() || fpsIdleStartTime != 0) {
            FpsReducerLogic.reset();
            fpsIdleStartTime = 0;
        }
    }

    private static void handleNoRenderTimer(MinecraftClient client, StorageConfig config, boolean windowFocused, boolean playerMoved) {
        if (!config.noRenderTimerEnabled) {
            noRenderIdleTicks = 0;
            return;
        }

        if (!playerMoved && windowFocused) {
            int maxIdleTicks = ((config.noRenderMinutes * 60) + config.noRenderSeconds) * 20;
            if (maxIdleTicks == 0 || noRenderIdleTicks >= maxIdleTicks) {
                if (!enabled) {
                    enabled = true;
                    setSoundSuppressed(client, true);
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Időzítő: §c3D Renderelés KIKAPCSOLVA"), true);
                    }
                }
            } else {
                noRenderIdleTicks++;
            }
        } else {
            if (enabled && windowFocused) {
                enabled = false;
                setSoundSuppressed(client, false);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Időzítő: §a3D Renderelés BEKAPCSOLVA"), true);
                }
            }
            noRenderIdleTicks = 0;
        }
    }

    private static boolean hasPlayerMoved(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getX() != lastX || player.getY() != lastY || player.getZ() != lastZ) return true;
        if (player.getYaw() != lastYaw || player.getPitch() != lastPitch) return true;

        var options = client.options;
        return options.forwardKey.isPressed() || options.backKey.isPressed() ||
                options.leftKey.isPressed() || options.rightKey.isPressed() ||
                options.jumpKey.isPressed() || options.sneakKey.isPressed() ||
                options.attackKey.isPressed() || options.useKey.isPressed();
    }

    private static void setSoundSuppressed(MinecraftClient client, boolean suppress) {
        if (soundSuppressed == suppress) return;
        soundSuppressed = suppress;
        SoundSuppressor.setSuppress(client, suppress);
    }

    public static void onUserInput() {
        noRenderIdleTicks = 0;
    }
}