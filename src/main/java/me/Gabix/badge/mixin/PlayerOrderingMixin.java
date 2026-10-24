package me.Gabix.badge.mixin;

import me.Gabix.badge.ModPlayerTracker;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.Normalizer;
import java.util.Comparator;

@Mixin(value = PlayerListHud.class, priority = 2000)
public class PlayerOrderingMixin {

    @Shadow
    @Final
    @Mutable
    private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void overrideOrdering(CallbackInfo ci) {
        Comparator<PlayerListEntry> original = ENTRY_ORDERING;

        ENTRY_ORDERING = (entry1, entry2) -> {
            boolean user1HasMod = ModPlayerTracker.usesMod(
                    entry1.getProfile().getId(), entry1.getProfile().getName());
            boolean user2HasMod = ModPlayerTracker.usesMod(
                    entry2.getProfile().getId(), entry2.getProfile().getName());

            if (user1HasMod && !user2HasMod) return -1;
            if (!user1HasMod && user2HasMod) return 1;

            if (user1HasMod && user2HasMod) {
                String name1 = entry1.getProfile().getName();
                String name2 = entry2.getProfile().getName();
                int cmp = compareNormalized(name1, name2);
                if (cmp != 0) return cmp;
            }

            return original.compare(entry1, entry2);
        };
    }

    private static int compareNormalized(String a, String b) {
        String ka = normalizeForSort(a);
        String kb = normalizeForSort(b);

        int len = Math.min(ka.length(), kb.length());
        for (int i = 0; i < len; i++) {
            char ca = ka.charAt(i);
            char cb = kb.charAt(i);
            if (ca != cb) {
                return Character.compare(ca, cb);
            }
        }
        return Integer.compare(ka.length(), kb.length());
    }

    private static String normalizeForSort(String s) {
        if (s == null) return "";
        String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if ((c >= '0' && c <= '9') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= 'a' && c <= 'z')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}