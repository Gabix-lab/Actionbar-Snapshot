package me.Gabix.renderscale;

public interface Platform {
    boolean isModLoaded(String modId);
    ModLoader loader();
    boolean isDevelopmentEnvironment();

    default boolean isDebug() { return isDevelopmentEnvironment(); }

    enum ModLoader { FABRIC, NEOFORGE, FORGE, QUILT }
}