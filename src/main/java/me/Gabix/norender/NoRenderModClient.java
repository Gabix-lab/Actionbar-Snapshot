package me.Gabix.norender;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class NoRenderModClient implements ClientModInitializer {
    private static KeyBinding noRenderKeyBinding;
    private static KeyBinding noRenderTimerKeyBinding;
    private static KeyBinding noRenderInactiveKeyBinding;

    @Override
    public void onInitializeClient() {
        noRenderKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "3D Renderelés Ki/Be", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, "NoRender"));
        noRenderTimerKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "NoRender Időzítő Ki/Be", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, "NoRender"));
        noRenderInactiveKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "NoRender Ablak Inaktívnál", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "NoRender"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            NoRenderManager.tick(client);
            while (noRenderKeyBinding.wasPressed()) NoRenderManager.toggle(client);
            while (noRenderTimerKeyBinding.wasPressed()) NoRenderManager.toggleTimer(client);
            while (noRenderInactiveKeyBinding.wasPressed()) NoRenderManager.toggleInactive(client);
        });
    }
}