package me.Gabix.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import me.Gabix.api.ConfigProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageConfig implements ConfigProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("ab-config.json").toFile();
    private static final StorageConfig DEFAULT_INSTANCE = new StorageConfig();

    public boolean[] hudSlotsEnabled = {true, true, true, true, true, true, true, true, true};
    public transient List<ItemPreset> normalItems;
    public transient List<LeveledItemPreset> leveledItems;
    public transient String selectedBossNotification = "Ki";
    public boolean autoBossDetection = true;
    public boolean showMana = true;
    public boolean showDamage = true;
    public boolean noRenderOnInactive = true;
    public boolean fpsLimitImmediateOnInactive = false;
    public boolean fpsLimitOnInactive = true;
    public int fpsLimitMinutes = 0;
    public int fpsLimitSeconds = 0;
    public int fpsLimitValue = 10;
    public int manaX = 0;
    public int manaY = 194;
    public float manaScale = 1.0f;
    public int damageX = 0;
    public int damageY = 210;
    public float damageScale = 1.0f;
    public int[] hudSlotsX = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    public int[] hudSlotsY = {32, 50, 68, 86, 104, 122, 140, 158, 176};
    public float[] hudSlotsScale = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    public boolean hideBlockBreakingParticles = false;
    public boolean hideFireOverlay = false;
    public int fireOverlayHeight = 100;
    public boolean muteOnInactive = false;
    public boolean showFps = true;
    public boolean showCps = true;
    public int fpsX = 0;
    public int fpsY = 0;
    public int cpsX = 0;
    public int cpsY = 16;
    public float fpsScale = 1.0f;
    public float cpsScale = 1.0f;
    public boolean motionBlurEnabled = false;
    public boolean offhandSlotEnabled = true;
    public int offhandX = 62;
    public int offhandY = 32;
    public float offhandScale = 1.0f;

    public StorageConfig() {
        this.selectedBossNotification = "Ki";
        this.normalItems = createDefaultNormalItems();
        this.leveledItems = createDefaultLeveledItems();
        this.motionBlurEnabled = false;
    }

    @Override
    public int getTargetBossDamage() {
        if (selectedBossNotification == null) return Integer.MAX_VALUE;
        switch (selectedBossNotification) {
            case "Jégsárkány": return 6000;
            case "Trex": return 20000;
            case "Endermágus": return 3000;
            case "Warden": return 10000;
            case "RadioPók": return 15000;
            case "Kraken": return 8000;
            case "Nyálkakirály": return 30000;
            default: return Integer.MAX_VALUE;
        }
    }

    private List<ItemPreset> createDefaultNormalItems() {
        List<ItemPreset> list = new ArrayList<>(8);
        list.add(new ItemPreset("Tiger blade", "minecraft:paper", 524, 0, 800, 0, 15, 1, 0));
        list.add(new ItemPreset("Warden staff", "minecraft:paper", 550, 14, 0, 1, 0, 0, 0));
        list.add(new ItemPreset("Kraken gun", "minecraft:paper", 10658, 0, 28, 0, 0, 1, 0));
        list.add(new ItemPreset("Skeleton dragon staff", "minecraft:paper", 10637, 10, 600, 2, 12, 2, 0));
        list.add(new ItemPreset("Lunar horseshoe staff", "minecraft:paper", 10641, 10, 160, 2, 15, 2, 0));
        list.add(new ItemPreset("Easter slingshot", "minecraft:paper", 585, 600, 10, 8, 0, 2, 0));
        list.add(new ItemPreset("Lunar snake staff", "minecraft:paper", 581, 10, 26, 1, 15, 2, 2));
        return list;
    }

    private List<LeveledItemPreset> createDefaultLeveledItems() {
        List<LeveledItemPreset> list = new ArrayList<>(4);
        LeveledItemPreset amethystSword = new LeveledItemPreset("Amethyst sword", "minecraft:paper", 562, 0, 800, 0, 15, 1, 0);
        amethystSword.levels.add(new ItemLevel("- 2. szint", 0, 700, 0, 14));
        amethystSword.levels.add(new ItemLevel("- 3. szint", 0, 700, 0, 14));
        amethystSword.levels.add(new ItemLevel("- 4. szint", 0, 600, 0, 12));
        amethystSword.levels.add(new ItemLevel("- MAXIMUM SZINT", 0, 600, 0, 12));
        list.add(amethystSword);

        LeveledItemPreset amethystStaff = new LeveledItemPreset("Amethyst staff", "minecraft:paper", 569, 20, 1200, 2, 18, 1, 0);
        amethystStaff.levels.add(new ItemLevel("- 2. szint", 20, 1000, 2, 18));
        amethystStaff.levels.add(new ItemLevel("- 3. szint", 17, 1000, 1, 18));
        amethystStaff.levels.add(new ItemLevel("- 4. szint", 17, 1000, 1, 15));
        amethystStaff.levels.add(new ItemLevel("- MAXIMUM SZINT", 14, 900, 1, 15));
        list.add(amethystStaff);

        LeveledItemPreset healStaff = new LeveledItemPreset("Heal staff", "minecraft:paper", 566, 10, 160, 2, 10, 2, 0);
        healStaff.levels.add(new ItemLevel("- 2. szint", 10, 160, 2, 9));
        healStaff.levels.add(new ItemLevel("- 3. szint", 10, 160, 2, 9));
        healStaff.levels.add(new ItemLevel("- 4. szint", 10, 140, 2, 9));
        healStaff.levels.add(new ItemLevel("- MAXIMUM SZINT", 10, 140, 2, 8));
        list.add(healStaff);

        LeveledItemPreset honeySword = new LeveledItemPreset("Honey Sword", "minecraft:diamond_sword", 567, 0, 40, 0, 5, 1, 0);
        honeySword.levels.add(new ItemLevel("- 2. szint", 0, 40, 0, 5));
        honeySword.levels.add(new ItemLevel("- 3. szint", 0, 40, 0, 5));
        honeySword.levels.add(new ItemLevel("- 4. szint", 0, 40, 0, 4));
        honeySword.levels.add(new ItemLevel("- MAXIMUM SZINT", 0, 40, 0, 4));
        list.add(honeySword);

        LeveledItemPreset radioactiveBow = new LeveledItemPreset("Radioactive Bow", "minecraft:bow", 8, 1200, 0, 10, 0, 0, 0);
        radioactiveBow.levels.add(new ItemLevel("- 2. szint", 1200, 0, 10, 0));
        radioactiveBow.levels.add(new ItemLevel("- 3. szint", 1200, 0, 10, 0));
        radioactiveBow.levels.add(new ItemLevel("- 4. szint", 1200, 0, 10, 0));
        radioactiveBow.levels.add(new ItemLevel("- MAXIMUM SZINT", 1200, 0, 10, 0));
        list.add(radioactiveBow);

        LeveledItemPreset staffInjection = new LeveledItemPreset("✚ Injekciós Pálca", "minecraft:blaze_rod", 10844, 20, 200, 4, 15, 2, 0);
        staffInjection.levels.add(new ItemLevel("- 2. szint", 20, 200, 5, 15));
        list.add(staffInjection);

        return list;
    }

    public static StorageConfig load() {
        if (!CONFIG_FILE.exists()) {
            StorageConfig defaultConfig = new StorageConfig();
            defaultConfig.save();
            return defaultConfig;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            StorageConfig loaded = GSON.fromJson(reader, StorageConfig.class);
            if (loaded != null) {
                // Csak a tömb hosszakat validáljuk
                if (loaded.hudSlotsX == null || loaded.hudSlotsX.length < 9)
                    loaded.hudSlotsX = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
                if (loaded.hudSlotsY == null || loaded.hudSlotsY.length < 9)
                    loaded.hudSlotsY = new int[]{32, 50, 68, 86, 104, 122, 140, 158, 176};
                if (loaded.hudSlotsScale == null || loaded.hudSlotsScale.length < 9)
                    loaded.hudSlotsScale = new float[]{1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f};
                if (loaded.hudSlotsEnabled == null || loaded.hudSlotsEnabled.length < 9)
                    loaded.hudSlotsEnabled = new boolean[]{true, true, true, true, true, true, true, true, true};

                // Preset-ek: csak akkor inicializáljuk, ha null vagy üres
                if (loaded.normalItems == null || loaded.normalItems.isEmpty())
                    loaded.normalItems = DEFAULT_INSTANCE.normalItems;
                if (loaded.leveledItems == null || loaded.leveledItems.isEmpty())
                    loaded.leveledItems = DEFAULT_INSTANCE.leveledItems;

                // NEM írjuk felül a user beállítását!
                if (loaded.selectedBossNotification == null)
                    loaded.selectedBossNotification = "Ki";

                if (loaded.offhandScale <= 0.0f) {
                    loaded.offhandSlotEnabled = true;
                    loaded.offhandX = 62;
                    loaded.offhandY = 32;
                    loaded.offhandScale = 1.0f;
                }
                return loaded;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new StorageConfig();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ItemLevel {
        public String levelNameIndicator;
        public int leftCD;
        public int rightCD;
        public int leftMana;
        public int rightMana;
        public ItemLevel() {}
        public ItemLevel(String levelNameIndicator, int leftCD, int rightCD, int leftMana, int rightMana) {
            this.levelNameIndicator = levelNameIndicator;
            this.leftCD = leftCD;
            this.rightCD = rightCD;
            this.leftMana = leftMana;
            this.rightMana = rightMana;
        }
    }

    public static class ItemPreset {
        public String name;
        public String itemId;
        public int customModelData;
        public int leftCD;
        public int rightCD;
        public int leftMana;
        public int rightMana;
        public int autoMode;
        public int maxCharges;
        public ItemPreset() {}
        public ItemPreset(String name, String itemId, int customModelData, int leftCD, int rightCD, int leftMana, int rightMana, int autoMode, int maxCharges) {
            this.name = name;
            this.itemId = itemId;
            this.customModelData = customModelData;
            this.leftCD = leftCD;
            this.rightCD = rightCD;
            this.leftMana = leftMana;
            this.rightMana = rightMana;
            this.autoMode = autoMode;
            this.maxCharges = maxCharges;
        }
    }

    public static class LeveledItemPreset extends ItemPreset {
        public List<ItemLevel> levels = new ArrayList<>();
        public LeveledItemPreset() {}
        public LeveledItemPreset(String name, String itemId, int customModelData, int leftCD, int rightCD, int leftMana, int rightMana, int autoMode, int maxCharges) {
            super(name, itemId, customModelData, leftCD, rightCD, leftMana, rightMana, autoMode, maxCharges);
        }
    }

    @Override public boolean isHudSlotEnabled(int slot) { return slot >= 0 && slot < 9 && hudSlotsEnabled[slot]; }
    @Override public int getHudSlotX(int slot) { return slot >= 0 && slot < 9 ? hudSlotsX[slot] : 0; }
    @Override public int getHudSlotY(int slot) { return slot >= 0 && slot < 9 ? hudSlotsY[slot] : 0; }
    @Override public float getHudSlotScale(int slot) { return slot >= 0 && slot < 9 ? hudSlotsScale[slot] : 1.0f; }
    @Override public boolean isOffhandEnabled() { return offhandSlotEnabled; }
    @Override public int getOffhandX() { return offhandX; }
    @Override public int getOffhandY() { return offhandY; }
    @Override public float getOffhandScale() { return offhandScale; }
    @Override public boolean showMana() { return showMana; }
    @Override public boolean showDamage() { return showDamage; }
    @Override public boolean showFps() { return showFps; }
    @Override public boolean showCps() { return showCps; }
    @Override public int getManaX() { return manaX; }
    @Override public int getManaY() { return manaY; }
    @Override public float getManaScale() { return manaScale; }
    @Override public int getDamageX() { return damageX; }
    @Override public int getDamageY() { return damageY; }
    @Override public float getDamageScale() { return damageScale; }
    @Override public int getFpsX() { return fpsX; }
    @Override public int getFpsY() { return fpsY; }
    @Override public float getFpsScale() { return fpsScale; }
    @Override public int getCpsX() { return cpsX; }
    @Override public int getCpsY() { return cpsY; }
    @Override public float getCpsScale() { return cpsScale; }
    @Override public boolean noRenderOnInactive() { return noRenderOnInactive; }
    @Override public boolean fpsLimitOnInactive() { return fpsLimitOnInactive; }
    @Override public boolean fpsLimitImmediateOnInactive() { return fpsLimitImmediateOnInactive; }
    @Override public int fpsLimitMinutes() { return fpsLimitMinutes; }
    @Override public int fpsLimitSeconds() { return fpsLimitSeconds; }
    @Override public int fpsLimitValue() { return fpsLimitValue; }
    @Override public boolean hideBlockBreakingParticles() { return hideBlockBreakingParticles; }
    @Override public boolean hideFireOverlay() { return hideFireOverlay; }
    @Override public int fireOverlayHeight() { return fireOverlayHeight; }
    @Override public boolean muteOnInactive() { return muteOnInactive; }
    @Override public boolean autoBossDetection() { return autoBossDetection; }
    @Override public String selectedBossNotification() { return selectedBossNotification; }
}