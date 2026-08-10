package me.Gabix.fpsreducer.mixin;

import me.Gabix.fpsreducer.FpsReducerLogic;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSystem.class, remap = false)
public class RenderSystemMixin {

    @Shadow
    private static double lastDrawTime;

    @Inject(
            method = "limitDisplayFPS",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void overrideLimitDisplayFPS(int fps, CallbackInfo ci) {
        if (FpsReducerLogic.isActive()) {
            double wakeupTime = lastDrawTime + (1.0 / (double) fps);
            double curTime = GLFW.glfwGetTime();

            while (curTime < wakeupTime) {
                // Amint fókuszba kerül a játék (visszaváltasz), AZONNAL kilép a várakozásból
                if (!FpsReducerLogic.isActive()) {
                    break;
                }

                double remaining = wakeupTime - curTime;
                GLFW.glfwWaitEventsTimeout(remaining);
                curTime = GLFW.glfwGetTime();
            }

            lastDrawTime = curTime;
            ci.cancel(); // Letiltjuk a Minecraft belső alvási várakoztatását
        }
    }
}