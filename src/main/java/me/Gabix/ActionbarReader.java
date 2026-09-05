package me.Gabix;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionbarReader {
    private static String lastRawActionbarText = "";
    private static String lastCleanActionbarText = "";
    private static long lastActionbarUpdateTime = 0;
    private static boolean continuousTitle = true;
    public static int currentDamage = 0;
    public static boolean isDamageReady = false;
    private static long lastDamageUpdateTime = 0;
    private static boolean wasDamageReady = false;
    private static final Pattern MANA_PATTERN = Pattern.compile("§b(\\d+)§8/§b\\d+");
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("Sebzés:\\s*.*?§f(\\d+)");
    private static Field cachedBossBarsField = null;

    public static void init() {}

    public static void handleTick(MinecraftClient client) {
        if (client.player == null) return;

        if (System.currentTimeMillis() - lastActionbarUpdateTime > 3000) {
            lastRawActionbarText = "";
            lastCleanActionbarText = "";
        }

        try {
            if (client.inGameHud != null) {
                BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
                if (cachedBossBarsField == null) {
                    for (Field field : BossBarHud.class.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            cachedBossBarsField = field;
                            break;
                        }
                    }
                }

                if (cachedBossBarsField != null) {
                    Map<?, ?> bossBars = (Map<?, ?>) cachedBossBarsField.get(bossBarHud);
                    if (bossBars != null && !bossBars.isEmpty()) {
                        for (Object value : bossBars.values()) {
                            if (value instanceof BossBar bossBar) {
                                String cleanBossName = bossBar.getName().getString().toLowerCase();
                                StorageConfig config = ActionbarStorageClient.getConfig();
                                if (config != null && config.autoBossDetection) {
                                    String targetBoss = null;
                                    if (cleanBossName.contains("ender") && cleanBossName.contains("mágus")) targetBoss = "Endermágus";
                                    else if (cleanBossName.contains("trex") || cleanBossName.contains("t-rex") || cleanBossName.contains("㕕ꈂ㕖ꈂ㕗ꈂ㕘ꈂ㕙ꈂ㕚ꈂ㕛ꈂ㕜")) targetBoss = "Trex";
                                    else if (cleanBossName.contains("warden")) targetBoss = "Warden";
                                    else if (cleanBossName.contains("radio") && (cleanBossName.contains("pók") || cleanBossName.contains("pok"))) targetBoss = "RadioPók";
                                    else if (((cleanBossName.contains("jég") || cleanBossName.contains("jeg")) && (cleanBossName.contains("sárkány") || cleanBossName.contains("sarkany")))
                                            || cleanBossName.contains("ice dragon")
                                            || cleanBossName.contains("icedragon")
                                            || cleanBossName.contains("㔎ꈂ㔏ꈂ㔐ꈂ㔑ꈂ㔒ꈂ㔓ꈂ㔔ꈂ㔖膛ꈋꈃꈇ㪀ꈂ㪁ꈂ㪂ꈂ㪃ꈂ㪄ꈂ㪅ꈂ㪆")) {
                                        targetBoss = "JégSárkány";
                                    }
                                    else if (cleanBossName.contains("kraken")) targetBoss = "Kraken";

                                    if (targetBoss != null && !config.selectedBossNotification.equals(targetBoss)) {
                                        config.selectedBossNotification = targetBoss;
                                        currentDamage = 0;
                                        wasDamageReady = false;
                                        client.inGameHud.setTitleTicks(5, 20, 5);
                                        client.inGameHud.setTitle(Text.literal("§d§lMÓD VÁLTVA"));
                                        client.inGameHud.setSubtitle(Text.literal("§eProfil automatikusan átállítva: §b" + targetBoss));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (currentDamage > 0 && System.currentTimeMillis() - lastDamageUpdateTime > 30000) {
            currentDamage = 0;
            if (ActionbarStorageClient.getInstance() != null && ActionbarStorageClient.getInstance().getManaManager() != null) {
                ActionbarStorageClient.getInstance().getManaManager().setCurrentDamage(0);
            }
        }

        int targetDamage = 5000;
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config != null) {
            targetDamage = config.getTargetBossDamage();
        }

        boolean isNowReady = (currentDamage >= targetDamage && currentDamage > 0);
        isDamageReady = isNowReady;

        if (isNowReady && !wasDamageReady) {
            if (client.inGameHud != null) {
                if (continuousTitle) {
                    client.inGameHud.setTitleTicks(5, 72000, 5);
                } else {
                    client.inGameHud.setTitleTicks(5, 20, 5);
                }
                client.inGameHud.setTitle(Text.literal("§a§l★ KÉSZ ★"));
            }
        } else if (!isNowReady && wasDamageReady) {
            if (client.inGameHud != null) {
                client.inGameHud.setTitleTicks(0, 0, 0);
                client.inGameHud.setTitle(Text.empty());
            }
        }
        wasDamageReady = isNowReady;

        while (ActionbarStorageClient.toggleContinuousKeyBinding.wasPressed()) {
            continuousTitle = !continuousTitle;
            if (isNowReady) {
                if (client.inGameHud != null) {
                    if (continuousTitle) {
                        client.inGameHud.setTitleTicks(0, 72000, 5);
                    } else {
                        client.inGameHud.setTitleTicks(0, 20, 5);
                    }
                    client.inGameHud.setTitle(Text.literal("§a§l★ KÉSZ ★"));
                }
            } else {
                wasDamageReady = false;
                if (client.inGameHud != null) {
                    client.inGameHud.setTitleTicks(0, 0, 0);
                    client.inGameHud.setTitle(Text.empty());
                    client.inGameHud.setSubtitle(Text.empty());
                }
            }
            if (continuousTitle) {
                client.player.sendMessage(Text.literal("§a[Mod] Kész felirat: §eFolyamatosan a képernyőn marad"), false);
            } else {
                client.player.sendMessage(Text.literal("§a[Mod] Kész felirat: §eCsak egyszer villan fel"), false);
            }
        }
    }

    public static void handleReadActionbar(MinecraftClient client) {
        if (lastRawActionbarText == null || lastRawActionbarText.isEmpty()) {
            client.player.sendMessage(Text.literal("§c[Actionbar] Jelenleg nincs aktív Actionbar a képernyőn!"), false);
        } else {
            String formattedWithAmpersand = lastRawActionbarText.replace("§", "&");
            client.player.sendMessage(Text.literal("§a[Actionbar] Aktív Actionbar adatok:"), false);
            client.player.sendMessage(Text.literal("§e - Tiszta név: §f" + lastCleanActionbarText), false);
            client.player.sendMessage(Text.literal("§e   Színezett/Kódolt (&): §f" + formattedWithAmpersand), false);
        }
    }

    public static void handleReadBossBars(MinecraftClient client) {
        readBossBars(client);
    }

    public static void handleItemInfo(MinecraftClient client) {
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) {
            client.player.sendMessage(Text.literal("§c[Item Info] Nincs tárgy a kezedben!"), false);
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("§a[Item Info]\n");
        result.append("§fNév: §e").append(held.getName().getString()).append("\n");

        String itemId = Registries.ITEM.getId(held.getItem()).toString();
        result.append("§fID: §e").append(itemId).append("\n");

        int cmd = 0;
        boolean hasCmd = false;
        if (held.hasNbt() && held.getNbt().contains("CustomModelData")) {
            cmd = held.getNbt().getInt("CustomModelData");
            hasCmd = true;
        }
        result.append("§fCMD: ").append(hasCmd ? "§b" + cmd : "§7nincs").append("\n");

        result.append("§fLore:\n");
        if (held.hasNbt() && held.getNbt().contains("display")) {
            NbtCompound display = held.getNbt().getCompound("display");
            if (display.contains("Lore", 9)) {
                NbtList loreList = display.getList("Lore", 8);
                boolean hasLore = false;
                for (int i = 0; i < loreList.size(); i++) {
                    String jsonText = loreList.getString(i);
                    try {
                        Text line = Text.Serializer.fromJson(jsonText);
                        if (line != null) {
                            result.append("§7- §f").append(line.getString()).append("\n");
                            hasLore = true;
                        } else {
                            result.append("§7- §c[hibás JSON]\n");
                            hasLore = true;
                        }
                    } catch (Exception e) {
                        result.append("§7- §c[hibás formátum]\n");
                        hasLore = true;
                    }
                }
                if (!hasLore) {
                    result.append("§7- (üres)\n");
                }
            } else {
                result.append("§7nincs\n");
            }
        } else {
            result.append("§7nincs\n");
        }

        client.player.sendMessage(Text.literal(result.toString()), false);
    }

    public static void readBossBars(MinecraftClient client) {
        if (client.player == null || client.inGameHud == null) return;
        try {
            BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
            if (cachedBossBarsField == null) {
                for (Field field : BossBarHud.class.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        cachedBossBarsField = field;
                        break;
                    }
                }
            }
            if (cachedBossBarsField != null) {
                Map<?, ?> bossBars = (Map<?, ?>) cachedBossBarsField.get(bossBarHud);
                if (bossBars == null || bossBars.isEmpty()) {
                    client.player.sendMessage(Text.literal("§c[BossBar] Jelenleg nincs aktív BossBar a képernyőn!"), false);
                } else {
                    client.player.sendMessage(Text.literal("§a[BossBar] Képernyőn lévő BossBar-ok:"), false);
                    for (Object value : bossBars.values()) {
                        if (value instanceof BossBar bossBar) {
                            String rawName = bossBar.getName().getString();
                            String formattedTextWithAmpersand = toRawLegacyText(bossBar.getName());
                            client.player.sendMessage(Text.literal("§e - Tiszta név: §f" + rawName), false);
                            client.player.sendMessage(Text.literal("§e   Színezett/Kódolt (&): §f" + formattedTextWithAmpersand), false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("§c[BossBar] Hiba történt a BossBar-ok kiolvasása közben!"), false);
        }
    }

    public static void updateText(Text text) {
        if (text == null) return;
        lastActionbarUpdateTime = System.currentTimeMillis();
        lastCleanActionbarText = text.getString();
        String rawText = toRawLegacyText(text).replace("&", "§");
        lastRawActionbarText = rawText;

        Matcher manaMatcher = MANA_PATTERN.matcher(rawText);
        if (manaMatcher.find()) {
            try {
                int foundMana = Integer.parseInt(manaMatcher.group(1));
                if (ActionbarStorageClient.getInstance() != null && ActionbarStorageClient.getInstance().getManaManager() != null) {
                    ActionbarStorageClient.getInstance().getManaManager().setCurrentMana(foundMana);
                }
            } catch (NumberFormatException ignored) {}
        }

        Matcher dmgMatcher = DAMAGE_PATTERN.matcher(rawText);
        if (dmgMatcher.find()) {
            try {
                int foundDamage = Integer.parseInt(dmgMatcher.group(1));
                currentDamage = foundDamage;
                lastDamageUpdateTime = System.currentTimeMillis();
                if (ActionbarStorageClient.getInstance() != null && ActionbarStorageClient.getInstance().getManaManager() != null) {
                    ActionbarStorageClient.getInstance().getManaManager().setCurrentDamage(foundDamage);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private static String toRawLegacyText(Text text) {
        if (text == null) return "";
        StringBuilder raw = new StringBuilder();
        text.visit((style, string) -> {
            if (style != null) {
                if (style.getColor() != null) {
                    String colorName = style.getColor().getName();
                    switch (colorName) {
                        case "black": raw.append("&0"); break;
                        case "dark_blue": raw.append("&1"); break;
                        case "dark_green": raw.append("&2"); break;
                        case "dark_aqua": raw.append("&3"); break;
                        case "dark_red": raw.append("&4"); break;
                        case "dark_purple": raw.append("&5"); break;
                        case "gold": raw.append("&6"); break;
                        case "gray": raw.append("&7"); break;
                        case "dark_gray": raw.append("&8"); break;
                        case "blue": raw.append("&9"); break;
                        case "green": raw.append("&a"); break;
                        case "aqua": raw.append("&b"); break;
                        case "red": raw.append("&c"); break;
                        case "light_purple": raw.append("&d"); break;
                        case "yellow": raw.append("&e"); break;
                        case "white": raw.append("&f"); break;
                        default: break;
                    }
                }
                if (style.isObfuscated()) raw.append("&k");
                if (style.isBold()) raw.append("&l");
                if (style.isStrikethrough()) raw.append("&m");
                if (style.isUnderlined()) raw.append("&n");
                if (style.isItalic()) raw.append("&o");
            }
            raw.append(string);
            return Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return raw.toString();
    }

    public static String getLastRawActionbarText() { return lastRawActionbarText; }
    public static String getLastCleanActionbarText() { return lastCleanActionbarText; }
}