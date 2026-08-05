package me.Gabix;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawableHelper;

public class StorageHudRenderer {
    public static void render(MatrixStack matrices, StorageConfig config, CooldownTracker tracker, ManaManager manaManager, int globalTickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;
        ItemRenderer itemRenderer = client.getItemRenderer();
        PlayerInventory inventory = client.player.getInventory();
        int height = client.getWindow().getScaledHeight();

        int startX = 5;
        int slotSize = 20;
        int totalHotbarHeight = 9 * slotSize;
        int startY = (height - totalHotbarHeight) / 2;

        CloneManager cloneManager = ActionbarStorageClient.getInstance().getCloneManager();

        for (int i = 0; i < 9; i++) {
            if (!config.hudSlotsEnabled[i]) continue;

            int slotY = startY + i * slotSize;
            ItemStack stack = inventory.getStack(i);

            DrawableHelper.fill(matrices, startX, slotY, startX + slotSize, slotY + slotSize, 0x66000000);

            if (inventory.selectedSlot == i) {
                DrawableHelper.fill(matrices, startX, slotY, startX + slotSize, slotY + 1, 0xFFFFFFFF);
                DrawableHelper.fill(matrices, startX, slotY + slotSize - 1, startX + slotSize, slotY + slotSize, 0xFFFFFFFF);
                DrawableHelper.fill(matrices, startX, slotY, startX + 1, slotY + slotSize, 0xFFFFFFFF);
                DrawableHelper.fill(matrices, startX + slotSize - 1, slotY, startX + slotSize, slotY + slotSize, 0xFFFFFFFF);
            }

            if (!stack.isEmpty()) {
                itemRenderer.renderInGuiWithOverrides(matrices, stack, startX + 2, slotY + 2);
                itemRenderer.renderGuiItemOverlay(matrices, textRenderer, stack, startX + 2, slotY + 2);
            }

            ActionbarStorageClient.SlotSettings settings = ActionbarStorageClient.getSlotSettings(i);
            if (settings != null && settings.cooldownKey() != null) {
                String key = settings.cooldownKey();
                String leftStr = "§7-";
                String rightStr = "§7-";

                if (settings.leftCD() > 0) {
                    int leftExpiry = tracker.getLeftExpiry(key);
                    if (leftExpiry > globalTickCounter) {
                        float sec = (leftExpiry - globalTickCounter) / 20.0f;
                        leftStr = "§e" + String.format("%.1f", sec);
                    } else {
                        leftStr = "§akész";
                    }
                }
                if (settings.rightCD() > 0) {
                    int rightExpiry = tracker.getRightExpiry(key);
                    if (rightExpiry > globalTickCounter) {
                        float sec = (rightExpiry - globalTickCounter) / 20.0f;
                        rightStr = "§d" + String.format("%.1f", sec);
                    } else {
                        rightStr = "§akész";
                    }
                }

                textRenderer.drawWithShadow(matrices, leftStr + " §f| " + rightStr, startX + slotSize + 4, slotY + 2, 0xFFFFFF);

                if (key.startsWith("minecraft:paper:581") && cloneManager != null && cloneManager.getActiveClonesCount() >= 2) {
                    int nextExpiry = cloneManager.getNextExpiryTick();
                    int remTicks = Math.max(0, nextExpiry - globalTickCounter);
                    float secLeft = remTicks / 20.0f;
                    String foglaltStr = "§cFOGLALT §7(" + String.format("%.1f", secLeft) + "s)";
                    textRenderer.drawWithShadow(matrices, foglaltStr, startX + slotSize + 4, slotY + 11, 0xFFFFFF);
                }
            } else {
                textRenderer.drawWithShadow(matrices, "§7- §f| §7-", startX + slotSize + 4, slotY + 6, 0xFFFFFF);
            }
        }

        int infoY = startY + totalHotbarHeight + 4;
        textRenderer.drawWithShadow(matrices, "Mana: " + manaManager.getCurrentMana(), startX, infoY, 0x55FFFF);

        int currentDmg = ActionbarReader.currentDamage;
        int targetDmg = config != null ? config.getTargetBossDamage() : 5000;
        boolean hasActiveBossProfile = config != null && config.selectedBossNotification != null
                && !config.selectedBossNotification.isEmpty() && !config.selectedBossNotification.equalsIgnoreCase("Ki")
                && targetDmg != Integer.MAX_VALUE;

        String dmgText;
        if (currentDmg <= 0) {
            dmgText = hasActiveBossProfile ? "Sebzés: §70§7 / §e" + targetDmg : "Sebzés: §70 / -";
        } else if (hasActiveBossProfile) {
            String dmgColor = currentDmg >= targetDmg ? "§a" : "§f";
            dmgText = "Sebzés: " + dmgColor + currentDmg + "§7 / §e" + targetDmg;
        } else {
            dmgText = "Sebzés: §f" + currentDmg + "§7 / -";
        }
        textRenderer.drawWithShadow(matrices, dmgText, startX, infoY + 10, 0xFFFFFF);
    }
}