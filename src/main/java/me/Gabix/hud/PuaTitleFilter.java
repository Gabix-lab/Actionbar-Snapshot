package me.Gabix.hud;

import me.Gabix.core.ActionbarStorageClient;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * A szerverek a title/subtitle mezőben gyakran úgynevezett PUA (Private Use Area)
 * karakterekkel rajzolnak custom HUD effekteket (ikonokat, csíkokat). Ezek a
 * karakterek csak akkor jelennek meg rendesen, ha a szerver által küldött
 * font/resource pack be van töltve, egyébként négyzetekként vagy más
 * karakterekként látszanak.
 *
 * Ez az osztály a beállítástól függően kiszűri ezeket a karaktereket a
 * title-ből és a subtitle-ből, a többi szöveget és a formázást megtartva.
 */
public final class PuaTitleFilter {

    private PuaTitleFilter() {
    }

    /** A beállítás be van-e kapcsolva. */
    public static boolean isEnabled() {
        try {
            return ActionbarStorageClient.getConfig().hidePuaInTitle;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Kiszűri a PUA karaktereket a megadott szövegből, ha a beállítás aktív.
     * Ha nincs benne ilyen karakter, a kapott példányt adja vissza változatlanul.
     */
    public static Text filter(Text text) {
        if (text == null || !isEnabled() || !hasPua(text)) return text;

        Text stripped = stripNode(text);
        return stripped == null ? Text.empty() : stripped;
    }

    /** Tartalmaz-e a szöveg (bármelyik résztagjában) PUA karaktert. */
    public static boolean hasPua(Text text) {
        if (text == null) return false;

        if (text.getContent() instanceof LiteralTextContent literal && containsPua(literal.string())) {
            return true;
        }

        for (Text sibling : text.getSiblings()) {
            if (hasPua(sibling)) return true;
        }
        return false;
    }

    /** Igaz, ha a kódcsoport a Private Use Area tartományba esik. */
    public static boolean isPua(int codePoint) {
        return (codePoint >= 0xE000 && codePoint <= 0xF8FF)          // BMP Private Use Area
                || (codePoint >= 0xF0000 && codePoint <= 0xFFFFD)   // Supplementary PUA-A
                || (codePoint >= 0x100000 && codePoint <= 0x10FFFD); // Supplementary PUA-B
    }

    public static boolean containsPua(String value) {
        if (value == null || value.isEmpty()) return false;

        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (isPua(codePoint)) return true;
            i += Character.charCount(codePoint);
        }
        return false;
    }

    public static String removePua(String value) {
        if (value == null || value.isEmpty()) return value;

        StringBuilder result = null;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            int length = Character.charCount(codePoint);

            if (isPua(codePoint)) {
                if (result == null) {
                    result = new StringBuilder(value.length());
                    result.append(value, 0, i);
                }
            } else if (result != null) {
                result.appendCodePoint(codePoint);
            }

            i += length;
        }
        return result == null ? value : result.toString();
    }

    /**
     * Rekurzívan végigmegy a szövegrészeken, megtartva a stílusokat.
     */
    private static Text stripNode(Text node) {
        if (node.getContent() instanceof LiteralTextContent literal) {
            MutableText result = Text.literal(removePua(literal.string()));
            result.setStyle(node.getStyle());
            for (Text sibling : node.getSiblings()) {
                result.append(stripNode(sibling));
            }
            return result;
        }

        // Nem literális szöveg (pl. translatable): a stílus megtartása mellett
        // a teljes szöveget egyszerű literállá alakítjuk.
        return Text.literal(removePua(node.getString())).setStyle(node.getStyle());
    }
}
