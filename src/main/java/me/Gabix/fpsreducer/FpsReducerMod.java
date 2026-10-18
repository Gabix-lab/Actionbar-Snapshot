package me.Gabix.fpsreducer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class FpsReducerMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FpsManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> FpsManager.tick());
    }
}