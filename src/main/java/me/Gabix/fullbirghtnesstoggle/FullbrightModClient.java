package me.Gabix.fullbirghtnesstoggle;

import me.Gabix.fullbirghtnesstoggle.event.ToggleEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class FullbrightModClient implements ClientModInitializer {
    private static KeyBinding fullbrightKeyBinding;

    @Override
    public void onInitializeClient() {
        fullbrightKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Fullbright Ki/Be",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "Fullbright"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            ToggleEvent.startupCheck();
            while (fullbrightKeyBinding.wasPressed()) {
                ToggleEvent.onHotkeyPress();
            }
        });
    }
}