package me.Gabix.soundreducer;

import me.Gabix.api.WindowFocusEvent;
import net.fabricmc.api.ClientModInitializer;

public class SoundReducerMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WindowFocusEvent.register(new MuteOnInactive());
    }
}