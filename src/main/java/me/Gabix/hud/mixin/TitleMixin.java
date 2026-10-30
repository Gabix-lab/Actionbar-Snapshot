package me.Gabix.hud.mixin;

import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.hud.PuaTitleFilter;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A szerverek által küldött title/subtitle szövegből kiszűri a PUA karaktereket,
 * ha a "Szerver HUD effektek (PUA) elrejtése" beállítás be van kapcsolva.
 */
@Mixin(InGameHud.class)
public class TitleMixin {

    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private Text mvp$filterTitle(Text title) {
        return PuaTitleFilter.filter(title, ActionbarStorageClient.getConfig().hidePuaInTitle);
    }

    @ModifyVariable(method = "setSubtitle", at = @At("HEAD"), argsOnly = true)
    private Text mvp$filterSubtitle(Text subtitle) {
        return PuaTitleFilter.filter(subtitle, ActionbarStorageClient.getConfig().hidePuaInSubtitle);
    }
}
