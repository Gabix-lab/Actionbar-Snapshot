package me.Gabix.cooldown;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CooldownMod implements ClientModInitializer {
    public static KeyBinding resetCooldownKeyBinding;

    @Override
    public void onInitializeClient() {
        resetCooldownKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Cooldown Nullázása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SEMICOLON, "MVP Mod"));
    }
}