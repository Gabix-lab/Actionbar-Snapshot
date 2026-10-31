package me.Gabix.norender;

import me.Gabix.api.WindowFocusEvent;
import me.Gabix.api.WindowFocusListener;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import me.Gabix.core.StorageConfigSaver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicBoolean;

public class NoRenderEngine implements WindowFocusListener {
    public static volatile boolean enabled = false;
    private static volatile boolean wasAutoEnabled = false;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void init() {
        if (initialized.compareAndSet(false, true)) {
            WindowFocusEvent.register(new NoRenderEngine());
        }
    }

    @Override
    public void onFocusGained() {
        if (enabled && wasAutoEnabled) {
            enabled = false;
            wasAutoEnabled = false;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.literal("§7[GabixMod] §fAblak aktív - 3D Renderelés §aBEKAPCSOLVA"), true);
            }
        }
    }

    @Override
    public void onFocusLost() {
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config != null && config.noRenderOnInactive && !enabled) {
            enabled = true;
            wasAutoEnabled = true;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.literal("§7[GabixMod] §fAblak inaktív - 3D Renderelés §cKIKAPCSOLVA"), true);
            }
        }
    }

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;
        wasAutoEnabled = false;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("§7[GabixMod] §f3D Renderelés: " + (enabled ? "§cKIKAPCSOLVA" : "§aBEKAPCSOLVA")),
                    true);
        }
    }

    public static void toggleInactive(MinecraftClient client) {
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) return;
        config.noRenderOnInactive = !config.noRenderOnInactive;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Ablak Inaktívnál: " +
                    (config.noRenderOnInactive ? "§aBE" : "§cKI")), true);
        }
        StorageConfigSaver.requestSave(config);
    }
}