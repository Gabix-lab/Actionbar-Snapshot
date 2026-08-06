package me.Gabix.developer;

import me.Gabix.ActionbarReader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DeveloperModClient implements ClientModInitializer {
    private static KeyBinding readActionbarKeyBinding;
    private static KeyBinding readBossBarsKeyBinding;
    private static KeyBinding itemInfoKeyBinding;

    private boolean readActionbarPressed = false;
    private boolean readBossBarsPressed = false;
    private boolean itemInfoPressed = false;

    @Override
    public void onInitializeClient() {
        readActionbarKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Actionbar Nyers Beolvasása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "Developer Tools"));
        readBossBarsKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "BossBar Név Beolvasása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "Developer Tools"));
        itemInfoKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Item Info Megjelenítése", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, "Developer Tools"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (readActionbarKeyBinding.isPressed()) {
                if (!readActionbarPressed) {
                    readActionbarPressed = true;
                    ActionbarReader.handleReadActionbar(client);
                }
            } else {
                readActionbarPressed = false;
            }

            if (readBossBarsKeyBinding.isPressed()) {
                if (!readBossBarsPressed) {
                    readBossBarsPressed = true;
                    ActionbarReader.handleReadBossBars(client);
                }
            } else {
                readBossBarsPressed = false;
            }

            if (itemInfoKeyBinding.isPressed()) {
                if (!itemInfoPressed) {
                    itemInfoPressed = true;
                    ActionbarReader.handleItemInfo(client);
                }
            } else {
                itemInfoPressed = false;
            }
        });
    }
}