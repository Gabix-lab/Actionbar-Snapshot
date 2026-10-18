package me.Gabix.boss;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BossMod implements ClientModInitializer {
    public static KeyBinding toggleContinuousKeyBinding;

    @Override
    public void onInitializeClient() {
        toggleContinuousKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Kesz Felirat Mod Valtas", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_APOSTROPHE, "MVP Mod"));
        ClientTickEvents.END_CLIENT_TICK.register(BossMonitor::tick);
    }
}