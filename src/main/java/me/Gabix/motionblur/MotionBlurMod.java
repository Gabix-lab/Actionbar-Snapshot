package me.Gabix.motionblur;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.event.PostWorldRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
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
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class MotionBlurMod implements ClientModInitializer {
    private static ManagedShaderEffect motionBlurEffect;
    private static boolean enabled = false;
    private static boolean needReset = false;
    private static KeyBinding toggleKey;
    private static Vec3d prevCameraPos = Vec3d.ZERO;

    private static final Matrix4f prevModelView = new Matrix4f();
    private static final Matrix4f prevProjection = new Matrix4f();
    private static final Matrix4f tempModelView = new Matrix4f();
    private static final Matrix4f tempProjection = new Matrix4f();
    private static final Matrix4f tempMvInverse = new Matrix4f();
    private static final Matrix4f tempProjInverse = new Matrix4f();

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "MotionBlur ki/be",
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

                tempModelView.set(RenderSystem.getModelViewMatrix());
                tempProjection.set(RenderSystem.getProjectionMatrix());
                tempMvInverse.set(tempModelView).invert();
                tempProjInverse.set(tempProjection).invert();

                if (needReset) {
                    prevCameraPos = currentCameraPos;
                    prevModelView.set(tempModelView);
                    prevProjection.set(tempProjection);
                    needReset = false;
                }

                motionBlurEffect.setUniformValue("BlendFactor", 0.7f);
                motionBlurEffect.setUniformValue("motionBlurSamples", 7);
                motionBlurEffect.setUniformValue("useDepth", 1);
                motionBlurEffect.setUniformValue("blurAlgorithm", 0);

                motionBlurEffect.setUniformValue("cameraPos",
                        (float) currentCameraPos.x, (float) currentCameraPos.y, (float) currentCameraPos.z);
                motionBlurEffect.setUniformValue("prevCameraPos",
                        (float) prevCameraPos.x, (float) prevCameraPos.y, (float) prevCameraPos.z);

                motionBlurEffect.setUniformValue("mvInverse", tempMvInverse);
                motionBlurEffect.setUniformValue("projInverse", tempProjInverse);
                motionBlurEffect.setUniformValue("prevModelView", prevModelView);
                motionBlurEffect.setUniformValue("prevProjection", prevProjection);

                motionBlurEffect.render(tickDelta);

                prevCameraPos = currentCameraPos;
                prevModelView.set(tempModelView);
                prevProjection.set(tempProjection);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;

                if (enabled) {
                    needReset = true;
                }

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