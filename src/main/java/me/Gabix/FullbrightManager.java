package me.Gabix;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class FullbrightManager {
    public static boolean enabled = true;

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[GabixMod] §fFullbright: " +
                    (enabled ? "§aBE" : "§cKI")), true);
        }
    }
}