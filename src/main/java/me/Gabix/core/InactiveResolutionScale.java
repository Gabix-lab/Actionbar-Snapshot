package me.Gabix.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL11;

public final class InactiveResolutionScale {
    private static boolean active = false;
    private static float factor = 1.0f;

    private InactiveResolutionScale() {
    }

    public static boolean isActive() {
        return active;
    }

    public static float getFactor() {
        return factor;
    }

    public static void update(MinecraftClient client) {
        if (client == null) return;

        Window window = client.getWindow();
        Framebuffer framebuffer = client.getFramebuffer();
        if (window == null || framebuffer == null || client.gameRenderer == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        boolean enabled = config != null && config.downscaleOnInactive && !WindowFocusState.isFocused();
        float newFactor = enabled
                ? Math.max(0.1f, Math.min(1.0f, config.inactiveResolutionScale / 100.0f))
                : 1.0f;

        WindowAccess access = (WindowAccess) (Object) window;
        int realWidth = Math.max(1, access.mvp$getRealFramebufferWidth());
        int realHeight = Math.max(1, access.mvp$getRealFramebufferHeight());

        int targetWidth = newFactor < 1.0f
                ? Math.max(1, Math.round(realWidth * newFactor))
                : realWidth;
        int targetHeight = newFactor < 1.0f
                ? Math.max(1, Math.round(realHeight * newFactor))
                : realHeight;

        boolean newActive = newFactor < 1.0f;
        if (active == newActive
                && factor == newFactor
                && framebuffer.textureWidth == targetWidth
                && framebuffer.textureHeight == targetHeight) {
            if (active) framebuffer.setTexFilter(GL11.GL_LINEAR);
            return;
        }

        try {
            active = false;
            framebuffer.resize(targetWidth, targetHeight, MinecraftClient.IS_SYSTEM_MAC);
            factor = newFactor;
            active = newActive;
            framebuffer.setTexFilter(active ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            client.gameRenderer.onResized(targetWidth, targetHeight);
        } catch (Throwable ignored) {
            active = false;
            factor = 1.0f;
        }
    }
}