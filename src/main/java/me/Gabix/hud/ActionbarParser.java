package me.Gabix.hud;

import me.Gabix.boss.BossMonitor;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionbarParser {
    private static String lastRawActionbarText = "";
    private static String lastCleanActionbarText = "";
    private static Text lastParsedText;
    private static final Pattern MANA_PATTERN = Pattern.compile("§b(\\d+)§8/§b(\\d+)");
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("Sebzés:\\s*.*?§f(\\d+)");
    private static Field cachedBossBarsField = null;
    private static Field cachedHeaderField = null;
    private static Field cachedFooterField = null;

    private static Map<Integer, String> buildCharLabelMap() {
        Map<Integer, String> map = new HashMap<>();
        StorageConfig cfg = ActionbarStorageClient.getConfig();
        if (cfg != null && cfg.customCharLabels != null) {
            for (Map.Entry<String, String> e : cfg.customCharLabels.entrySet()) {
                String key = e.getKey();
                String val = e.getValue();
                if (key == null || key.isEmpty() || val == null) continue;
                int cp = key.codePointAt(0);
                map.put(cp, val);
            }
        }
        if (!map.containsKey(0xE111)) map.put(0xE111, "[MVP]");
        if (!map.containsKey(0x662F)) map.put(0x662F, "[DEV]");
        if (!map.containsKey(0x910D)) map.put(0x910D, "[OWNER]");
        return map;
    }

    public static void updateText(Text text, ManaManager manaManager) {
        if (text == null) return;

        if (text == lastParsedText) return;

        String cleanText = text.getString();
        if (cleanText.equals(lastCleanActionbarText)) {
            lastParsedText = text;
            return;
        }

        boolean maybeMana = cleanText.indexOf('/') >= 0;
        boolean maybeDmg = cleanText.contains("Sebz");

        if (!maybeMana && !maybeDmg) {
            lastParsedText = text;
            lastCleanActionbarText = cleanText;
            return;
        }

        lastParsedText = text;
        lastCleanActionbarText = cleanText;
        String rawText = toRawLegacyText(text).replace("&", "§");
        lastRawActionbarText = rawText;

        if (maybeMana) {
            Matcher manaMatcher = MANA_PATTERN.matcher(rawText);
            if (manaMatcher.find()) {
                try {
                    manaManager.setCurrentMana(Integer.parseInt(manaMatcher.group(1)));
                    manaManager.setMaxMana(Integer.parseInt(manaMatcher.group(2)));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (maybeDmg) {
            Matcher dmgMatcher = DAMAGE_PATTERN.matcher(rawText);
            if (dmgMatcher.find()) {
                try {
                    int foundDamage = Integer.parseInt(dmgMatcher.group(1));
                    manaManager.setCurrentDamage(foundDamage);
                    BossMonitor.setCurrentDamage(foundDamage);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private static Text parseLegacyText(String input) {
        if (input == null) return Text.empty();
        MutableText result = Text.empty();
        Style currentStyle = Style.EMPTY;
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§' && i + 1 < input.length()) {
                if (segment.length() > 0) {
                    result.append(Text.literal(segment.toString()).setStyle(currentStyle));
                    segment.setLength(0);
                }
                char code = Character.toLowerCase(input.charAt(i + 1));
                i++;
                if (code == 'r') {
                    currentStyle = Style.EMPTY;
                } else {
                    Formatting fmt = switch (code) {
                        case '0' -> Formatting.BLACK;
                        case '1' -> Formatting.DARK_BLUE;
                        case '2' -> Formatting.DARK_GREEN;
                        case '3' -> Formatting.DARK_AQUA;
                        case '4' -> Formatting.DARK_RED;
                        case '5' -> Formatting.DARK_PURPLE;
                        case '6' -> Formatting.GOLD;
                        case '7' -> Formatting.GRAY;
                        case '8' -> Formatting.DARK_GRAY;
                        case '9' -> Formatting.BLUE;
                        case 'a' -> Formatting.GREEN;
                        case 'b' -> Formatting.AQUA;
                        case 'c' -> Formatting.RED;
                        case 'd' -> Formatting.LIGHT_PURPLE;
                        case 'e' -> Formatting.YELLOW;
                        case 'f' -> Formatting.WHITE;
                        case 'k' -> Formatting.OBFUSCATED;
                        case 'l' -> Formatting.BOLD;
                        case 'm' -> Formatting.STRIKETHROUGH;
                        case 'n' -> Formatting.UNDERLINE;
                        case 'o' -> Formatting.ITALIC;
                        default -> null;
                    };
                    if (fmt != null) {
                        currentStyle = currentStyle.withFormatting(fmt);
                    }
                }
            } else {
                segment.append(c);
            }
        }
        if (segment.length() > 0) {
            result.append(Text.literal(segment.toString()).setStyle(currentStyle));
        }
        return result;
    }

    private static boolean isCustomRange(int cp) {
        if (cp >= 0xE000 && cp <= 0xF8FF) return true;
        if (cp >= 0xF900 && cp <= 0xFAFF) return true;
        if (cp >= 0xF0000 && cp <= 0x10FFFD) return true;
        return false;
    }

    private static String expandCustomChars(String s) {
        if (s == null) return "";
        Map<Integer, String> map = buildCharLabelMap();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            int cc = Character.charCount(cp);

            String label = map.get(cp);
            if (label != null) {
                sb.append(label);
            } else if (isCustomRange(cp)) {
                if (cp <= 0xFFFF) {
                    sb.append(String.format("\\u%04X", cp));
                } else {
                    sb.append(String.format("\\u%05X", cp));
                }
            } else if (cp >= 0xFFF0 && cp <= 0xFFFF) {
                sb.append(String.format("\\u%04X", cp));
            } else {
                sb.appendCodePoint(cp);
            }
            i += cc;
        }
        return sb.toString();
    }

    private static String stripColorCodes(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toReadable(String raw) {
        return expandCustomChars(stripColorCodes(raw));
    }

    public static void handleReadActionbar(MinecraftClient client) {
        if (client.player == null) return;
        if (lastRawActionbarText == null || lastRawActionbarText.isEmpty()) {
            client.player.sendMessage(parseLegacyText("§c[Actionbar] Jelenleg nincs aktív Actionbar a képernyőn!"), false);
        } else {
            String formattedWithAmpersand = lastRawActionbarText.replace("§", "&");
            client.player.sendMessage(parseLegacyText("§a[Actionbar] Aktív Actionbar adatok:"), false);
            client.player.sendMessage(parseLegacyText("§e - Tiszta név: §f" + expandCustomChars(lastCleanActionbarText)), false);
            client.player.sendMessage(parseLegacyText("§e   Színezett/Kódolt (&): §f" + expandCustomChars(formattedWithAmpersand)), false);
        }
    }

    public static void handleReadBossBars(MinecraftClient client) {
        readBossBars(client);
    }

    public static void handleItemInfo(MinecraftClient client) {
        if (client.player == null) return;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) {
            client.player.sendMessage(parseLegacyText("§c[Item Info] Nincs tárgy a kezedben!"), false);
            return;
        }

        StringBuilder out = new StringBuilder();

        String nameRaw = toRawLegacyText(held.getName()).replace("&", "§");
        out.append("Név: ").append(toReadable(nameRaw)).append("\n");

        String itemId = Registries.ITEM.getId(held.getItem()).toString();
        out.append("ID: ").append(itemId).append("\n");

        int cmd = 0;
        boolean hasCmd = false;
        if (held.hasNbt() && held.getNbt().contains("CustomModelData")) {
            cmd = held.getNbt().getInt("CustomModelData");
            hasCmd = true;
        }
        out.append("CMD: ").append(hasCmd ? String.valueOf(cmd) : "nincs").append("\n");

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
                    out.append("Szín (festés): ").append(hexColor)
                            .append(" (RGB: ").append(r).append(", ").append(g).append(", ").append(b).append(")")
                            .append(" [Int: ").append(display.getInt("color")).append("]\n");
                    hasColor = true;
                }
            }
            if (!hasColor) out.append("Szín (festés): alapértelmezett (nincs festve)\n");
        }

        out.append("Lore:\n");
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
                            String lineRaw = toRawLegacyText(line).replace("&", "§");
                            out.append("- ").append(toReadable(lineRaw)).append("\n");
                            hasLore = true;
                        } else {
                            out.append("- [hibás JSON]\n");
                            hasLore = true;
                        }
                    } catch (Exception e) {
                        out.append("- [hibás formátum]\n");
                        hasLore = true;
                    }
                }
                if (!hasLore) out.append("- (üres)\n");
            } else {
                out.append("nincs\n");
            }
        } else {
            out.append("nincs\n");
        }

        Clipboard clipboard = new Clipboard();
        clipboard.setClipboard(client.getWindow().getHandle(), out.toString());

        client.player.sendMessage(parseLegacyText("§a[Item Info] Az adatok a vágólapra másolva!"), false);
    }

    public static void handleReadLeaderboard(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        try {
            Scoreboard scoreboard = client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(Scoreboard.SIDEBAR_DISPLAY_SLOT_ID);

            if (objective == null) {
                client.player.sendMessage(parseLegacyText("§c[Leaderboard] Jelenleg nincs aktív leaderboard a képernyőn!"), false);
                return;
            }

            Collection<ScoreboardPlayerScore> entries = scoreboard.getAllPlayerScores(objective);
            if (entries == null || entries.isEmpty()) {
                client.player.sendMessage(parseLegacyText("§c[Leaderboard] A leaderboard üres!"), false);
                return;
            }

            List<ScoreboardPlayerScore> sorted = new ArrayList<>(entries);
            sorted.sort(Comparator.comparingInt(ScoreboardPlayerScore::getScore).reversed());

            StringBuilder out = new StringBuilder();
            String titleRaw = objective.getDisplayName().getString();
            String titleFormatted = toRawLegacyText(objective.getDisplayName()).replace("&", "§");

            out.append("===== LEADERBOARD =====\n");
            out.append("Cím (tiszta): ").append(expandCustomChars(titleRaw)).append("\n");
            out.append("Cím (színezett): ").append(toReadable(titleFormatted)).append("\n");
            out.append("========================\n\n");

            out.append("--- Tiszta lista ---\n");
            int rank = 1;
            for (ScoreboardPlayerScore entry : sorted) {
                Team team = scoreboard.getPlayerTeam(entry.getPlayerName());
                Text displayName = Team.decorateName(team, Text.literal(entry.getPlayerName()));
                out.append(rank).append(". ").append(expandCustomChars(displayName.getString()))
                        .append(" - ").append(entry.getScore()).append("\n");
                rank++;
            }

            out.append("\n--- Színezett lista ---\n");
            rank = 1;
            for (ScoreboardPlayerScore entry : sorted) {
                Team team = scoreboard.getPlayerTeam(entry.getPlayerName());
                Text displayName = Team.decorateName(team, Text.literal(entry.getPlayerName()));
                String colored = toRawLegacyText(displayName).replace("&", "§");
                out.append(rank).append(". ").append(toReadable(colored))
                        .append(" - ").append(entry.getScore()).append("\n");
                rank++;
            }

            out.append("\n--- Nyers (name;score) ---\n");
            for (ScoreboardPlayerScore entry : sorted) {
                out.append(entry.getPlayerName()).append(";").append(entry.getScore()).append("\n");
            }

            Clipboard clipboard = new Clipboard();
            clipboard.setClipboard(client.getWindow().getHandle(), out.toString());

            client.player.sendMessage(parseLegacyText("§a[Leaderboard] " + sorted.size() + " bejegyzés a vágólapra másolva!"), false);
        } catch (Exception e) {
            client.player.sendMessage(parseLegacyText("§c[Leaderboard] Hiba: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }

    public static void handleReadTablist(MinecraftClient client) {
        if (client.player == null || client.inGameHud == null) {
            if (client.player != null) {
                client.player.sendMessage(parseLegacyText("§c[Tablist] Nem vagy játékban!"), false);
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
                client.player.sendMessage(parseLegacyText("§c[Tablist] Nincs header/footer a tab listán!"), false);
                return;
            }

            StringBuilder out = new StringBuilder();
            out.append("===== TABLIST KÖRÍTÉS =====\n\n");

            out.append("--- HEADER ---\n");
            if (header != null && !header.getString().isEmpty()) {
                String headerRaw = toRawLegacyText(header);
                out.append("Tiszta (olvasható):\n").append(expandCustomChars(header.getString())).append("\n\n");
                out.append("Színezett (&):\n").append(toReadable(headerRaw)).append("\n\n");
                out.append("Nyers (& + escape-elt custom chars):\n").append(expandCustomChars(headerRaw)).append("\n\n");
            } else {
                out.append("(üres)\n\n");
            }

            out.append("--- FOOTER ---\n");
            if (footer != null && !footer.getString().isEmpty()) {
                String footerRaw = toRawLegacyText(footer);
                out.append("Tiszta (olvasható):\n").append(expandCustomChars(footer.getString())).append("\n\n");
                out.append("Színezett (&):\n").append(toReadable(footerRaw)).append("\n\n");
                out.append("Nyers (& + escape-elt custom chars):\n").append(expandCustomChars(footerRaw)).append("\n");
            } else {
                out.append("(üres)\n");
            }

            Clipboard clipboard = new Clipboard();
            clipboard.setClipboard(client.getWindow().getHandle(), out.toString());

            client.player.sendMessage(parseLegacyText("§a[Tablist] A körítés (header/footer) a vágólapra másolva!"), false);
        } catch (Exception e) {
            client.player.sendMessage(parseLegacyText("§c[Tablist] Hiba: " + e.getMessage()), false);
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
                    client.player.sendMessage(parseLegacyText("§c[BossBar] Jelenleg nincs aktív BossBar a képernyőn!"), false);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("BossBar adatok:\n");
                    for (Object value : bossBars.values()) {
                        if (value instanceof BossBar bossBar) {
                            String clean = bossBar.getName().getString();
                            String colored = toRawLegacyText(bossBar.getName());
                            sb.append("Tiszta név: ").append(expandCustomChars(clean)).append("\n");
                            sb.append("Színezett (&): ").append(expandCustomChars(colored)).append("\n\n");
                        }
                    }
                    Clipboard clipboard = new Clipboard();
                    clipboard.setClipboard(client.getWindow().getHandle(), sb.toString());
                    client.player.sendMessage(parseLegacyText("§a[BossBar] Az adatok a vágólapra másolva!"), false);
                }
            }
        } catch (Exception e) {
            client.player.sendMessage(parseLegacyText("§c[BossBar] Hiba történt a BossBar-ok kiolvasása közben!"), false);
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