package me.Gabix.motionblur;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix4f;

import ladysnake.satin.api.event.PostWorldRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;

public class NaturalMotionBlurMod implements ClientModInitializer {
    private static ManagedShaderEffect motionBlurEffect;
    private static boolean enabled = false;
    private static KeyBinding toggleKey;

    private static Vec3d prevCameraPos = Vec3d.ZERO;
    private static Matrix4f prevModelView = new Matrix4f();
    private static Matrix4f prevProjection = new Matrix4f();

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "MotionBlur ki/be kapcsolás",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "MotionBlur"
        ));

        motionBlurEffect = ShaderEffectManager.getInstance()
                .manage(new Identifier("mvp", "shaders/post/motion_blur.json"));

        PostWorldRenderCallback.EVENT.register((camera, tickDelta, nanoTime) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            if (enabled && motionBlurEffect != null) {
                Vec3d currentCameraPos = camera.getPos();

                // Uniform alapértékek
                motionBlurEffect.setUniformValue("BlendFactor", 0.7f);
                motionBlurEffect.setUniformValue("motionBlurSamples", 7);
                motionBlurEffect.setUniformValue("useDepth", 1);
                motionBlurEffect.setUniformValue("blurAlgorithm", 0);

                // Kamera pozíciók
                motionBlurEffect.setUniformValue("cameraPos",
                        (float) currentCameraPos.x, (float) currentCameraPos.y, (float) currentCameraPos.z);
                motionBlurEffect.setUniformValue("prevCameraPos",
                        (float) prevCameraPos.x, (float) prevCameraPos.y, (float) prevCameraPos.z);

                // Mátrixok kiszámítása és átadása
                Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
                Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());

                Matrix4f mvInverse = new Matrix4f(modelView).invert();
                Matrix4f projInverse = new Matrix4f(projection).invert();

                motionBlurEffect.setUniformValue("mvInverse", mvInverse);
                motionBlurEffect.setUniformValue("projInverse", projInverse);
                motionBlurEffect.setUniformValue("prevModelView", prevModelView);
                motionBlurEffect.setUniformValue("prevProjection", prevProjection);

                motionBlurEffect.render(tickDelta);

                prevCameraPos = currentCameraPos;
                prevModelView = modelView;
                prevProjection = projection;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;

                if (client.player != null) {
                    Text statusText = enabled
                            ? Text.literal("ON").formatted(Formatting.GREEN)
                            : Text.literal("OFF").formatted(Formatting.RED);

                    Text message = Text.literal("§7[GabixMod] §fMotionBlur: ").append(statusText);

                    client.player.sendMessage(message, true);
                }
            }
        });
    }
}