package me.Gabix.hud.mixin;

import me.Gabix.hud.ActionbarParser;
import me.Gabix.hud.HudMod;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ActionbarMixin {

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void onActionbarUpdate(Text message, boolean tinted, CallbackInfo ci) {
        ActionbarParser.updateText(message, HudMod.getManaManager());
    }
}