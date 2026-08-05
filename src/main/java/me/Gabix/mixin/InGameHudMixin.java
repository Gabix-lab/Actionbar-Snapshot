package me.Gabix.mixin;

import me.Gabix.ActionbarStorageClient;
import me.Gabix.CloneManager;
import me.Gabix.CooldownTracker;
import me.Gabix.ManaManager;
import me.Gabix.StorageConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustomHud(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options.hudHidden || client.textRenderer == null || client.player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        PlayerEntity player = client.player;
        int globalTick = ActionbarStorageClient.getInstance().getGlobalTickCounter();
        CloneManager cloneManager = ActionbarStorageClient.getInstance().getCloneManager();

        for (int i = 0; i < 9; i++) {
            if (!config.hudSlotsEnabled[i]) continue;

            int slotX = config.hudSlotsX[i];
            int slotY = config.hudSlotsY[i];
            float scale = config.hudSlotsScale[i];
            int slotSize = (int) (18 * scale);

            InGameHud.fill(matrices, slotX, slotY, slotX + slotSize, slotY + slotSize, 0x80000000);

            if (i == player.getInventory().selectedSlot) {
                InGameHud.fill(matrices, slotX, slotY, slotX + slotSize, slotY + 1, 0xFFFFFFFF);
                InGameHud.fill(matrices, slotX, slotY + slotSize - 1, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF);
                InGameHud.fill(matrices, slotX, slotY, slotX + 1, slotY + slotSize, 0xFFFFFFFF);
                InGameHud.fill(matrices, slotX + slotSize - 1, slotY, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF);
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

                if (settings.cooldownKey().startsWith("minecraft:paper:581") && cloneManager != null && cloneManager.getActiveClonesCount() >= 2) {
                    int nextExpiry = cloneManager.getNextExpiryTick();
                    int remTicks = Math.max(0, nextExpiry - globalTick);
                    float secondsLeft = remTicks / 20.0f;
                    String foglaltStr = "§cFOGLALT §7(" + String.format("%.1f", secondsLeft) + "s)";
                    client.textRenderer.drawWithShadow(matrices, foglaltStr, slotX + slotSize + 4, slotY + (int)(14 * scale), 0xFFFFFF);
                }
            } else {
                client.textRenderer.drawWithShadow(matrices, "§7- §f| §7-", slotX + slotSize + 4, slotY + (int)(5 * scale), 0xFFFFFF);
            }
        }

        ManaManager manaManager = ActionbarStorageClient.getInstance().getManaManager();

        if (config.showMana) {
            matrices.push();
            matrices.translate(config.manaX, config.manaY, 0);
            matrices.scale(config.manaScale, config.manaScale, 1.0f);
            String manaText = String.format("§bMana: %d", manaManager.getCurrentMana());
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

    @Unique
    private String getStatusText(String key, int cooldownTicks, boolean isRight, int currentTick) {
        if (cooldownTicks <= 0) {
            return "§7-";
        }
        int expiry = isRight ? CooldownTracker.INSTANCE.getRightExpiry(key) : CooldownTracker.INSTANCE.getLeftExpiry(key);
        if (expiry > currentTick) {
            double remainingSec = (expiry - currentTick) / 20.0;
            String color = isRight ? "§d" : "§e";
            return String.format("%s%.1fs", color, remainingSec);
        }
        return "§akész";
    }
}