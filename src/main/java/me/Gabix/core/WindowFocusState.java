package me.Gabix.core;

import me.Gabix.api.WindowFocusEvent;
import me.Gabix.api.WindowFocusListener;

public final class WindowFocusState implements WindowFocusListener {
    private static volatile boolean focused = true;
    private static boolean registered = false;

    private WindowFocusState() {
    }

    public static synchronized void init() {
        if (registered) return;
        registered = true;
        WindowFocusEvent.register(new WindowFocusState());
    }

    public static boolean isFocused() {
        return focused;
    }

    @Override
    public void onFocusGained() {
        focused = true;
    }

    @Override
    public void onFocusLost() {
        focused = false;
    }
}