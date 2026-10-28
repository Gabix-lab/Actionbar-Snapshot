package me.Gabix.hud;

import me.Gabix.boss.BossMonitor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.util.Clipboard;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionbarParser {
    private static String lastRawActionbarText = "";
    private static String lastCleanActionbarText = "";
    private static final Pattern MANA_PATTERN = Pattern.compile("§b(\\d+)§8/§b(\\d+)");
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("Sebzés:\\s*.*?§f(\\d+)");
    private static Field cachedBossBarsField = null;
    private static Field cachedHeaderField = null;
    private static Field cachedFooterField = null;

    public static void updateText(Text text, ManaManager manaManager) {
        if (text == null) return;
        lastCleanActionbarText = text.getString();
        String rawText = toRawLegacyText(text).replace("&", "§");
        lastRawActionbarText = rawText;

        Matcher manaMatcher = MANA_PATTERN.matcher(rawText);
        if (manaMatcher.find()) {
            try {
                manaManager.setCurrentMana(Integer.parseInt(manaMatcher.group(1)));
                manaManager.setMaxMana(Integer.parseInt(manaMatcher.group(2)));
            } catch (NumberFormatException ignored) {}
        }

        Matcher dmgMatcher = DAMAGE_PATTERN.matcher(rawText);
        if (dmgMatcher.find()) {
            try {
                int foundDamage = Integer.parseInt(dmgMatcher.group(1));
                manaManager.setCurrentDamage(foundDamage);
                BossMonitor.setCurrentDamage(foundDamage);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void handleReadActionbar(MinecraftClient client) {
        if (client.player == null) return;
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
        if (client.player == null) return;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) {
            client.player.sendMessage(Text.literal("§c[Item Info] Nincs tárgy a kezedben!"), false);
            return;
        }

        StringBuilder result = new StringBuilder();

        result.append("Név: ")
                .append(toRawLegacyText(held.getName()).replace("&", "§"))
                .append("\n");

        String itemId = Registries.ITEM.getId(held.getItem()).toString();
        result.append("ID: ").append(itemId).append("\n");

        int cmd = 0;
        boolean hasCmd = false;
        if (held.hasNbt() && held.getNbt().contains("CustomModelData")) {
            cmd = held.getNbt().getInt("CustomModelData");
            hasCmd = true;
        }
        result.append("CMD: ").append(hasCmd ? String.valueOf(cmd) : "nincs").append("\n");

        boolean isDyeableLeather = itemId.equals("minecraft:leather_helmet")
                || itemId.equals("minecraft:leather_chestplate")
                || itemId.equals("minecraft:leather_leggings")
                || itemId.equals("minecraft:leather_boots")
                || itemId.equals("minecraft:leather_horse_armor")
                || itemId.equals("minecraft:wolf_armor");

        if (isDyeableLeather) {
            boolean hasColor = false;
            if (held.hasNbt() && held.getNbt().contains("display", 10)) {
                NbtCompound display = held.getNbt().getCompound("display");
                if (display.contains("color", 99)) {
                    int colorInt = display.getInt("color") & 0xFFFFFF;
                    String hexColor = String.format("#%06X", colorInt);
                    int r = (colorInt >> 16) & 0xFF;
                    int g = (colorInt >> 8) & 0xFF;
                    int b = colorInt & 0xFF;

                    result.append("Szín (festés): ")
                            .append(hexColor)
                            .append(" (RGB: ").append(r).append(", ").append(g).append(", ").append(b).append(")")
                            .append(" [Int: ").append(display.getInt("color"))
                            .append("]\n");
                    hasColor = true;
                }
            }
            if (!hasColor) {
                result.append("Szín (festés): §7alapértelmezett (nincs festve)\n");
            }
        }

        result.append("Lore:\n");
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
                            result.append("- ")
                                    .append(toRawLegacyText(line).replace("&", "§"))
                                    .append("\n");
                            hasLore = true;
                        } else {
                            result.append("- [hibás JSON]\n");
                            hasLore = true;
                        }
                    } catch (Exception e) {
                        result.append("- [hibás formátum]\n");
                        hasLore = true;
                    }
                }
                if (!hasLore) {
                    result.append("- (üres)\n");
                }
            } else {
                result.append("nincs\n");
            }
        } else {
            result.append("nincs\n");
        }

        Clipboard clipboard = new Clipboard();
        clipboard.setClipboard(client.getWindow().getHandle(), result.toString());

        client.player.sendMessage(Text.literal("§a[Item Info] Az adatok a vágólapra másolva!"), false);
    }

    public static void handleReadLeaderboard(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        try {
            Scoreboard scoreboard = client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(Scoreboard.SIDEBAR_DISPLAY_SLOT_ID);

            if (objective == null) {
                client.player.sendMessage(Text.literal("§c[Leaderboard] Jelenleg nincs aktív leaderboard a képernyőn!"), false);
                return;
            }

            Collection<ScoreboardPlayerScore> entries = scoreboard.getAllPlayerScores(objective);

            if (entries == null || entries.isEmpty()) {
                client.player.sendMessage(Text.literal("§c[Leaderboard] A leaderboard üres!"), false);
                return;
            }

            List<ScoreboardPlayerScore> sorted = new ArrayList<>(entries);
            sorted.sort(Comparator.comparingInt(ScoreboardPlayerScore::getScore).reversed());

            StringBuilder sb = new StringBuilder();

            String titleRaw = objective.getDisplayName().getString();
            String titleFormatted = toRawLegacyText(objective.getDisplayName()).replace("&", "§");

            sb.append("===== LEADERBOARD =====\n");
            sb.append("Cím (tiszta): ").append(titleRaw).append("\n");
            sb.append("Cím (színezett): ").append(titleFormatted).append("\n");
            sb.append("========================\n\n");

            sb.append("--- Tiszta lista ---\n");
            int rank = 1;
            for (ScoreboardPlayerScore entry : sorted) {
                Team team = scoreboard.getPlayerTeam(entry.getPlayerName());
                Text displayName = Team.decorateName(team, Text.literal(entry.getPlayerName()));
                sb.append(rank).append(". ").append(displayName.getString())
                        .append(" - ").append(entry.getScore()).append("\n");
                rank++;
            }

            sb.append("\n--- Színezett lista (& kódokkal) ---\n");
            rank = 1;
            for (ScoreboardPlayerScore entry : sorted) {
                Team team = scoreboard.getPlayerTeam(entry.getPlayerName());
                Text displayName = Team.decorateName(team, Text.literal(entry.getPlayerName()));
                sb.append(rank).append(". ").append(toRawLegacyText(displayName).replace("&", "§"))
                        .append(" - ").append(entry.getScore()).append("\n");
                rank++;
            }

            sb.append("\n--- Nyers (name;score) ---\n");
            for (ScoreboardPlayerScore entry : sorted) {
                sb.append(entry.getPlayerName()).append(";").append(entry.getScore()).append("\n");
            }

            Clipboard clipboard = new Clipboard();
            clipboard.setClipboard(client.getWindow().getHandle(), sb.toString());

            client.player.sendMessage(Text.literal("§a[Leaderboard] " + sorted.size() + " bejegyzés a vágólapra másolva!"), false);
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("§c[Leaderboard] Hiba: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }

    public static void handleReadTablist(MinecraftClient client) {
        if (client.player == null || client.inGameHud == null) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[Tablist] Nem vagy játékban!"), false);
            }
            return;
        }

        try {
            PlayerListHud playerListHud = client.inGameHud.getPlayerListHud();
            initTablistFields();

            Text header = null;
            Text footer = null;

            if (cachedHeaderField != null) header = (Text) cachedHeaderField.get(playerListHud);
            if (cachedFooterField != null) footer = (Text) cachedFooterField.get(playerListHud);

            if ((header == null || header.getString().isEmpty())
                    && (footer == null || footer.getString().isEmpty())) {
                client.player.sendMessage(Text.literal("§c[Tablist] Nincs header/footer a tab listán!"), false);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("===== TABLIST KÖRÍTÉS =====\n\n");

            sb.append("--- HEADER ---\n");
            if (header != null && !header.getString().isEmpty()) {
                sb.append("Tiszta:\n").append(header.getString()).append("\n\n");
                sb.append("Színezett (&):\n")
                        .append(toRawLegacyText(header).replace("&", "§"))
                        .append("\n\n");
                sb.append("Nyers (& kódokkal):\n")
                        .append(toRawLegacyText(header))
                        .append("\n\n");
            } else {
                sb.append("(üres)\n\n");
            }

            sb.append("--- FOOTER ---\n");
            if (footer != null && !footer.getString().isEmpty()) {
                sb.append("Tiszta:\n").append(footer.getString()).append("\n\n");
                sb.append("Színezett (&):\n")
                        .append(toRawLegacyText(footer).replace("&", "§"))
                        .append("\n\n");
                sb.append("Nyers (& kódokkal):\n")
                        .append(toRawLegacyText(footer))
                        .append("\n");
            } else {
                sb.append("(üres)\n");
            }

            Clipboard clipboard = new Clipboard();
            clipboard.setClipboard(client.getWindow().getHandle(), sb.toString());

            client.player.sendMessage(Text.literal("§a[Tablist] A körítés (header/footer) a vágólapra másolva!"), false);
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("§c[Tablist] Hiba: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }

    private static void initTablistFields() {
        if (cachedHeaderField != null && cachedFooterField != null) return;
        for (Field field : PlayerListHud.class.getDeclaredFields()) {
            if (field.getType() == Text.class) {
                field.setAccessible(true);
                if (cachedHeaderField == null) {
                    cachedHeaderField = field;
                } else if (cachedFooterField == null) {
                    cachedFooterField = field;
                    break;
                }
            }
        }
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
                    StringBuilder sb = new StringBuilder();
                    sb.append("BossBar adatok:\n");
                    for (Object value : bossBars.values()) {
                        if (value instanceof BossBar bossBar) {
                            sb.append("Tiszta név: ").append(bossBar.getName().getString()).append("\n");
                            sb.append("Színezett (&): ").append(toRawLegacyText(bossBar.getName())).append("\n");
                        }
                    }
                    Clipboard clipboard = new Clipboard();
                    clipboard.setClipboard(client.getWindow().getHandle(), sb.toString());
                    client.player.sendMessage(Text.literal("§a[BossBar] Az adatok a vágólapra másolva!"), false);
                }
            }
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("§c[BossBar] Hiba történt a BossBar-ok kiolvasása közben!"), false);
        }
    }

    private static String toRawLegacyText(Text text) {
        if (text == null) return "";
        StringBuilder raw = new StringBuilder();
        text.visit((style, string) -> {
            if (style != null) {
                if (style.getColor() != null) {
                    String colorName = style.getColor().getName();
                    if (colorName != null && colorName.startsWith("#") && colorName.length() == 7) {
                        raw.append("&x");
                        for (int i = 1; i < 7; i++) {
                            raw.append("&").append(Character.toLowerCase(colorName.charAt(i)));
                        }
                    } else {
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
                }
                if (style.isObfuscated()) raw.append("&k");
                if (style.isBold()) raw.append("&l");
                if (style.isStrikethrough()) raw.append("&m");
                if (style.isUnderlined()) raw.append("&n");
                if (style.isItalic()) raw.append("&o");
            }
            raw.append(string);
            return Optional.empty();
        }, Style.EMPTY);
        return raw.toString();
    }
}