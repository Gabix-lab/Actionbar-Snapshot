package me.Gabix.norender;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class NoRenderMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NoRenderEngine.init();

        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "3D Rendereles Ki/Be",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "NoRender"
        ));

        KeyBinding toggleInactive = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "NoRender Ablak Inaktivnal",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "NoRender"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.wasPressed()) {
                NoRenderEngine.toggle(client);
            }
            while (toggleInactive.wasPressed()) {
                NoRenderEngine.toggleInactive(client);
            }
        });
    }
}