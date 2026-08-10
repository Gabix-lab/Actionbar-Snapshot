package me.Gabix;

import java.util.ArrayList;
import java.util.List;

public class WindowFocusEvent {
    private static final List<WindowFocusListener> listeners = new ArrayList<>();

    public static void register(WindowFocusListener listener) {
        listeners.add(listener);
    }

    public static void onFocusChanged(boolean focused) {
        for (WindowFocusListener l : listeners) {
            if (focused) {
                l.onFocusGained();
            } else {
                l.onFocusLost();
            }
        }
    }
}