package me.Gabix;

import net.minecraft.client.MinecraftClient;
import java.util.ArrayDeque;
import java.util.Deque;

public class FpsCpsCounter {
    private static final int SAMPLE_WINDOW_MS = 1000;
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private int currentFps = 0;
    private int currentLeftCps = 0;
    private int currentRightCps = 0;

    public void tick(MinecraftClient client) {
        if (client != null) {
            this.currentFps = client.getCurrentFps();
        }
        long now = System.currentTimeMillis();
        cleanClicks(leftClicks, now);
        cleanClicks(rightClicks, now);
        currentLeftCps = leftClicks.size();
        currentRightCps = rightClicks.size();
    }

    private void cleanClicks(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > SAMPLE_WINDOW_MS) {
            clicks.pollFirst();
        }
    }

    public void registerLeftClick() {
        long now = System.currentTimeMillis();
        cleanClicks(leftClicks, now);
        leftClicks.addLast(now);
        currentLeftCps = leftClicks.size();
    }

    public void registerRightClick() {
        long now = System.currentTimeMillis();
        cleanClicks(rightClicks, now);
        rightClicks.addLast(now);
        currentRightCps = rightClicks.size();
    }

    public int getFps() { return currentFps; }
    public int getLeftCps() { return currentLeftCps; }
    public int getRightCps() { return currentRightCps; }
}