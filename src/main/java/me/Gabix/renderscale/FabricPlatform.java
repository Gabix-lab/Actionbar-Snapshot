package me.Gabix.renderscale;

import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatform implements Platform {
    @Override public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
    @Override public Platform.ModLoader loader() {
        return Platform.ModLoader.FABRIC;
    }
    @Override public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}