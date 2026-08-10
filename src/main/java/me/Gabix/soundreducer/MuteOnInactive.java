package me.Gabix.soundreducer;

import me.Gabix.ActionbarStorageClient;
import me.Gabix.StorageConfig;
import me.Gabix.WindowFocusListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;

public class MuteOnInactive implements WindowFocusListener {
    private double previousMasterVolume = -1.0;
    private boolean muted = false;

    @Override
    public void onFocusLost() {
        MinecraftClient client = MinecraftClient.getInstance();
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null || !config.muteOnInactive) {
            return;
        }
        if (client.options != null) {
            double current = client.options.getSoundVolumeOption(SoundCategory.MASTER).getValue();
            if (current > 0.0) {
                previousMasterVolume = current;

                client.options.getSoundVolumeOption(SoundCategory.MASTER).setValue(0.0);

                if (client.getSoundManager() != null) {
                    client.getSoundManager().stopAll();
                }

                muted = true;
            }
        }
    }

    @Override
    public void onFocusGained() {
        if (muted) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options != null && previousMasterVolume >= 0.0) {

                client.options.getSoundVolumeOption(SoundCategory.MASTER).setValue(previousMasterVolume);
                previousMasterVolume = -1.0;
                muted = false;
            }
        }
    }
}