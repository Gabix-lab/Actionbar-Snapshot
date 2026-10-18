package me.Gabix.fullbright;

import me.Gabix.fullbright.event.ToggleEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class FullbrightMod implements ClientModInitializer {
    private static KeyBinding fullbrightKeyBinding;
    private static boolean startupChecked = false;
    private static boolean wasKeyDown = false;

    @Override
    public void onInitializeClient() {
        fullbrightKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mvp.fullbright.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "Fullbright"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!startupChecked && client.player != null) {
                ToggleEvent.startupCheck();
                startupChecked = true;
            }

            boolean isKeyDown = fullbrightKeyBinding.isPressed();
            if (isKeyDown && !wasKeyDown) {
                ToggleEvent.onHotkeyPress();
            }
            wasKeyDown = isKeyDown;
        });
    }
}