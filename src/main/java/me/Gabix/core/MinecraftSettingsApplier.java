package me.Gabix.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundCategory;
import org.lwjgl.glfw.GLFW;

public final class MinecraftSettingsApplier {

    private MinecraftSettingsApplier() {}

    public static void applyPreset(MinecraftClient client) {
        if (client == null || client.options == null) return;

        GameOptions o = client.options;

        try { o.getAutoJump().setValue(false); } catch (Throwable ignored) {}

        try { o.getChatColors().setValue(true); } catch (Throwable ignored) {}
        try { o.getChatLinks().setValue(true); } catch (Throwable ignored) {}
        try { o.getChatLinksPrompt().setValue(true); } catch (Throwable ignored) {}
        try { o.getEnableVsync().setValue(false); } catch (Throwable ignored) {}
        try { o.getEntityShadows().setValue(false); } catch (Throwable ignored) {}
        try { o.getForceUnicodeFont().setValue(false); } catch (Throwable ignored) {}
        try { o.getDiscreteMouseScroll().setValue(false); } catch (Throwable ignored) {}
        try { o.getInvertYMouse().setValue(false); } catch (Throwable ignored) {}
        try { o.getRealmsNotifications().setValue(true); } catch (Throwable ignored) {}
        try { o.getReducedDebugInfo().setValue(false); } catch (Throwable ignored) {}
        try { o.getShowSubtitles().setValue(false); } catch (Throwable ignored) {}
        try { o.getDirectionalAudio().setValue(false); } catch (Throwable ignored) {}
        try { o.getTouchscreen().setValue(false); } catch (Throwable ignored) {}
        try { o.getFullscreen().setValue(false); } catch (Throwable ignored) {}
        try { o.getBobView().setValue(false); } catch (Throwable ignored) {}

        try { o.getMouseSensitivity().setValue(0.3380281690140845); } catch (Throwable ignored) {}
        try { o.getFov().setValue(110); } catch (Throwable ignored) {}
        try { o.getFovEffectScale().setValue(0.0); } catch (Throwable ignored) {}
        try { o.getDarknessEffectScale().setValue(0.0); } catch (Throwable ignored) {}
        try { o.getDamageTiltStrength().setValue(0.0); } catch (Throwable ignored) {}

        try { o.getViewDistance().setValue(8); } catch (Throwable ignored) {}
        try { o.getSimulationDistance().setValue(12); } catch (Throwable ignored) {}
        try { o.getEntityDistanceScaling().setValue(5.0); } catch (Throwable ignored) {}
        try { o.getGuiScale().setValue(2); } catch (Throwable ignored) {}
        try { o.getMaxFps().setValue(260); } catch (Throwable ignored) {}
        try { o.getBiomeBlendRadius().setValue(7); } catch (Throwable ignored) {}
        try { o.getParticles().setValue(ParticlesMode.DECREASED); } catch (Throwable ignored) {}
        try { o.getGraphicsMode().setValue(GraphicsMode.FANCY); } catch (Throwable ignored) {}
        try { o.getCloudRenderMode().setValue(CloudRenderMode.OFF); } catch (Throwable ignored) {}
        try { o.getAo().setValue(true); } catch (Throwable ignored) {}

        try { o.getSoundVolumeOption(SoundCategory.MASTER).setValue(0.37748344370860926); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.MUSIC).setValue(0.0); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.RECORDS).setValue(0.30985915492957744); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.WEATHER).setValue(0.0); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.BLOCKS).setValue(0.5035211267605634); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.HOSTILE).setValue(0.2007042253521127); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.NEUTRAL).setValue(0.20422522535211267606); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.PLAYERS).setValue(0.15492957746478872); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.AMBIENT).setValue(0.4084507042253521); } catch (Throwable ignored) {}
        try { o.getSoundVolumeOption(SoundCategory.VOICE).setValue(0.0); } catch (Throwable ignored) {}

        applyKeyBindings(o);

        try { o.write(); } catch (Throwable ignored) {}
    }

    private static void applyKeyBindings(GameOptions o) {
        try {
            InputUtil.Key keyR = InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_R);
            InputUtil.Key keyQ = InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_Q);
            InputUtil.Key unbound = InputUtil.UNKNOWN_KEY;

            if (o.togglePerspectiveKey != null) {
                o.togglePerspectiveKey.setBoundKey(keyR);
            }
            if (o.dropKey != null) {
                o.dropKey.setBoundKey(unbound);
            }
            if (o.hotbarKeys != null && o.hotbarKeys.length > 6 && o.hotbarKeys[6] != null) {
                o.hotbarKeys[6].setBoundKey(keyQ);
            }

            KeyBinding.updateKeysByCode();
        } catch (Throwable ignored) {}
    }
}