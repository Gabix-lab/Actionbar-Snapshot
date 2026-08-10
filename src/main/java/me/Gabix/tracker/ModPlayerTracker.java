package me.Gabix.tracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModPlayerTracker {

    private static final Set<UUID> activeUsers = Collections.synchronizedSet(new HashSet<>());

    public static void registerUser(UUID uuid) {
        if (uuid != null) {
            activeUsers.add(uuid);
        }
    }

    public static void unregisterUser(UUID uuid) {
        if (uuid != null) {
            activeUsers.remove(uuid);
        }
    }

    /**
     * A Mixin osztályok által hívott metódus.
     * Ellenőrzi, hogy az adott UUID-val rendelkező játékos használja-e a modot.
     */
    public static boolean usesMod(UUID uuid) {
        return uuid != null && activeUsers.contains(uuid);
    }

    // Alias a korábbi hívásokhoz, ha szükséges
    public static boolean isUserActive(UUID uuid) {
        return usesMod(uuid);
    }

    public static void clear() {
        activeUsers.clear();
    }

    public static Set<UUID> getActiveUsers() {
        return new HashSet<>(activeUsers);
    }
}