package me.Gabix.developer;

public final class DevAccess {
    private static volatile boolean toolsEnabled = false;
    private static volatile boolean tabUnlocked = false;

    private DevAccess() {}

    public static boolean isToolsEnabled() { return toolsEnabled; }
    public static void setToolsEnabled(boolean enabled) { toolsEnabled = enabled; }

    public static boolean isTabUnlocked() { return tabUnlocked; }
    public static void unlockTab() { tabUnlocked = true; }
}