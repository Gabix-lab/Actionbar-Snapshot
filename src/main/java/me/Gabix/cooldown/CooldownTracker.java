package me.Gabix.cooldown;

import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.hud.HudMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownTracker {
    public static final CooldownTracker INSTANCE = new CooldownTracker();

    private final Map<String, Integer> leftExpiryTicks = new ConcurrentHashMap<>(16);
    private final Map<String, Integer> rightExpiryTicks = new ConcurrentHashMap<>(16);
    private final Map<String, List<Integer>> activeCloneTimers = new ConcurrentHashMap<>(16);

    public int getLeftExpiry(String key) {
        return leftExpiryTicks.getOrDefault(key, 0);
    }

    public int getRightExpiry(String key) {
        return rightExpiryTicks.getOrDefault(key, 0);
    }

    public void registerLeftClick(String key, int currentTick, int cooldownTicks) {
        leftExpiryTicks.put(key, currentTick + cooldownTicks);
    }

    public void registerRightClick(String key, int currentTick, int cooldownTicks) {
        rightExpiryTicks.put(key, currentTick + cooldownTicks);
        List<Integer> timers = activeCloneTimers.computeIfAbsent(key,
                k -> Collections.synchronizedList(new ArrayList<>(2)));
        synchronized (timers) {
            timers.add(currentTick + 226);
        }
    }

    public void resetCooldowns(String key) {
        leftExpiryTicks.put(key, 0);
        rightExpiryTicks.put(key, 0);
        List<Integer> timers = activeCloneTimers.get(key);
        if (timers != null) {
            synchronized (timers) {
                timers.clear();
            }
        }
    }

    public int getActiveClones(String key, int currentTick) {
        List<Integer> timers = activeCloneTimers.get(key);
        if (timers == null) return 0;
        synchronized (timers) {
            timers.removeIf(expire -> expire <= currentTick);
            return timers.size();
        }
    }

    public boolean isReady(String key, ActionbarStorageClient.SlotSettings settings, int currentTick, int currentMana, boolean isRightCheck) {
        if (key == null) return false;

        int maxMana = HudMod.getManaManager().getMaxMana();
        int tolerance = (int) (maxMana * 0.025);
        int effectiveMana = currentMana + tolerance;

        if (!isRightCheck) {
            return currentTick >= getLeftExpiry(key) && effectiveMana >= settings.leftMana();
        } else {
            boolean hasCapacity = (settings.maxCharges() <= 0)
                    || (getActiveClones(key, currentTick) < settings.maxCharges());
            return currentTick >= getRightExpiry(key)
                    && effectiveMana >= settings.rightMana()
                    && hasCapacity;
        }
    }
}