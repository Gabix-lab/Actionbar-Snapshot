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

    public static void init() {
        WindowFocusEvent.register(new NoRenderEngine());
    }

    @Override
    public void onFocusGained() {
        isFocused = true;
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
    }

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("§7[GabixMod] §f3D Renderelés: " + (enabled ? "§cKIKAPCSOLVA" : "§aBEKAPCSOLVA")),
                    true);
        }
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
}