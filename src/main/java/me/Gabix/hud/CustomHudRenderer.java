package me.Gabix.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import me.Gabix.cooldown.CloneManager;
import me.Gabix.cooldown.CooldownTracker;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
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

/**
 * Egyedi HUD renderelő.
 *
 * <p>Fő optimalizálások:
 * <ul>
 *     <li>a hotbar item -> SlotSettings keresés csak inventory változáskor fut;</li>
 *     <li>a cooldown szövegek 2 tickenként (10 Hz) frissülnek, nem minden frame-ben;</li>
 *     <li>a totem szám cache-elt, csak inventory változáskor / biztonsági időközönként számolódik újra;</li>
 *     <li>minden háttér/keret egyetlen batchelt draw call-ban megy ki;</li>
 *     <li>a String-ek és szövegszélességek csak értékváltozáskor épülnek újra.</li>
 * </ul>
 */
public final class CustomHudRenderer {

    // ---------------------------------------------------------------------
    // Alap méretek / színek
    // ---------------------------------------------------------------------

    private static final int HOTBAR_SLOTS = 9;
    private static final int OFFHAND_INDEX = HOTBAR_SLOTS;
    private static final int CACHE_SIZE = HOTBAR_SLOTS + 1;

    private static final int BASE_SLOT_SIZE = 18;
    private static final int STAT_BOX_HEIGHT = 16;
    private static final int FPS_BOX_WIDTH = 60;
    private static final int STAT_BOX_WIDTH = 80;
    private static final int STAT_BOX_PADDING = 8;

    // mana + sebzés + FPS + CPS + nyíl + totem = 6 látható panel egyszerre.
    private static final int MAX_STAT_TEXT_BOXES = 8;

    // A dobozok a saját, már skálázott koordináta-rendszerükben fix méretűek.
    // A config.scale a MatrixStack.scale()-ben érvényesül, így nem duplázódik a méret.

    private static final int COLOR_BAR_BG = 0x88000000;
    private static final int COLOR_LEFT_BAR = 0xCCFFFF55;
    private static final int COLOR_RIGHT_BAR = 0xCCFF55FF;

    private static final String DASH = "§7-";
    private static final String READY_CHECKMARK = "§a✔";
    private static final String READY_TEXT = "§akész";
    private static final String NO_COOLDOWN_LINE = "§7- §f| §7-";
    private static final String CLONE_COOLDOWN_PREFIX = "minecraft:paper:581";

    // Biztonsági újraszámolás: a PlayerInventory changeCount nem minden kliensoldali
    // count-változásnál nő (pl. totem elhasználás), ezért 2 tickenként (10 Hz) még
    // egyszer megszámoljuk a totemeket. Ez még mindig 6x ritkább a frame-enkéntinél.
    private static final int TOTEM_REFRESH_TICKS = 2;

    // Ritka biztonsági újraszámolás a preset-keresésre. Az ItemStack referencia
    // változását azonnal látjuk; ez csak az in-place NBT/config preset változásokat
    // kapja el, ezért elég 1 másodpercenként.
    private static final int SETTINGS_REFRESH_TICKS = 20;

    private CustomHudRenderer() {
    }

    // ---------------------------------------------------------------------
    // Renderelés
    // ---------------------------------------------------------------------

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null || client.player == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null) return;

        // Változatlan viselkedés: fókuszvesztésnél az egész custom HUD kimarad.
        if (config.pauseHudOnInactive && !client.isWindowFocused()) return;

        // F1 (hudHidden): a vanilla HUD-dal együtt az egész custom HUD is eltűnik,
        // beleértve az FPS/CPS/nyíl/totem boxokat is.
        if (client.options.hudHidden) return;

        PlayerEntity player = client.player;
        int globalTick = ActionbarStorageClient.getInstance().getGlobalTickCounter();

        // Egyetlen helyen döntjük el, hogy változott-e az inventory.
        refreshInventoryCaches(player, config, globalTick);

        TextRenderer textRenderer = client.textRenderer;

        renderInventoryHud(matrices, client, player, config, textRenderer, globalTick);
        // A renderInventoryHud már flush-ölte a maga batch-ét (slot hátterek + cooldown barok).

        // A stat box paneleket egyetlen batch-be gyűjtjük. A szövegeket külön,
        // a flush UTÁN rajzoljuk, különben a panelek a szöveg fölé kerülnének.
        RECT_BATCH.reset();
        statTextBoxCount = 0;
        renderMana(matrices, textRenderer, config, HudMod.getManaManager());
        renderDamage(matrices, textRenderer, config, HudMod.getManaManager());
        renderStatBoxes(matrices, client, player, config, textRenderer, globalTick);
        RECT_BATCH.flush(matrices);
        drawStatBoxTexts(matrices, textRenderer);
    }

    /**
     * Hotbar + offhand + hozzájuk tartozó cooldown feliratok.
     */
    private static void renderInventoryHud(MatrixStack matrices, MinecraftClient client,
                                           PlayerEntity player, StorageConfig config,
                                           TextRenderer textRenderer, int globalTick) {
        PlayerInventory inventory = player.getInventory();
        ItemStack offhandStack = player.getOffHandStack();
        boolean renderOffhand = !offhandStack.isEmpty() && config.offhandSlotEnabled;

        // --- 1. fázis: minden háttér és keret egyetlen batchelt draw call-ban ---
        // A fill hívások számát így nem a slotok száma, hanem a draw call-ok száma korlátozza.
        RECT_BATCH.reset();
        int selectedSlot = inventory.selectedSlot;

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (!config.hudSlotsEnabled[i]) continue;

            int slotX = config.hudSlotsX[i];
            int slotY = config.hudSlotsY[i];
            float scale = config.hudSlotsScale[i];
            int slotSize = (int) (BASE_SLOT_SIZE * scale);

            // Régi stílus: sima sötét háttér, keret csak az aktív sloton.
            RECT_BATCH.add(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x80000000);

            if (i == selectedSlot) {
                // Régi stílus: 4 külön 1 px fehér vonal az aktív sloton.
                RECT_BATCH.add(slotX, slotY, slotX + slotSize, slotY + 1, 0xFFFFFFFF);                       // felső
                RECT_BATCH.add(slotX, slotY + slotSize - 1, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF); // alsó
                RECT_BATCH.add(slotX, slotY, slotX + 1, slotY + slotSize, 0xFFFFFFFF);                       // bal
                RECT_BATCH.add(slotX + slotSize - 1, slotY, slotX + slotSize, slotY + slotSize, 0xFFFFFFFF); // jobb
            }
        }

        if (renderOffhand) {
            int offX = config.offhandX;
            int offY = config.offhandY;
            int offSize = (int) (BASE_SLOT_SIZE * config.offhandScale);

            // Régi stílus: csak sötét háttér, keret nélkül.
            RECT_BATCH.add(offX, offY, offX + offSize, offY + offSize, 0x80000000);
        }

        RECT_BATCH.flush(matrices);

        // --- 2. fázis: itemek, szövegek, cooldown progress barok ---
        // A barokat külön batch gyűjti, mert csak az itemek fölé kell kerülniük.
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (!config.hudSlotsEnabled[i]) continue;
            renderHotbarSlot(matrices, client, player, config, textRenderer, globalTick, i);
        }

        if (renderOffhand) {
            renderOffhand(matrices, client, player, config, textRenderer, globalTick, offhandStack);
        }

        // Cooldown progress barok: legfeljebb egy draw call.
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

        // Cache: csak akkor hívja a preset-listás keresést, ha az inventory
        // változott, vagy a slotban lévő ItemStack referencia más lett.
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

            // A Lunar snake staff speciális clone cooldownja.
            if (settings.cooldownKey().startsWith(CLONE_COOLDOWN_PREFIX)) {
                String cloneText = getCloneText(globalTick);
                if (cloneText != null) {
                    textRenderer.drawWithShadow(matrices, cloneText,
                            textX, slotY + (int) (14 * scale), 0xFFFFFF);
                }
            }

            addCooldownBars(RECT_BATCH, slotX, slotY, slotSize, settings, globalTick);
        } else {
            // Nincs preset: statikus String, nem allokálunk frame-enként.
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
        // Az item renderelése továbbra is frame-enként szükséges; framebuffer cache
        // nélkül nem hagyható ki. A drága rész a preset keresés és a String építés,
        // azokat cache-eljük.
        matrices.push();
        matrices.translate(slotX + 1, slotY + 1, 0);
        matrices.scale(scale, scale, 1.0f);
        client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, 0, 0);
        client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, 0, 0);
        matrices.pop();
    }

    // ---------------------------------------------------------------------
    // Mana / sebzés
    // ---------------------------------------------------------------------

    /**
     * Mana ugyanabban a panel-stílusban, mint a többi stat box.
     * Nincs pop animáció: a szöveg és a scale mindig fix.
     */
    private static void renderMana(MatrixStack matrices, TextRenderer textRenderer,
                                   StorageConfig config, ManaManager manaManager) {
        if (!config.showMana) return;

        renderStatBox(matrices, textRenderer, getManaText(manaManager.getCurrentMana()),
                config.manaX, config.manaY, config.manaScale,
                STAT_BOX_WIDTH, MANA_WIDTH_CACHE);
    }

    /**
     * Sebzés ugyanabban a panel-stílusban, mint a többi stat box.
     * A panel a hosszabb szöveghez automatikusan szélesedik (lásd renderStatBox).
     */
    private static void renderDamage(MatrixStack matrices, TextRenderer textRenderer,
                                     StorageConfig config, ManaManager manaManager) {
        if (!config.showDamage) return;

        renderStatBox(matrices, textRenderer,
                getDamageText(manaManager.getCurrentDamage(), config.getTargetBossDamage()),
                config.damageX, config.damageY, config.damageScale,
                STAT_BOX_WIDTH, DAMAGE_WIDTH_CACHE);
    }

    // ---------------------------------------------------------------------
    // FPS / CPS / nyíl / totem boxok
    // ---------------------------------------------------------------------

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

        // Megjegyzés: az FPS és CPS box szándékosan külön maradt, mert a config-ban
        // külön fpsX/fpsY és cpsX/cpsY pozíciók vannak; összevonásuk eltörné a
        // HudPositionEditorScreen működését.
    }

    private static void renderStatBox(MatrixStack matrices, TextRenderer textRenderer,
                                      String text, int x, int y, float scale,
                                      int boxWidth, WidthCache widthCache) {
        int textWidth = widthCache.get(textRenderer, text);
        // A panel alapból a kért szélességű, de hosszú szövegnél (pl. sebzés)
        // automatikusan szélesedik, hogy ne lógjon ki a háttérből.
        int panelWidth = Math.max(boxWidth, textWidth + STAT_BOX_PADDING * 2);

        // Csak hozzáadjuk a közös batch-hez, NEM reset/flush. A panel abszolút
        // képernyő-koordinátába kerül, mert a flush már a popolt matrixszel fut.
        RECT_BATCH.add(x, y,
                x + Math.round(panelWidth * scale),
                y + Math.round(STAT_BOX_HEIGHT * scale),
                0x80000000);

        int textX = Math.max(0, (panelWidth - textWidth) / 2);
        int textY = Math.max(0, (STAT_BOX_HEIGHT - textRenderer.fontHeight) / 2);

        // A szöveget később, a batch kirajzolása után adjuk hozzá, hogy ne
        // takarja el a panel. Így a 6 háttér 1 draw call-ból megy ki.
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

    // ---------------------------------------------------------------------
    // Szöveg cache-ek (csak változáskor allokálnak)
    // ---------------------------------------------------------------------

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
            // Mindig rózsaszín szám + fehér felirat, 0 esetén is (eredeti viselkedés).
            cachedTotemText = "§d" + total + " §fTotem";
        }
        return cachedTotemText;
    }

    // ---------------------------------------------------------------------
    // Cooldown szöveg cache
    // ---------------------------------------------------------------------

    private static String getStatusTextCached(int cacheIndex, boolean rightSide, String key,
                                              int cooldownTicks, int currentTick, String readyText) {
        StatusCache cache = rightSide ? RIGHT_STATUS_CACHE[cacheIndex] : LEFT_STATUS_CACHE[cacheIndex];

        // Nincs beállított cooldown erre az oldalra.
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

        // Csak akkor építünk Stringet, ha a kulcs, a cooldown hossza, a lejárat,
        // a kész-jelzés stílusa vagy a 2 tickes frissítési bucket változott.
        // 10 Hz elég a 0.1 s kijelzéshez.
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

    /** A cooldown lejártakor megjelenő jelzés a config beállítása szerint. */
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

    // ---------------------------------------------------------------------
    // Inventory cache
    // ---------------------------------------------------------------------

    /**
     * Csak akkor invalidál, ha a játékos / inventory / config preset lista
     * vagy az inventory changeCount változott.
     *
     * <p>A PlayerInventory.getChangeCount() a markDirty hívásoknál nő. Kliensoldali
     * predikciós count-módosulás ezt nem feltétlenül növeli, ezért van a 2 tickes
     * totem- és a 20 tickes preset-újraszámolási fallback is.
     */
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

        // Fallback: ha valami in-place változik (NBT, preset mező) anélkül, hogy a
        // changeCount nőne, akkor is legfeljebb 1 másodpercig maradhat elavult a preset.
        if (currentTick < lastSettingsRefreshTick
                || (long) currentTick - lastSettingsRefreshTick >= SETTINGS_REFRESH_TICKS) {
            invalidateSlotSettingsCaches();
            lastSettingsRefreshTick = currentTick;
        }
    }

    /**
     * Külső hook (pl. ScreenHandler / ClientPlayerInteractionManager mixin számára).
     *
     * <p>A jelenlegi implementáció a PlayerInventory changeCount-ját figyeli, ezért
     * a hook használata nem kötelező, de azonnali invalidálást biztosít, ha valaki
     * később beköti.
     */
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

        // Ez a drága hívás: normalItems + leveledItems végigjárása és
        // Registries.ITEM.getId() minden presetnél. Csak cache miss esetén fut.
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

        // Ez már nem frame-enként fut, csak inventory változáskor vagy 1 másodpercenként.
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

    // ---------------------------------------------------------------------
    // Cooldown progress bar
    // ---------------------------------------------------------------------

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

    // ---------------------------------------------------------------------
    // Batchelt téglalap renderelés
    // ---------------------------------------------------------------------

    private static final RectBatch RECT_BATCH = new RectBatch();

    // Stat box szövegek: a paneleket egy batch-ben rajzoljuk, ezért a szöveget
    // egy későbbi pass-ra eltároljuk (fix méretű tömb, nincs frame-allokáció).
    private static final String[] statTextBoxText = new String[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxX = new int[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxY = new int[MAX_STAT_TEXT_BOXES];
    private static final float[] statTextBoxScale = new float[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxTextX = new int[MAX_STAT_TEXT_BOXES];
    private static final int[] statTextBoxTextY = new int[MAX_STAT_TEXT_BOXES];
    private static int statTextBoxCount;

    /**
     * Kerekített sarkú panel: 1 px border + sötét háttér.
     * A sarkokat 1 px levágással imitáljuk; minden téglalap egyetlen draw call-ba kerül.
     */
    private static void addRoundedPanel(RectBatch batch, int x, int y, int width, int height,
                                        int backgroundColor, int borderColor) {
        if (width <= 2 || height <= 2) return;

        // Nagyon kicsi (pl. 0.25 scale) slotnál a kerekítés nem látszik, marad az egyszerű fill.
        if (width < 8 || height < 8) {
            batch.add(x, y, x + width, y + height, backgroundColor);
            return;
        }

        int x2 = x + width;
        int y2 = y + height;

        // Háttér 1 px-rel beljebb, levágott belső sarkokkal.
        int bgX1 = x + 1;
        int bgY1 = y + 1;
        int bgX2 = x2 - 1;
        int bgY2 = y2 - 1;

        batch.add(bgX1 + 1, bgY1, bgX2 - 1, bgY1 + 1, backgroundColor);
        batch.add(bgX1, bgY1 + 1, bgX2, bgY2 - 1, backgroundColor);
        batch.add(bgX1 + 1, bgY2 - 1, bgX2 - 1, bgY2, backgroundColor);

        // 1 px border, a sarkoknál 1 px kihagyással.
        batch.add(x + 1, y, x2 - 1, y + 1, borderColor);
        batch.add(x + 1, y2 - 1, x2 - 1, y2, borderColor);
        batch.add(x, y + 1, x + 1, y2 - 1, borderColor);
        batch.add(x2 - 1, y + 1, x2, y2 - 1, borderColor);
    }

    /**
     * Egyszerű, fix méretű float/int tömbös gyűjtő. Frame-enként újrahasznosított,
     * így nincs allokáció és a sok kis fill hívás helyett egy BufferBuilder draw lesz.
     */
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

    // ---------------------------------------------------------------------
    // Állapot / cache mezők
    // ---------------------------------------------------------------------

    // Inventory cache
    private static PlayerEntity cachedPlayer;
    private static PlayerInventory cachedInventory;
    private static int cachedInventoryVersion = Integer.MIN_VALUE;
    private static StorageConfig cachedConfig;
    private static List<StorageConfig.ItemPreset> cachedNormalItems;
    private static List<StorageConfig.LeveledItemPreset> cachedLeveledItems;
    private static int cachedNormalItemsSize = -1;
    private static int cachedLeveledItemsSize = -1;

    // Slot -> SlotSettings cache
    private static final ActionbarStorageClient.SlotSettings[] SLOT_SETTINGS_CACHE =
            new ActionbarStorageClient.SlotSettings[CACHE_SIZE];
    private static final ItemStack[] SLOT_STACK_CACHE = new ItemStack[CACHE_SIZE];
    private static final boolean[] SLOT_CACHE_VALID = new boolean[CACHE_SIZE];
    private static int lastSettingsRefreshTick = Integer.MIN_VALUE;

    // Cooldown szöveg cache
    private static final StatusCache[] LEFT_STATUS_CACHE = createStatusCaches();
    private static final StatusCache[] RIGHT_STATUS_CACHE = createStatusCaches();
    private static final String[] STATUS_LINE_CACHE = new String[CACHE_SIZE];
    private static final String[] STATUS_LINE_LEFT_CACHE = new String[CACHE_SIZE];
    private static final String[] STATUS_LINE_RIGHT_CACHE = new String[CACHE_SIZE];

    // Clone cooldown szöveg cache
    private static String cloneTextCache;
    private static int cloneTextTickBucket = Integer.MIN_VALUE;
    private static int cloneTextCloneCount = -1;
    private static int cloneTextExpiry = -1;

    // Totem cache
    private static int totemCountCache;
    private static int totemCacheTick = -1;
    private static boolean totemCacheValid;

    // Stat szöveg cache-ek
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

    // Szövegszélesség cache
    private static final WidthCache MANA_WIDTH_CACHE = new WidthCache();
    private static final WidthCache DAMAGE_WIDTH_CACHE = new WidthCache();
    private static final WidthCache FPS_WIDTH_CACHE = new WidthCache();
    private static final WidthCache CPS_WIDTH_CACHE = new WidthCache();
    private static final WidthCache ARROW_WIDTH_CACHE = new WidthCache();
    private static final WidthCache TOTEM_WIDTH_CACHE = new WidthCache();

    // ---------------------------------------------------------------------
    // Segédosztályok
    // ---------------------------------------------------------------------

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
