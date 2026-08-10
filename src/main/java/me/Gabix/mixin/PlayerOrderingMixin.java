package me.Gabix.mixin;

import me.Gabix.tracker.ModPlayerTracker;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(PlayerListHud.class)
public class PlayerOrderingMixin {

    @Shadow
    @Final
    @Mutable
    private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void overrideOrdering(CallbackInfo ci) {
        Comparator<PlayerListEntry> original = ENTRY_ORDERING;

        ENTRY_ORDERING = (entry1, entry2) -> {
            boolean user1HasMod = ModPlayerTracker.usesMod(entry1.getProfile().getId());
            boolean user2HasMod = ModPlayerTracker.usesMod(entry2.getProfile().getId());

            if (user1HasMod && !user2HasMod) return -1;
            if (!user1HasMod && user2HasMod) return 1;

            return original.compare(entry1, entry2);
        };
    }
}