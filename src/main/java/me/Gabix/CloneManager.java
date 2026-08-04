package me.Gabix;

import java.util.ArrayList;
import java.util.List;

public class CloneManager {
    private final List<Integer> cloneExpiryTicks = new ArrayList<>();

    public void tick(int currentGlobalTick) {
        cloneExpiryTicks.removeIf(expiryTick -> currentGlobalTick >= expiryTick);
    }

    public void spawnClone(int currentGlobalTick) {
        if (cloneExpiryTicks.size() < 2) {
            cloneExpiryTicks.add(currentGlobalTick + 227);
        }
    }

    public int getActiveClonesCount() {
        return cloneExpiryTicks.size();
    }

    public int getNextExpiryTick() {
        if (cloneExpiryTicks.isEmpty()) return 0;
        int earliest = Integer.MAX_VALUE;
        for (int expiry : cloneExpiryTicks) {
            if (expiry < earliest) earliest = expiry;
        }
        return earliest;
    }
}