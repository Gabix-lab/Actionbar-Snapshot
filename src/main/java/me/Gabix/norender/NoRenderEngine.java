package me.Gabix.norender;

import me.Gabix.ActionbarStorageClient;
import me.Gabix.StorageConfig;
import me.Gabix.WindowFocusEvent;
import me.Gabix.WindowFocusListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class NoRenderEngine implements WindowFocusListener {
    public static boolean enabled = false;
    private static boolean isFocused = true;
    private static int noRenderIdleTicks = 0;
    private static double lastX, lastY, lastZ;
    private static float lastYaw, lastPitch;

    public static void init() {
        WindowFocusEvent.register(new NoRenderEngine());
    }

    @Override
    public void onFocusGained() {
        isFocused = true;
        noRenderIdleTicks = 0;
        if (enabled) {
            enabled = false;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.literal("§7[GabixMod] §fAblak aktív - 3D Renderelés §aBEKAPCSOLVA"), true);
            }
        }
    }

    @Override
    public void onFocusLost() {
        isFocused = false;
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config.noRenderOnInactive && !enabled) {
            enabled = true;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.literal("§7[GabixMod] §fAblak inaktív - 3D Renderelés §cKIKAPCSOLVA"), true);
            }
        }
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
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

    public static void onUserInput() {
        noRenderIdleTicks = 0;
    }
}