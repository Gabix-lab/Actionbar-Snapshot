package me.Gabix.universal;

import net.fabricmc.api.ClientModInitializer;

public final class UniversalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Satin is shaded into the variant namespace, so its entrypoint is no
        // longer discovered by Fabric Loader and has to be invoked explicitly.
        UniversalBootstrap.invoke("ladysnake.satin.Satin", "onInitializeClient");
        UniversalBootstrap.invoke("me.Gabix.core.ActionbarStorageClient", "onInitializeClient");
        UniversalBootstrap.invoke("me.Gabix.renderscale.FabricRenderScaleEntrypoint", "onInitializeClient");
    }
}
