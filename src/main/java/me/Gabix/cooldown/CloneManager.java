package me.Gabix.cooldown;

import java.util.ArrayList;
import java.util.List;

public class CloneManager {
    public static final CloneManager INSTANCE = new CloneManager();

    private final List<Integer> cloneExpiryTicks = new ArrayList<>(2);

    public void tick(int currentGlobalTick) {
        cloneExpiryTicks.removeIf(expiryTick -> currentGlobalTick >= expiryTick);
    }

    public void spawnClone(int currentGlobalTick, int maxCharges) {
        int limit = maxCharges > 0 ? maxCharges : 2;
        if (cloneExpiryTicks.size() < limit) {
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