package me.Gabix.universal;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Selects the relocated implementation that matches the running Minecraft version.
 *
 * <p>The universal jar contains one complete copy of the mod for every supported
 * Minecraft version.  Each copy lives in its own top-level package so that the
 * different {@code Screen}/{@code InGameHud} bytecode signatures can coexist in
 * one jar.</p>
 */
public final class VersionSelector {
    public static final String V1194 = "mvp_1194";
    public static final String V120 = "mvp_120";
    public static final String V1201 = "mvp_1201";

    private static final String PREFIX = resolvePrefix();

    private VersionSelector() {
    }

    public static String prefix() {
        return PREFIX;
    }

    private static String resolvePrefix() {
        String minecraft = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElseThrow(() -> new IllegalStateException("MVP: Minecraft mod container is missing"));

        // Check 1.20.1 before 1.20 because both strings share a prefix.
        if (minecraft.startsWith("1.20.1")) {
            return V1201;
        }
        if (minecraft.startsWith("1.20")) {
            return V120;
        }
        if (minecraft.startsWith("1.19.4")) {
            return V1194;
        }

        throw new IllegalStateException("MVP: unsupported Minecraft version " + minecraft);
    }
}
