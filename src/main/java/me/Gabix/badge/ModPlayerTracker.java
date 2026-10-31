package me.Gabix.badge;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModPlayerTracker {

    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, String> offlineUuidCache = new ConcurrentHashMap<>();
    private static final Map<UUID, String> normalizedUuidCache = new ConcurrentHashMap<>();

    public static String normalizeUuid(Object uuid) {
        if (uuid == null) return null;
        if (uuid instanceof UUID u) {
            return normalizedUuidCache.computeIfAbsent(u,
                    k -> k.toString().replace("-", "").toLowerCase());
        }
        return uuid.toString().replace("-", "").toLowerCase().trim();
    }

    public static String getOfflineUuid(String username) {
        if (username == null || username.isEmpty()) return null;

        String cached = offlineUuidCache.get(username);
        if (cached != null) return cached;

        UUID offlineUuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        String normalized = normalizeUuid(offlineUuid);

        offlineUuidCache.put(username, normalized);
        return normalized;
    }

    public static void clear() {
        activeUsers.clear();
    }

    public static void updateUsers(Set<?> newUsers) {
        if (newUsers == null) return;
        synchronized (activeUsers) {
            activeUsers.clear();
            for (Object u : newUsers) {
                String norm = normalizeUuid(u);
                if (norm != null) activeUsers.add(norm);
            }
        }
    }

    public static boolean usesMod(Object uuid, String username) {
        String normalizedUuid = normalizeUuid(uuid);
        if (normalizedUuid != null && activeUsers.contains(normalizedUuid)) {
            return true;
        }

        String offlineUuid = getOfflineUuid(username);
        return offlineUuid != null && activeUsers.contains(offlineUuid);
    }

    public static boolean usesMod(Object uuid) {
        return usesMod(uuid, null);
    }
}