package me.Gabix.mixin;

import me.Gabix.ActionbarReader;
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
        ActionbarReader.updateText(message);
    }
}