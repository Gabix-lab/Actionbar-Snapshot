package me.Gabix.cooldown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloneManager {
    public static final CloneManager INSTANCE = new CloneManager();

    private final Map<String, List<Integer>> cloneExpiryTicks = new HashMap<>(4);

    public void tick(int currentGlobalTick) {
        for (List<Integer> timers : cloneExpiryTicks.values()) {
            timers.removeIf(expiryTick -> currentGlobalTick >= expiryTick);
        }
    }

    public void spawnClone(String key, int currentGlobalTick, int maxCharges, int expiryTicks) {
        if (key == null) return;
        int limit = maxCharges > 0 ? maxCharges : 2;
        int expiry = expiryTicks > 0 ? expiryTicks : 227;
        List<Integer> timers = cloneExpiryTicks.computeIfAbsent(key, k -> new ArrayList<>(2));
        if (timers.size() < limit) {
            timers.add(currentGlobalTick + expiry);
        }
    }

    public int getActiveClonesCount(String key) {
        if (key == null) return 0;
        List<Integer> timers = cloneExpiryTicks.get(key);
        return timers == null ? 0 : timers.size();
    }

    public int getActiveClonesCount() {
        int total = 0;
        for (List<Integer> timers : cloneExpiryTicks.values()) {
            total += timers.size();
        }
        return total;
    }

    public int getNextExpiryTick(String key) {
        if (key == null) return 0;
        List<Integer> timers = cloneExpiryTicks.get(key);
        if (timers == null || timers.isEmpty()) return 0;
        int earliest = Integer.MAX_VALUE;
        for (int expiry : timers) {
            if (expiry < earliest) earliest = expiry;
        }
        return earliest;
    }

    public int getNextExpiryTick() {
        int earliest = Integer.MAX_VALUE;
        boolean found = false;
        for (List<Integer> timers : cloneExpiryTicks.values()) {
            for (int expiry : timers) {
                if (expiry < earliest) {
                    earliest = expiry;
                    found = true;
                }
            }
        }
        return found ? earliest : 0;
    }

    public void resetKey(String key) {
        if (key == null) return;
        cloneExpiryTicks.remove(key);
    }

    public void reset() {
        cloneExpiryTicks.clear();
    }
}