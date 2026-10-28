package me.Gabix.renderscale;

import net.fabricmc.api.ClientModInitializer;

public class FabricRenderScaleEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RenderScale.init();
    }
}