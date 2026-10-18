package me.Gabix.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WindowFocusEvent {
    private static final List<WindowFocusListener> listeners = new CopyOnWriteArrayList<>();

    public static void register(WindowFocusListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void onFocusChanged(boolean focused) {
        for (WindowFocusListener l : listeners) {
            try {
                if (focused) l.onFocusGained();
                else l.onFocusLost();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}