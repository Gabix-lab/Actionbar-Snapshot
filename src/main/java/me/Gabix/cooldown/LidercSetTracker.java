package me.Gabix.cooldown;

import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class LidercSetTracker {
    public static final LidercSetTracker INSTANCE = new LidercSetTracker();

    private int cooldownExpiryTick = 0;
    private boolean wasSneaking = false;

    public void tick(MinecraftClient client, int currentTick) {
        if (client == null || client.player == null || client.options == null) {
            wasSneaking = false;
            return;
        }

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) return;

        boolean isSneaking = client.options.sneakKey.isPressed();
        boolean setEquipped = isFullSetEquipped(client.player);

        if (setEquipped && isSneaking && !wasSneaking && currentTick >= cooldownExpiryTick) {
            cooldownExpiryTick = currentTick + Math.max(20, config.lidercCooldownTicks);
        }

        wasSneaking = isSneaking;
    }

    public int getRemainingTicks(int currentTick) {
        return Math.max(0, cooldownExpiryTick - currentTick);
    }

    public boolean hasCooldownActive(int currentTick) {
        return cooldownExpiryTick > currentTick;
    }

    public void reset() {
        cooldownExpiryTick = 0;
        wasSneaking = false;
    }

    public boolean isFullSetEquipped(PlayerEntity player) {
        if (player == null) return false;
        return isHelmetPiece(player.getInventory().getArmorStack(3))
                && isBodyPiece(player.getInventory().getArmorStack(2))
                && isBodyPiece(player.getInventory().getArmorStack(1))
                && isBodyPiece(player.getInventory().getArmorStack(0));
    }

    private boolean isHelmetPiece(ItemStack stack) {
        return matchesName(stack, "Lidércvadász")
                || matchesName(stack, "Allay Plüssfej")
                || matchesName(stack, "Védelmi Maszk");
    }

    private boolean isBodyPiece(ItemStack stack) {
        return matchesName(stack, "Lidércvadász");
    }

    private boolean matchesName(ItemStack stack, String keyword) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getName().getString();
        if (name == null || name.isEmpty()) return false;
        return stripFormatting(name).contains(keyword);
    }

    private static String stripFormatting(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < s.length()) {
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}