package me.Gabix.universal;

import net.fabricmc.api.ModInitializer;

public final class UniversalMain implements ModInitializer {
    @Override
    public void onInitialize() {
        UniversalBootstrap.invoke("me.Gabix.core.ActionbarStorage", "onInitialize");
    }
}
