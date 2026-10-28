package me.Gabix.hud;

import me.Gabix.cooldown.CloneManager;
import me.Gabix.cooldown.CooldownTracker;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class CustomHudRenderer {
    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null || client.player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config != null && config.pauseHudOnInactive && !client.isWindowFocused()) return;

        PlayerEntity player = client.player;
        int globalTick = ActionbarStorageClient.getInstance().getGlobalTickCounter();

        if (!client.options.hudHidden) {
            for (int i = 0; i < 9; i++) {
                if (!config.hudSlotsEnabled[i]) continue;

                int slotX = config.hudSlotsX[i];
                int slotY = config.hudSlotsY[i];
                float scale = config.hudSlotsScale[i];
                int slotSize = (int) (18 * scale);

                net.minecraft.client.gui.hud.InGameHud.fill(matrices, slotX, slotY, slotX + slotSize, slotY + slotSize, 0x80000000);

                if (i == player.getInventory().selectedSlot) {
                    net.minecraft.client.gui.hud.InGameHud.fill(matrices, slotX, slotY, slotX + slotSize, slotY + 1, 0xFFFFFFFF);
                    net.minecraft.client.gui.hud.InGameHud.fill(matrices, slotX, slotY + slotSize - 1, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF);
                    net.minecraft.client.gui.hud.InGameHud.fill(matrices, slotX, slotY, slotX + 1, slotY + slotSize, 0xFFFFFFFF);
                    net.minecraft.client.gui.hud.InGameHud.fill(matrices, slotX + slotSize - 1, slotY, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF);
                }

                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    matrices.push();
                    matrices.translate(slotX + 1, slotY + 1, 0);
                    matrices.scale(scale, scale, 1.0f);
                    client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, 0, 0);
                    client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, 0, 0);
                    matrices.pop();
                }

                ActionbarStorageClient.SlotSettings settings = ActionbarStorageClient.getSlotSettings(i);
                if (settings != null && settings.cooldownKey() != null) {
                    String leftStatus = getStatusText(settings.cooldownKey(), settings.leftCD(), false, globalTick);
                    String rightStatus = getStatusText(settings.cooldownKey(), settings.rightCD(), true, globalTick);
                    String fullStatusText = leftStatus + " §f| " + rightStatus;
                    client.textRenderer.drawWithShadow(matrices, fullStatusText, slotX + slotSize + 4, slotY + (int)(5 * scale), 0xFFFFFF);

                    if (settings.cooldownKey().startsWith("minecraft:paper:581") && CloneManager.INSTANCE.getActiveClonesCount() >= 2) {
                        int nextExpiry = CloneManager.INSTANCE.getNextExpiryTick();
                        int remTicks = Math.max(0, nextExpiry - globalTick);
                        float secondsLeft = remTicks / 20.0f;
                        String foglaltStr = "§cFOGLALT §7(" + round1(secondsLeft) + "s)";
                        client.textRenderer.drawWithShadow(matrices, foglaltStr, slotX + slotSize + 4, slotY + (int)(14 * scale), 0xFFFFFF);
                    }
                } else {
                    client.textRenderer.drawWithShadow(matrices, "§7- §f| §7-", slotX + slotSize + 4, slotY + (int)(5 * scale), 0xFFFFFF);
                }
            }

            ItemStack offhandStack = player.getOffHandStack();
            if (!offhandStack.isEmpty() && config.offhandSlotEnabled) {
                int offX = config.offhandX;
                int offY = config.offhandY;
                float offScale = config.offhandScale;
                int offSlotSize = (int) (18 * offScale);

                net.minecraft.client.gui.hud.InGameHud.fill(matrices, offX, offY, offX + offSlotSize, offY + offSlotSize, 0x80000000);

                matrices.push();
                matrices.translate(offX + 1, offY + 1, 0);
                matrices.scale(offScale, offScale, 1.0f);
                client.getItemRenderer().renderInGuiWithOverrides(matrices, offhandStack, 0, 0);
                client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, offhandStack, 0, 0);
                matrices.pop();

                ActionbarStorageClient.SlotSettings offhandSettings = ActionbarStorageClient.getSettingsForItem(offhandStack);
                if (offhandSettings != null && offhandSettings.cooldownKey() != null) {
                    String leftStatus = getStatusText(offhandSettings.cooldownKey(), offhandSettings.leftCD(), false, globalTick);
                    String rightStatus = getStatusText(offhandSettings.cooldownKey(), offhandSettings.rightCD(), true, globalTick);
                    String fullStatusText = leftStatus + " §f| " + rightStatus;
                    client.textRenderer.drawWithShadow(matrices, fullStatusText, offX + offSlotSize + 4, offY + (int)(5 * offScale), 0xFFFFAA);
                }
            }

            ManaManager manaManager = HudMod.getManaManager();

            if (config.showMana) {
                matrices.push();
                matrices.translate(config.manaX, config.manaY, 0);
                matrices.scale(config.manaScale, config.manaScale, 1.0f);
                String manaText = "§bMana: " + manaManager.getCurrentMana();
                client.textRenderer.drawWithShadow(matrices, manaText, 0, 0, 0xFFFFFF);
                matrices.pop();
            }

            if (config.showDamage) {
                matrices.push();
                matrices.translate(config.damageX, config.damageY, 0);
                matrices.scale(config.damageScale, config.damageScale, 1.0f);
                int targetDmg = config.getTargetBossDamage();
                String targetStr = (targetDmg == Integer.MAX_VALUE) ? "§f§7-" : String.valueOf(targetDmg);
                int currentDmg = manaManager.getCurrentDamage();

                String damageColor;
                if (currentDmg <= 0) {
                    damageColor = "§7";
                } else if (targetDmg != Integer.MAX_VALUE && currentDmg >= targetDmg) {
                    damageColor = "§a";
                } else {
                    damageColor = "§f";
                }

                String damageText = "§fSebzés: " + damageColor + currentDmg + " §7/ §e" + targetStr;
                client.textRenderer.drawWithShadow(matrices, damageText, 0, 0, 0xFFFFFF);
                matrices.pop();
            }
        }

        FpsCpsCounter counter = HudMod.getFpsCpsCounter();
        int textHeight = client.textRenderer.fontHeight;

        if (config.showFps) {
            String fpsText = counter.getFps() + " FPS";
            int textWidth = client.textRenderer.getWidth(fpsText);
            int boxWidth = (int) (60 * config.fpsScale);
            int boxHeight = (int) (16 * config.fpsScale);

            matrices.push();
            matrices.translate(config.fpsX, config.fpsY, 0);
            matrices.scale(config.fpsScale, config.fpsScale, 1.0f);

            net.minecraft.client.gui.hud.InGameHud.fill(matrices, 0, 0, boxWidth, boxHeight, 0x77000000);
            int textX = (boxWidth - textWidth) / 2;
            int textY = (boxHeight - textHeight) / 2;
            client.textRenderer.drawWithShadow(matrices, fpsText, textX, textY, 0xFFFFFF);

            matrices.pop();
        }

        if (config.showCps) {
            String cpsText = counter.getLeftCps() + " | " + counter.getRightCps() + " CPS";
            int textWidth = client.textRenderer.getWidth(cpsText);
            int boxWidth = (int) (80 * config.cpsScale);
            int boxHeight = (int) (16 * config.cpsScale);

            matrices.push();
            matrices.translate(config.cpsX, config.cpsY, 0);
            matrices.scale(config.cpsScale, config.cpsScale, 1.0f);

            net.minecraft.client.gui.hud.InGameHud.fill(matrices, 0, 0, boxWidth, boxHeight, 0x77000000);
            int textX = (boxWidth - textWidth) / 2;
            int textY = (boxHeight - textHeight) / 2;
            client.textRenderer.drawWithShadow(matrices, cpsText, textX, textY, 0xFFFFFF);

            matrices.pop();
        }
    }

    private static String getStatusText(String key, int cooldownTicks, boolean isRight, int currentTick) {
        if (cooldownTicks <= 0) {
            return "§7-";
        }
        int expiry = isRight ? CooldownTracker.INSTANCE.getRightExpiry(key) : CooldownTracker.INSTANCE.getLeftExpiry(key);
        if (expiry > currentTick) {
            double remainingSec = (expiry - currentTick) / 20.0;
            String color = isRight ? "§d" : "§e";
            return color + round1(remainingSec) + "s";
        }
        return "§akész";
    }

    private static String round1(double value) {
        long rounded = Math.round(value * 10);
        long whole = rounded / 10;
        long decimal = Math.abs(rounded % 10);
        return whole + "." + decimal;
    }
}