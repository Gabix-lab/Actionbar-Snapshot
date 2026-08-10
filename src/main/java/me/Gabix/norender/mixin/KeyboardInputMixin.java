package me.Gabix.norender.mixin;

import me.Gabix.norender.NoRenderEngine;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardInputMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onInput(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        NoRenderEngine.onUserInput();
    }
}