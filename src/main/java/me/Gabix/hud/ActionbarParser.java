package me.Gabix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
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
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionbarParser {
    private static String lastRawActionbarText = "";
    private static String lastCleanActionbarText = "";
    
    // Pattern for extracting numbers from string values
    private static final java.util.regex.Pattern MANA_PATTERN = java.util.regex.Pattern.compile("§b(\\\\d+)§8/§b(\\\\d+)");
    private static final java.util.regex.Pattern DAMAGE_PATTERN = java.util.regex.Pattern.compile("Sebzés:\\s*.*?§f(\\\\d+)");
    
    // Cache for field access
    private static net.minecraft.client.gui.hud.BossBarHud.HudBars cachedBossBarsField = null;

    /** Custom char map */
    private static final HashMap<Integer, String> CUSTOM_CHARS = new HashMap<>();

    public ActionbarParser() {
        initCustomChars();
    }

    public void initCustomChars() {
        // Initialize common custom character mappings
        if (CUSTOM_CHARS.size() == 0) {
            // MVP-specific chars
            int mvpChar = 0xE111;
            CUSTOM_CHARS.put(mvpChar, "[MVP]");
            
            // Developer char
            int devChar = 0x662F;
            CUSTOM_CHARS.put(devChar, "[DEV]");
            
            // Owner char  
            int ownerChar = 0x910D;
            CUSTOM_CHARS.put(ownerChar, "[OWNER]");
        }
    }

    /** Get the current custom character map */
    public Map<Integer, String> getCustomChars() {
        if (CUSTOM_CHARS.size() == 0) {
            initCustomChars();
        }
        return CUSTOM_CHARS;
    }

    /** Process action bar text and extract data */
    public void processActionbarText(String rawInput) {
        if (rawInput.trim().isEmpty()) {
            lastRawActionbarText = "";
            lastCleanActionbarText = "";
            System.err.println("[DEBUG] No action bar detected");
            return;
        }

        // Extract and parse the input text
        int startIndex = 0;
        int lastIndex = rawInput.length();
        String cleanVersion = rawInput.replace("§", "&");
        
        lastRawActionbarText = "";
        lastCleanActionbarText = cleanVersion;

        Map<net.minecraft.text.Text, java.util.List<String>> resultData = new HashMap<>();

        for (int i = 0; i < lastIndex; ) {
            // Parse code point at current index
            int cp = rawInput.codePointAt(i);
            
            // Handle high surrogate (for UTF-16 > U+FFFF)
            char lowSurrogate = Character.highSurrogate((char)(cp & 0xFFFF));
            if (lowSurrogate >= 0xD800 && lowSurrogate <= 0xDBFF) {
                int nextCp = rawInput.codePointAt(i + Character.charCount(cp)); // Get next code point
                
                // If we found a valid surrogate pair, append the character using Unicode escape
                String result = String.format("\\u%04X\\u%04X", cp, (nextCp & 0xFFFF) | 0xDC00);
                lastRawActionbarText += result;
                
                // Handle any potential overflow
                if (cp >= 0x110000) {
                    result = String.format("\\u%04X\\u%04X", cp, (nextCp & 0xFFFF));
                    lastRawActionbarText += result;
                }
            } else {
                // Standard single code point handling with proper Unicode escape
                if (cp < 0x10000) {
                    lastRawActionbarText += String.format("\\u%04X", cp);
                } else {
                    // Supplementary plane character
                    int surrogateA = (cp - 0x10000) >> 10;
                    int surrogateB = (cp - 0x10000) & 0x3FF;
                    lastRawActionbarText += String.format("\\u%04X\\u%04X", 0xD800 + surrogateA, 0xDC00 + surrogateB);
                }
            }
            
            i += Character.charCount(cp);
        }

        System.err.println("[DEBUG] Actionbar processed: " + lastRawActionbarText.length() + " characters");
    }

    /** Get the full code point array for debugging */
    public char[] getCodePointArray(String input) {
        int length = input.length();
        char[] result = new char[length];
        
        // Process each code point with proper UTF-16 encoding
        for (int i = 0; i < length; ) {
            int cp = input.codePointAt(i);
            int cc = Character.charCount(cp);
            
            // Use Character.toChars() for proper surrogate handling
            char[] chars = Character.toChars(cp);
            
            System.err.println("[DEBUG] Code point: 0x" + Integer.toHexString(cp) + " Length: " + chars.length);
            
            // Append to result using proper encoding
            for (int j = 0; j < cc; j++) {
                result[i] = input.charAt(i + j);
                i += j + 1;
            }
        }
        
        return result;
    }

    /** Parse text with proper legacy formatting handling */
    public Text parseLegacyFormattedText(String text) {
        MutableText result = Text.empty();
        
        // Process with standard Minecraft formatting parsing
        StringBuilder rawBuilder = new StringBuilder(text);
        String[] parts = processRawText(rawBuilder.toString());
        
        for (String part : parts) {
            if (part.contains("$")) {
                continue; // Skip special characters
            }
            
            result.append(Text.literal(part));
        }
        
        return result;
    }

    private java.util.List<String> processRawText(String raw) {
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }

        int startIndex = 0;
        java.util.List<String> parts = new ArrayList<>();
        String currentSegment = "";

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            
            // Handle escape sequences and formatting codes
            if (c == '\n' || c == '§') {
                if (currentSegment.length() > 0) {
                    parts.add(currentSegment.trim());
                }
                currentSegment = "";
            } else {
                StringBuilder segment = new StringBuilder(" " + c);
                i++; // Skip the next character
            
                // Append to current segment
                currentSegment = currentSegment + " " + c;
            }
            
            if (currentSegment == null || !currentSegment.isEmpty()) {
                rawBuilder.append(currentSegment);
                parts.add(rawBuilder.toString());
            }
        }

        return parts;
    }

    /** Process text with proper Unicode escaping */
    public java.util.List<String> processTextWithUnicode(String raw) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < raw.length(); ) {
            int cp = Character.codePointAt(raw, i);
            int cc = Character.charCount(cp);

            // Debug each code point with character count info
            System.err.println("[DEBUG] Processing: U+" + Integer.toHexString(cp) + " (" + cc + " chars)");

            String processed = Character.toString(cp);

            sb.append(processed);
            i += cc;
        }

        return java.util.Arrays.asList(sb.toString(), sb.toString());
    }

    /** Convert code point to UTF-8 string representation */
    private String utf8Encode(int cp) {
        if (cp < 0x10000) {
            return String.format("\\u%04X", cp);
        }
        
        // Handle supplementary plane characters with proper surrogate logic
        int high = (int)((cp - 0x10000L) >> 18);
        int low = ((cp - 0x10000L) & 0xFFFFL) | 0x0E000;

        return String.format("\\u%04X\\u%04X", high, low);
    }

    /** Get the code point as UTF-8 surrogate pair */
    public String getCodePointAsUtf8(int cp) {
        if (cp < 0x10000) {
            return String.format("\\u%04X", cp);
        }

        // Handle supplementary characters with proper surrogate encoding logic
        int high = (int)((cp - 0x10000L) >> 18);
        int low = ((cp - 0x10000L) & 0xFFFFL) | 0x0E000;

        return String.format("\\u%04X\\u%04X", high, low);
    }

    /** Convert raw text into usable string format */
    public String convertRawToUsable(String rawStr) {
        StringBuilder sb = new StringBuilder();
        System.err.println("[DEBUG] Converting raw length: " + rawStr.length() + " -> usable");

        for (int i = 0; i < rawStr.length(); ) {
            int cp = Character.codePointAt(rawStr, i);
            int cc = Character.charCount(cp);

            sb.append(Character.toChars(cp));

            // Debug output for proper Unicode handling
            if (Character.isHighSurrogate(cp)) {
                System.err.println("[DEBUG] High surrogate: 0x" + Integer.toHexString(cp));
            } else if (cc > 1) {
                System.err.println("[DEBUG] Low surrogate: 0x" + Integer.toChars(cp));
            }

            i += cc;
        }

        return sb.toString();
    }

    /** Process boss bar information with proper encoding */
    public void processBossBars(String[] values) {
        StringBuilder output = new StringBuilder();

        for (String value : values) {
            System.err.println("[DEBUG] Processing boss bar: '" + value + "'");
            
            // Convert each character with proper Unicode handling
            String cleaned = convertRawToUsable(value);
            output.append(cleaned);
        }

        System.err.println("[DEBUG] Boss bars processed into: " + output.length() + " characters");
    }

    /** Process tablist data with proper formatting */
    public void processTablist(String playerName, int score) {
        String displayName = "";
        
        if (playerName != null && !playerName.isEmpty()) {
            // Handle potential Unicode characters in team/decoration names if needed
            String cleanName = playerName;
            
            for (int i = 0; i < cleanName.length(); ) {
                int cp = Character.codePointAt(cleanName, i);
                
                System.err.println("[DEBUG] Tablist char: U+" + Integer.toHexString(cp));
                
                if (i > 0 && Character.isLowSurrogate(cleanName.charAt(i))) {
                    continue; // Skip surrogate chars for debugging purposes
                }

                displayName += cleanName.charAt(i);
                i += Character.charCount(cp);
            }
        }

        String output = displayName + " - Score: " + score;
        System.err.println("[DEBUG] Tablist output: " + output);
    }

    /** Debug method for proper character processing */
    public static void debugStringProcessing(String inputStr) {
        if (inputStr != null && inputStr.length() > 0) {
            StringBuilder chars = new StringBuilder();
            
            // Process character by character with UTF-8 logic
            for (int i = 0; i < inputStr.length(); ) {
                int cp = Character.codePointAt(inputStr, i);

                System.err.println("[DEBUG] Raw char: '" + inputStr.charAt(i) + "' -> U" + Integer.toHexString(cp));

                if (!inputStr.isEmpty() && Character.isHighSurrogate(cp)) {
                    continue;
                }

                String escaped = Character.toString(cp);
                
                // Append with proper encoding
                chars.append(escaped);
            }
            
            System.err.println("[DEBUG] Processed: " + chars.toString().length());
        }
    }

}
