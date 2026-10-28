package me.Gabix.fullbright.mixin;

import me.Gabix.fullbright.event.ToggleEvent;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Inject(method = "write", at = @At("HEAD"))
    private void onWriteHead(CallbackInfo ci) {
        ToggleEvent.isSaving = true;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void onWriteReturn(CallbackInfo ci) {
        ToggleEvent.isSaving = false;
    }
}