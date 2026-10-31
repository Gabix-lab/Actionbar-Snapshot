package me.Gabix.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import me.Gabix.cooldown.CloneManager;
import me.Gabix.cooldown.CooldownTracker;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import me.Gabix.core.WindowFocusState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.Matrix4f;

import java.util.List;

public final class CustomHudRenderer {

    private static final int HOTBAR_SLOTS = 9;
    private static final int OFFHAND_INDEX = HOTBAR_SLOTS;
    private static final int CACHE_SIZE = HOTBAR_SLOTS + 1;

    private static final int BASE_SLOT_SIZE = 18;
    private static final int STAT_BOX_HEIGHT = 16;
    private static final int FPS_BOX_WIDTH = 60;
    private static final int STAT_BOX_WIDTH = 80;
    private static final int STAT_BOX_PADDING = 8;

    private static final int MAX_STAT_TEXT_BOXES = 8;

    private static final int COLOR_BAR_BG = 0x88000000;
    private static final int COLOR_LEFT_BAR = 0xCCFFFF55;
    private static final int COLOR_RIGHT_BAR = 0xCCFF55FF;

    private static final String DASH = "§7-";
    private static final String READY_CHECKMARK = "§a✔";
    private static final String READY_TEXT = "§akész";
    private static final String NO_COOLDOWN_LINE = "§7- §f| §7-";
    private static final String CLONE_COOLDOWN_PREFIX = "minecraft:paper:581";

    private static final int TOTEM_REFRESH_TICKS = 2;
    private static final int SETTINGS_REFRESH_TICKS = 20;

    private CustomHudRenderer() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null || client.player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) return;

        if (config.pauseHudOnInactive && !WindowFocusState.isFocused()) return;

        if (client.options.hudHidden) return;

        PlayerEntity player = client.player;
        int globalTick = ActionbarStorageClient.getInstance().getGlobalTickCounter();

        refreshInventoryCaches(player, config, globalTick);

        TextRenderer textRenderer = client.textRenderer;

        renderInventoryHud(matrices, client, player, config, textRenderer, globalTick);

        RECT_BATCH.reset();
        statTextBoxCount = 0;
        renderMana(matrices, textRenderer, config, HudMod.getManaManager());
        renderDamage(matrices, textRenderer, config, HudMod.getManaManager());
        renderStatBoxes(matrices, client, player, config, textRenderer, globalTick);
        RECT_BATCH.flush(matrices);
        drawStatBoxTexts(matrices, textRenderer);
    }

    private static void renderInventoryHud(MatrixStack matrices, MinecraftClient client,
                                           PlayerEntity player, StorageConfig config,
                                           TextRenderer textRenderer, int globalTick) {
        PlayerInventory inventory = player.getInventory();
        ItemStack offhandStack = player.getOffHandStack();
        boolean renderOffhand = !offhandStack.isEmpty() && config.offhandSlotEnabled;

        RECT_BATCH.reset();
        int selectedSlot = inventory.selectedSlot;

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (!config.hudSlotsEnabled[i]) continue;

            int slotX = config.hudSlotsX[i];
            int slotY = config.hudSlotsY[i];
            float scale = config.hudSlotsScale[i];
            int slotSize = (int) (BASE_SLOT_SIZE * scale);

            RECT_BATCH.add(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x80000000);

            if (i == selectedSlot) {
                RECT_BATCH.add(slotX, slotY, slotX + slotSize, slotY + 1, 0xFFFFFFFF);
                RECT_BATCH.add(slotX, slotY + slotSize - 1, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF);
                RECT_BATCH.add(slotX, slotY, slotX + 1, slotY + slotSize, 0xFFFFFFFF);
                RECT_BATCH.add(slotX + slotSize - 1, slotY, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF);
            }
        }

        if (renderOffhand) {
            int offX = config.offhandX;
            int offY = config.offhandY;
            int offSize = (int) (BASE_SLOT_SIZE * config.offhandScale);

            RECT_BATCH.add(offX, offY, offX + offSize, offY + offSize, 0x80000000);
        }

        RECT_BATCH.flush(matrices);

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (!config.hudSlotsEnabled[i]) continue;
            renderHotbarSlot(matrices, client, player, config, textRenderer, globalTick, i);
        }

        if (renderOffhand) {
            renderOffhand(matrices, client, player, config, textRenderer, globalTick, offhandStack);
        }

        RECT_BATCH.flush(matrices);
    }

    private static void renderHotbarSlot(MatrixStack matrices, MinecraftClient client,
                                         PlayerEntity player, StorageConfig config,
                                         TextRenderer textRenderer, int globalTick, int slot) {
        int slotX = config.hudSlotsX[slot];
        int slotY = config.hudSlotsY[slot];
        float scale = config.hudSlotsScale[slot];
        int slotSize = (int) (BASE_SLOT_SIZE * scale);

        ItemStack stack = player.getInventory().getStack(slot);

        if (!stack.isEmpty()) {
            renderItem(matrices, client, stack, slotX, slotY, scale);
        }

        ActionbarStorageClient.SlotSettings settings = getCachedSlotSettings(slot, stack);
        int textX = slotX + slotSize + 4;
        int textY = slotY + (int) (5 * scale);

        if (settings != null && settings.cooldownKey() != null) {
            String readyText = getReadyText(config);
            String left = getStatusTextCached(slot, false, settings.cooldownKey(),
                    settings.leftCD(), globalTick, readyText);
            String right = getStatusTextCached(slot, true, settings.cooldownKey(),
                    settings.rightCD(), globalTick, readyText);

            textRenderer.drawWithShadow(matrices, getStatusLine(slot, left, right),
                    textX, textY, 0xFFFFFF);

            if (settings.cooldownKey().startsWith(CLONE_COOLDOWN_PREFIX)) {
                String cloneText = getCloneText(globalTick);
                if (cloneText != null) {
                    textRenderer.drawWithShadow(matrices, cloneText,
                            textX, slotY + (int) (14 * scale), 0xFFFFFF);
                }
            }

            addCooldownBars(RECT_BATCH, slotX, slotY, slotSize, settings, globalTick);
        } else {
            textRenderer.drawWithShadow(matrices, NO_COOLDOWN_LINE, textX, textY, 0xFFFFFF);
        }
    }

    private static void renderOffhand(MatrixStack matrices, MinecraftClient client,
                                      PlayerEntity player, StorageConfig config,
                                      TextRenderer textRenderer, int globalTick,
                                      ItemStack offhandStack) {
        int offX = config.offhandX;
        int offY = config.offhandY;
        float offScale = config.offhandScale;
        int offSize = (int) (BASE_SLOT_SIZE * offScale);

        renderItem(matrices, client, offhandStack, offX, offY, offScale);

        ActionbarStorageClient.SlotSettings settings = getCachedSlotSettings(OFFHAND_INDEX, offhandStack);
        if (settings != null && settings.cooldownKey() != null) {
            String readyText = getReadyText(config);
            String left = getStatusTextCached(OFFHAND_INDEX, false, settings.cooldownKey(),
                    settings.leftCD(), globalTick, readyText);
            String right = getStatusTextCached(OFFHAND_INDEX, true, settings.cooldownKey(),
                    settings.rightCD(), globalTick, readyText);

            textRenderer.drawWithShadow(matrices, getStatusLine(OFFHAND_INDEX, left, right),
                    offX + offSize + 4, offY + (int) (5 * offScale), 0xFFFFAA);

            addCooldownBars(RECT_BATCH, offX, offY, offSize, settings, globalTick);
        }
    }

    private static void renderItem(MatrixStack matrices, MinecraftClient client, ItemStack stack,
                                   int slotX, int slotY, float scale) {
        matrices.push();
        matrices.translate(slotX + 1, slotY + 1, 0);
        matrices.scale(scale, scale, 1.0f);
        client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, 0, 0);
        client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, 0, 0);
        matrices.pop();
    }

    private static void renderMana(MatrixStack matrices, TextRenderer textRenderer,
                                   StorageConfig config, ManaManager manaManager) {
        if (!config.showMana) return;

        renderStatBox(matrices, textRenderer, getManaText(manaManager.getCurrentMana()),
                config.manaX, config.manaY, config.manaScale,
                STAT_BOX_WIDTH, MANA_WIDTH_CACHE);
    }

    private static void renderDamage(MatrixStack matrices, TextRenderer textRenderer,
                                     StorageConfig config, ManaManager manaManager) {
        if (!config.showDamage) return;

        renderStatBox(matrices, textRenderer,
                getDamageText(manaManager.getCurrentDamage(), config.getTargetBossDamage()),
                config.damageX, config.damageY, config.damageScale,
                STAT_BOX_WIDTH, DAMAGE_WIDTH_CACHE);
    }

    private static void renderStatBoxes(MatrixStack matrices, MinecraftClient client,
                                        PlayerEntity player, StorageConfig config,
                                        TextRenderer textRenderer, int globalTick) {
        FpsCpsCounter counter = null;
        if (config.showFps || config.showCps) {
            counter = HudMod.getFpsCpsCounter();
        }

        if (config.showFps) {
            renderStatBox(matrices, textRenderer, getFpsText(counter.getFps()),
                    config.fpsX, config.fpsY, config.fpsScale,
                    FPS_BOX_WIDTH, FPS_WIDTH_CACHE);
        }

        if (config.showCps) {
            renderStatBox(matrices, textRenderer,
                    getCpsText(counter.getLeftCps(), counter.getRightCps()),
                    config.cpsX, config.cpsY, config.cpsScale,
                    STAT_BOX_WIDTH, CPS_WIDTH_CACHE);
        }

        if (config.showArrowAps) {
            renderStatBox(matrices, textRenderer,
                    getArrowText(HudMod.getArrowCounter().getAps()),
                    config.arrowX, config.arrowY, config.arrowScale,
                    STAT_BOX_WIDTH, ARROW_WIDTH_CACHE);
        }

        if (config.showTotems) {
            int total = getTotemCount(player, globalTick);
            renderStatBox(matrices, textRenderer, getTotemText(total),
                    config.totemX, config.totemY, config.totemScale,
                    STAT_BOX_WIDTH, TOTEM_WIDTH_CACHE);
        }
    }

    private static void renderStatBox(MatrixStack matrices, TextRenderer textRenderer,
                                      String text, int x, int y, float scale,
                                      int boxWidth, WidthCache widthCache) {
        int textWidth = widthCache.get(textRenderer, text);
        int panelWidth = Math.max(boxWidth, textWidth + STAT_BOX_PADDING * 2);

        RECT_BATCH.add(x, y,
                x + Math.round(panelWidth * scale),
                y + Math.round(STAT_BOX_HEIGHT * scale),
                0x80000000);

        int textX = Math.max(0, (panelWidth - textWidth) / 2);
        int textY = Math.max(0, (STAT_BOX_HEIGHT - textRenderer.fontHeight) / 2);

        if (statTextBoxCount < MAX_STAT_TEXT_BOXES) {
            int index = statTextBoxCount++;
            statTextBoxText[index] = text;
            statTextBoxX[index] = x;
            statTextBoxY[index] = y;
            statTextBoxScale[index] = scale;
            statTextBoxTextX[index] = textX;
            statTextBoxTextY[index] = textY;
        }
    }

    private static void drawStatBoxTexts(MatrixStack matrices, TextRenderer textRenderer) {
        for (int i = 0; i < statTextBoxCount; i++) {
            float scale = statTextBoxScale[i];
            matrices.push();
            matrices.translate(
                    statTextBoxX[i] + statTextBoxTextX[i] * scale,
                    statTextBoxY[i] + statTextBoxTextY[i] * scale,
                    0);
            matrices.scale(scale, scale, 1.0f);
            textRenderer.drawWithShadow(matrices, statTextBoxText[i], 0, 0, 0xFFFFFF);
            matrices.pop();
        }
        statTextBoxCount = 0;
    }

    private static String getManaText(int mana) {
        if (mana != cachedManaValue) {
            cachedManaValue = mana;
            cachedManaText = "§bMana: " + mana;
        }
        return cachedManaText;
    }

    private static String getDamageText(int currentDamage, int targetDamage) {
        if (currentDamage != cachedDamageValue || targetDamage != cachedDamageTarget) {
            cachedDamageValue = currentDamage;
            cachedDamageTarget = targetDamage;

            String targetText = targetDamage == Integer.MAX_VALUE
                    ? "§7-"
                    : Integer.toString(targetDamage);

            String damageColor;
            if (currentDamage <= 0) {
                damageColor = "§7";
            } else if (targetDamage != Integer.MAX_VALUE && currentDamage >= targetDamage) {
                damageColor = "§a";
            } else {
                damageColor = "§f";
            }

            cachedDamageText = "§fSebzés: " + damageColor + currentDamage + " §7/ §e" + targetText;
        }
        return cachedDamageText;
    }

    private static String getFpsText(int fps) {
        if (fps != cachedFpsValue) {
            cachedFpsValue = fps;
            cachedFpsText = fps + " FPS";
        }
        return cachedFpsText;
    }

    private static String getCpsText(int leftCps, int rightCps) {
        if (leftCps != cachedLeftCps || rightCps != cachedRightCps) {
            cachedLeftCps = leftCps;
            cachedRightCps = rightCps;
            cachedCpsText = leftCps + " | " + rightCps + " CPS";
        }
        return cachedCpsText;
    }

    private static String getArrowText(int aps) {
        if (aps != cachedApsValue) {
            cachedApsValue = aps;
            cachedArrowText = "§e" + aps + " §fNyíl/mp";
        }
        return cachedArrowText;
    }

    private static String getTotemText(int total) {
        if (total != cachedTotemTextValue) {
            cachedTotemTextValue = total;
            cachedTotemText = "§d" + total + " §fTotem";
        }
        return cachedTotemText;
    }

    private static String getStatusTextCached(int cacheIndex, boolean rightSide, String key,
                                              int cooldownTicks, int currentTick, String readyText) {
        StatusCache cache = rightSide ? RIGHT_STATUS_CACHE[cacheIndex] : LEFT_STATUS_CACHE[cacheIndex];

        if (cooldownTicks <= 0) {
            cache.key = key;
            cache.cooldownTicks = cooldownTicks;
            cache.expiry = 0;
            cache.tickBucket = currentTick >> 1;
            cache.readyText = readyText;
            cache.text = DASH;
            return DASH;
        }

        int expiry = rightSide
                ? CooldownTracker.INSTANCE.getRightExpiry(key)
                : CooldownTracker.INSTANCE.getLeftExpiry(key);
        int tickBucket = currentTick >> 1;

        if (cache.key != null && cache.key.equals(key)
                && cache.cooldownTicks == cooldownTicks
                && cache.expiry == expiry
                && cache.readyText.equals(readyText)
                && cache.tickBucket == tickBucket) {
            return cache.text;
        }

        String text;
        if (expiry > currentTick) {
            double remainingSeconds = (expiry - currentTick) / 20.0;
            String color = rightSide ? "§d" : "§e";
            text = color + round1(remainingSeconds) + "s";
        } else {
            text = readyText;
        }

        cache.key = key;
        cache.cooldownTicks = cooldownTicks;
        cache.expiry = expiry;
        cache.tickBucket = tickBucket;
        cache.readyText = readyText;
        cache.text = text;
        return text;
    }

    private static String getReadyText(StorageConfig config) {
        return config.cooldownReadyText ? READY_TEXT : READY_CHECKMARK;
    }

    private static String getStatusLine(int cacheIndex, String left, String right) {
        String cached = STATUS_LINE_CACHE[cacheIndex];
        if (cached != null
                && STATUS_LINE_LEFT_CACHE[cacheIndex] == left
                && STATUS_LINE_RIGHT_CACHE[cacheIndex] == right) {
            return cached;
        }

        String line = left + " §f| " + right;
        STATUS_LINE_CACHE[cacheIndex] = line;
        STATUS_LINE_LEFT_CACHE[cacheIndex] = left;
        STATUS_LINE_RIGHT_CACHE[cacheIndex] = right;
        return line;
    }

    private static String getCloneText(int currentTick) {
        int cloneCount = CloneManager.INSTANCE.getActiveClonesCount();
        if (cloneCount < 2) {
            cloneTextCache = null;
            return null;
        }

        int expiry = CloneManager.INSTANCE.getNextExpiryTick();
        int tickBucket = currentTick >> 1;

        if (cloneTextCache != null
                && cloneTextTickBucket == tickBucket
                && cloneTextCloneCount == cloneCount
                && cloneTextExpiry == expiry) {
            return cloneTextCache;
        }

        int remainingTicks = Math.max(0, expiry - currentTick);
        float secondsLeft = remainingTicks / 20.0f;

        cloneTextCache = "§cFOGLALT §7(" + round1(secondsLeft) + "s)";
        cloneTextTickBucket = tickBucket;
        cloneTextCloneCount = cloneCount;
        cloneTextExpiry = expiry;
        return cloneTextCache;
    }

    private static String round1(double value) {
        long rounded = Math.round(value * 10);
        long whole = rounded / 10;
        long decimal = Math.abs(rounded % 10);
        return whole + "." + decimal;
    }

    private static void refreshInventoryCaches(PlayerEntity player, StorageConfig config, int currentTick) {
        PlayerInventory inventory = player.getInventory();
        int inventoryVersion = inventory.getChangeCount();

        boolean listsChanged = config.normalItems != cachedNormalItems
                || config.leveledItems != cachedLeveledItems
                || sizeOf(config.normalItems) != cachedNormalItemsSize
                || sizeOf(config.leveledItems) != cachedLeveledItemsSize;

        if (player != cachedPlayer
                || inventory != cachedInventory
                || inventoryVersion != cachedInventoryVersion
                || config != cachedConfig
                || listsChanged) {
            cachedPlayer = player;
            cachedInventory = inventory;
            cachedInventoryVersion = inventoryVersion;
            cachedConfig = config;
            cachedNormalItems = config.normalItems;
            cachedLeveledItems = config.leveledItems;
            cachedNormalItemsSize = sizeOf(config.normalItems);
            cachedLeveledItemsSize = sizeOf(config.leveledItems);

            invalidateCaches();
        }

        if (currentTick < lastSettingsRefreshTick
                || (long) currentTick - lastSettingsRefreshTick >= SETTINGS_REFRESH_TICKS) {
            invalidateSlotSettingsCaches();
            lastSettingsRefreshTick = currentTick;
        }
    }

    public static void onInventoryChanged() {
        cachedInventoryVersion = Integer.MIN_VALUE;
        invalidateCaches();
    }

    private static void invalidateCaches() {
        invalidateSlotSettingsCaches();

        for (int i = 0; i < CACHE_SIZE; i++) {
            LEFT_STATUS_CACHE[i].reset();
            RIGHT_STATUS_CACHE[i].reset();

            STATUS_LINE_CACHE[i] = null;
            STATUS_LINE_LEFT_CACHE[i] = null;
            STATUS_LINE_RIGHT_CACHE[i] = null;
        }

        totemCacheValid = false;
        lastSettingsRefreshTick = Integer.MIN_VALUE;

        cloneTextCache = null;
        cloneTextTickBucket = Integer.MIN_VALUE;
        cloneTextCloneCount = -1;
        cloneTextExpiry = -1;
    }

    private static void invalidateSlotSettingsCaches() {
        for (int i = 0; i < CACHE_SIZE; i++) {
            SLOT_CACHE_VALID[i] = false;
            SLOT_STACK_CACHE[i] = null;
            SLOT_SETTINGS_CACHE[i] = null;
        }
    }

    private static ActionbarStorageClient.SlotSettings getCachedSlotSettings(int cacheIndex, ItemStack stack) {
        if (SLOT_CACHE_VALID[cacheIndex] && SLOT_STACK_CACHE[cacheIndex] == stack) {
            return SLOT_SETTINGS_CACHE[cacheIndex];
        }

        ActionbarStorageClient.SlotSettings settings = ActionbarStorageClient.getSettingsForItem(stack);

        SLOT_STACK_CACHE[cacheIndex] = stack;
        SLOT_SETTINGS_CACHE[cacheIndex] = settings;
        SLOT_CACHE_VALID[cacheIndex] = true;
        return settings;
    }

    private static int getTotemCount(PlayerEntity player, int currentTick) {
        if (!totemCacheValid
                || currentTick < totemCacheTick
                || currentTick - totemCacheTick >= TOTEM_REFRESH_TICKS) {
            totemCountCache = countTotems(player);
            totemCacheTick = currentTick;
            totemCacheValid = true;
        }
        return totemCountCache;
    }

    private static int countTotems(PlayerEntity player) {
        int count = 0;
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static void addCooldownBars(RectBatch batch, int slotX, int slotY, int slotSize,
                                        ActionbarStorageClient.SlotSettings settings,
                                        int currentTick) {
        if (settings == null || settings.cooldownKey() == null || slotSize < 10) return;

        int leftCd = settings.leftCD();
        int rightCd = settings.rightCD();
        if (leftCd <= 0 && rightCd <= 0) return;

        String key = settings.cooldownKey();
        int leftExpiry = leftCd > 0 ? CooldownTracker.INSTANCE.getLeftExpiry(key) : 0;
        int rightExpiry = rightCd > 0 ? CooldownTracker.INSTANCE.getRightExpiry(key) : 0;

        boolean leftActive = leftCd > 0 && leftExpiry > currentTick;
        boolean rightActive = rightCd > 0 && rightExpiry > currentTick;
        if (!leftActive && !rightActive) return;

        int barLeft = slotX + 1;
        int barRight = slotX + slotSize - 1;
        int barBottom = slotY + slotSize - 1;
        int barTop = barBottom - 2;

        if (barRight <= barLeft || barTop <= slotY) return;

        batch.add(barLeft, barTop, barRight, barBottom, COLOR_BAR_BG);

        int innerWidth = barRight - barLeft;

        if (leftActive) {
            int fill = progressWidth(innerWidth, leftExpiry - currentTick, leftCd);
            batch.add(barLeft, barTop, barLeft + fill, barTop + 1, COLOR_LEFT_BAR);
        }

        if (rightActive) {
            int fill = progressWidth(innerWidth, rightExpiry - currentTick, rightCd);
            batch.add(barLeft, barTop + 1, barLeft + fill, barBottom, COLOR_RIGHT_BAR);
        }
    }

    private static int progressWidth(int innerWidth, int remainingTicks, int totalTicks) {
        float ratio = Math.min(1.0f, remainingTicks / (float) totalTicks);
        int width = (int) (innerWidth * ratio);
        return Math.max(1, Math.min(innerWidth, width));
    }

    private static final RectBatch RECT_BATCH = new RectBatch();

    private static final String[] statTextBoxText = new String[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxX = new int[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxY = new int[MAX_STAT_TEXT_BOXES];
    private static final float[] statTextBoxScale = new float[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxTextX = new int[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxTextY = new int[MAX_STAT_TEXT_BOXES];
    private static int statTextBoxCount;

    private static void addRoundedPanel(RectBatch batch, int x, int y, int width, int height,
                                        int backgroundColor, int borderColor) {
        if (width <= 2 || height <= 2) return;

        if (width < 8 || height < 8) {
            batch.add(x, y, x + width, y + height, backgroundColor);
            return;
        }

        int x2 = x + width;
        int y2 = y + height;

        int bgX1 = x + 1;
        int bgY1 = y + 1;
        int bgX2 = x2 - 1;
        int bgY2 = y2 - 1;

        batch.add(bgX1 + 1, bgY1, bgX2 - 1, bgY1 + 1, backgroundColor);
        batch.add(bgX1, bgY1 + 1, bgX2, bgY2 - 1, backgroundColor);
        batch.add(bgX1 + 1, bgY2 - 1, bgX2 - 1, bgY2, backgroundColor);

        batch.add(x + 1, y, x2 - 1, y + 1, borderColor);
        batch.add(x + 1, y2 - 1, x2 - 1, y2, borderColor);
        batch.add(x, y + 1, x + 1, y2 - 1, borderColor);
        batch.add(x2 - 1, y + 1, x2, y2 - 1, borderColor);
    }

    private static final class RectBatch {
        private static final int MAX_RECTS = 256;

        private final int[] x1 = new int[MAX_RECTS];
        private final int[] y1 = new int[MAX_RECTS];
        private final int[] x2 = new int[MAX_RECTS];
        private final int[] y2 = new int[MAX_RECTS];
        private final int[] color = new int[MAX_RECTS];
        private int count;

        void reset() {
            count = 0;
        }

        void add(int ax1, int ay1, int ax2, int ay2, int argb) {
            if (count >= MAX_RECTS || ax1 >= ax2 || ay1 >= ay2 || (argb >>> 24) == 0) {
                return;
            }
            x1[count] = ax1;
            y1[count] = ay1;
            x2[count] = ax2;
            y2[count] = ay2;
            color[count] = argb;
            count++;
        }

        void flush(MatrixStack matrices) {
            if (count == 0) return;

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int i = 0; i < count; i++) {
                int argb = color[i];
                float a = ((argb >>> 24) & 0xFF) / 255.0f;
                float r = ((argb >> 16) & 0xFF) / 255.0f;
                float g = ((argb >> 8) & 0xFF) / 255.0f;
                float b = (argb & 0xFF) / 255.0f;

                buffer.vertex(matrix, x1[i], y1[i], 0.0f).color(r, g, b, a).next();
                buffer.vertex(matrix, x1[i], y2[i], 0.0f).color(r, g, b, a).next();
                buffer.vertex(matrix, x2[i], y2[i], 0.0f).color(r, g, b, a).next();
                buffer.vertex(matrix, x2[i], y1[i], 0.0f).color(r, g, b, a).next();
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();

            count = 0;
        }
    }

    private static PlayerEntity cachedPlayer;
    private static PlayerInventory cachedInventory;
    private static int cachedInventoryVersion = Integer.MIN_VALUE;
    private static StorageConfig cachedConfig;
    private static List<StorageConfig.ItemPreset> cachedNormalItems;
    private static List<StorageConfig.LeveledItemPreset> cachedLeveledItems;
    private static int cachedNormalItemsSize = -1;
    private static int cachedLeveledItemsSize = -1;

    private static final ActionbarStorageClient.SlotSettings[] SLOT_SETTINGS_CACHE =
            new ActionbarStorageClient.SlotSettings[CACHE_SIZE];
    private static final ItemStack[] SLOT_STACK_CACHE = new ItemStack[CACHE_SIZE];
    private static final boolean[] SLOT_CACHE_VALID = new boolean[CACHE_SIZE];
    private static int lastSettingsRefreshTick = Integer.MIN_VALUE;

    private static final StatusCache[] LEFT_STATUS_CACHE = createStatusCaches();
    private static final StatusCache[] RIGHT_STATUS_CACHE = createStatusCaches();
    private static final String[] STATUS_LINE_CACHE = new String[CACHE_SIZE];
    private static final String[] STATUS_LINE_LEFT_CACHE = new String[CACHE_SIZE];
    private static final String[] STATUS_LINE_RIGHT_CACHE = new String[CACHE_SIZE];

    private static String cloneTextCache;
    private static int cloneTextTickBucket = Integer.MIN_VALUE;
    private static int cloneTextCloneCount = -1;
    private static int cloneTextExpiry = -1;

    private static int totemCountCache;
    private static int totemCacheTick = -1;
    private static boolean totemCacheValid;

    private static int cachedManaValue = Integer.MIN_VALUE;
    private static String cachedManaText = "§bMana: 0";

    private static int cachedDamageValue = Integer.MIN_VALUE;
    private static int cachedDamageTarget = Integer.MIN_VALUE;
    private static String cachedDamageText = "§fSebzés: §70 §7/ §7-";

    private static int cachedFpsValue = Integer.MIN_VALUE;
    private static String cachedFpsText = "0 FPS";

    private static int cachedLeftCps = Integer.MIN_VALUE;
    private static int cachedRightCps = Integer.MIN_VALUE;
    private static String cachedCpsText = "0 | 0 CPS";

    private static int cachedApsValue = Integer.MIN_VALUE;
    private static String cachedArrowText = "§e0 §fNyíl/mp";

    private static int cachedTotemTextValue = Integer.MIN_VALUE;
    private static String cachedTotemText = "§d0 §fTotem";

    private static final WidthCache MANA_WIDTH_CACHE = new WidthCache();
    private static final WidthCache DAMAGE_WIDTH_CACHE = new WidthCache();
    private static final WidthCache FPS_WIDTH_CACHE = new WidthCache();
    private static final WidthCache CPS_WIDTH_CACHE = new WidthCache();
    private static final WidthCache ARROW_WIDTH_CACHE = new WidthCache();
    private static final WidthCache TOTEM_WIDTH_CACHE = new WidthCache();

    private static StatusCache[] createStatusCaches() {
        StatusCache[] caches = new StatusCache[CACHE_SIZE];
        for (int i = 0; i < caches.length; i++) {
            caches[i] = new StatusCache();
        }
        return caches;
    }

    private static final class StatusCache {
        private String key;
        private int cooldownTicks = -1;
        private int expiry = -1;
        private int tickBucket = Integer.MIN_VALUE;
        private String readyText = READY_CHECKMARK;
        private String text = DASH;

        void reset() {
            key = null;
            cooldownTicks = -1;
            expiry = -1;
            tickBucket = Integer.MIN_VALUE;
            readyText = READY_CHECKMARK;
            text = DASH;
        }
    }

    private static final class WidthCache {
        private String text;
        private int width;

        int get(TextRenderer textRenderer, String value) {
            if (value != text) {
                text = value;
                width = textRenderer.getWidth(value);
            }
            return width;
        }
    }
}