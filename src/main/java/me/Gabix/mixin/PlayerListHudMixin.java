package me.Gabix.mixin;

import me.Gabix.tracker.ModPlayerTracker;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void addIconToTabName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (ModPlayerTracker.usesMod(entry.getProfile().getId())) {
            Text originalName = cir.getReturnValue();
            Text baseName = (originalName != null) ? originalName : Text.literal(entry.getProfile().getName());

            MutableText customName = Text.literal("\uE001 ").append(baseName);
            cir.setReturnValue(customName);
        }
    }
}