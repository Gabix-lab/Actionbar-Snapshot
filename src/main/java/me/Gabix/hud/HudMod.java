package me.Gabix.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class HudMod implements ClientModInitializer {
    private static final ManaManager manaManager = new ManaManager();
    private static final FpsCpsCounter fpsCpsCounter = new FpsCpsCounter();

    public static ManaManager getManaManager() {
        return manaManager;
    }

    public static FpsCpsCounter getFpsCpsCounter() {
        return fpsCpsCounter;
    }

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(CustomHudRenderer::render);
    }

    public static void registerLeftClick() {
        fpsCpsCounter.registerLeftClick();
    }

    public static void registerRightClick() {
        fpsCpsCounter.registerRightClick();
    }
}