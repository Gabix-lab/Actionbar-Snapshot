package me.Gabix.norender.mixin;

import me.Gabix.WindowFocusEvent;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowFocusMixin {

    @Inject(method = "onWindowFocusChanged", at = @At("HEAD"))
    private void onFocusChange(long window, boolean focused, CallbackInfo ci) {
        WindowFocusEvent.onFocusChanged(focused);
    }
}