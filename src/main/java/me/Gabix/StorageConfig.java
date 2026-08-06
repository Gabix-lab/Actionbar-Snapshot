package me.Gabix;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("ab-config.json").toFile();

    public boolean[] hudSlotsEnabled = {true, true, true, true, true, true, true, true, true};
    public transient List<ItemPreset> normalItems;
    public transient List<LeveledItemPreset> leveledItems;

    public transient String selectedBossNotification = "Ki";
    public boolean autoBossDetection = true;

    public boolean showMana = true;
    public boolean showDamage = true;

    public boolean noRenderTimerEnabled = true;
    public int noRenderMinutes = 5;
    public int noRenderSeconds = 0;
    public boolean noRenderOnInactive = false;
    public boolean fpsLimitImmediateOnInactive = false;
    public boolean fpsLimitOnInactive = false;
    public int fpsLimitMinutes = 0;
    public int fpsLimitSeconds = 0;
    public int fpsLimitValue = 30;

    public int manaX = 1;
    public int manaY = 202;
    public float manaScale = 1.0000001f;

    public int damageX = 1;
    public int damageY = 218;
    public float damageScale = 1.0f;

    public int[] hudSlotsX = {1, 1, 1, 1, 1, 1, 1, 1, 1};
    public int[] hudSlotsY = {13, 34, 55, 76, 97, 118, 139, 160, 181};
    public float[] hudSlotsScale = {1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f};

    public boolean hideBlockBreakingParticles = false;

    public boolean hideFireOverlay = false;
    public int fireOverlayHeight = 100;

    public StorageConfig() {
        this.selectedBossNotification = "Ki";
        this.normalItems = createDefaultNormalItems();
        this.leveledItems = createDefaultLeveledItems();
    }

    public int getTargetBossDamage() {
        if (selectedBossNotification == null) return Integer.MAX_VALUE;
        return switch (selectedBossNotification) {
            case "JégSárkány" -> 6000;
            case "Trex" -> 20000;
            case "Endermágus" -> 3000;
            case "Warden" -> 10000;
            case "RadioPók" -> 15000;
            case "Kraken" -> 8000;
            default -> Integer.MAX_VALUE;
        };
    }

    private List<ItemPreset> createDefaultNormalItems() {
        List<ItemPreset> list = new ArrayList<>();
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
        List<LeveledItemPreset> list = new ArrayList<>();
        LeveledItemPreset amethystSword = new LeveledItemPreset("Amethyst sword", "minecraft:paper", 562, 0, 800, 0, 15, 1, 0);
        amethystSword.levels.add(new ItemLevel("2. szint", 0, 700, 0, 14));
        amethystSword.levels.add(new ItemLevel("3. szint", 0, 700, 0, 14));
        amethystSword.levels.add(new ItemLevel("4. szint", 0, 600, 0, 12));
        amethystSword.levels.add(new ItemLevel("MAXIMUM SZINT", 0, 600, 0, 12));
        list.add(amethystSword);

        LeveledItemPreset amethystStaff = new LeveledItemPreset("Amethyst staff", "minecraft:paper", 569, 20, 1200, 2, 18, 1, 0);
        amethystStaff.levels.add(new ItemLevel("2. szint", 20, 1000, 2, 18));
        amethystStaff.levels.add(new ItemLevel("3. szint", 17, 1000, 1, 18));
        amethystStaff.levels.add(new ItemLevel("4. szint", 17, 1000, 1, 15));
        amethystStaff.levels.add(new ItemLevel("MAXIMUM SZINT", 14, 900, 1, 15));
        list.add(amethystStaff);

        LeveledItemPreset healStaff = new LeveledItemPreset("Heal staff", "minecraft:paper", 566, 10, 160, 2, 10, 2, 0);
        healStaff.levels.add(new ItemLevel("2. szint", 10, 160, 2, 9));
        healStaff.levels.add(new ItemLevel("3. szint", 10, 160, 2, 9));
        healStaff.levels.add(new ItemLevel("4. szint", 10, 140, 2, 9));
        healStaff.levels.add(new ItemLevel("MAXIMUM SZINT", 10, 140, 2, 8));
        list.add(healStaff);

        LeveledItemPreset honeySword = new LeveledItemPreset("Honey Sword", "minecraft:diamond_sword", 567, 0, 40, 0, 5, 1, 0);
        honeySword.levels.add(new ItemLevel("2. szint", 0, 40, 0, 5));
        honeySword.levels.add(new ItemLevel("3. szint", 0, 40, 0, 5));
        honeySword.levels.add(new ItemLevel("4. szint", 0, 40, 0, 4));
        honeySword.levels.add(new ItemLevel("MAXIMUM SZINT", 0, 40, 0, 4));
        list.add(honeySword);

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
                if (loaded.hudSlotsX == null || loaded.hudSlotsX.length < 9)
                    loaded.hudSlotsX = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1};
                if (loaded.hudSlotsY == null || loaded.hudSlotsY.length < 9)
                    loaded.hudSlotsY = new int[]{13, 34, 55, 76, 97, 118, 139, 160, 181};
                if (loaded.hudSlotsScale == null || loaded.hudSlotsScale.length < 9)
                    loaded.hudSlotsScale = new float[]{1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f};
                loaded.normalItems = new StorageConfig().normalItems;
                loaded.leveledItems = new StorageConfig().leveledItems;
                loaded.selectedBossNotification = "Ki";
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
}