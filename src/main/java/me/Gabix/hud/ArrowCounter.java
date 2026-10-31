package me.Gabix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.ArrowEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Számlálja a játékos által kilőtt nyilakat (íj, számszeríj, multishot)
 * egy 1 másodperces csúszó ablakban.
 */
public class ArrowCounter {
    private static final int SAMPLE_WINDOW_MS = 1000;

    private final Set<Integer> seenArrows = new HashSet<>();
    private final Deque<Long> shots = new ArrayDeque<>();
    private int currentAps = 0;

    /**
     * A ClientWorldMixin hívja, amikor egy nyíl ténylegesen spawnol.
     * A seenArrows továbbra is véd a dupla spawn / újraspawn ellen.
     */
    public void onArrowSpawned(ArrowEntity arrow) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || arrow == null) return;
        if (arrow.getOwner() != client.player) return;
        if (!seenArrows.add(arrow.getId())) return;
        shots.addLast(System.currentTimeMillis());
    }

    public void tick(MinecraftClient client) {
        // Csak a lejárt shot-okat tisztítjuk; entitáslistát NEM iterálunk.
        long now = System.currentTimeMillis();
        while (!shots.isEmpty() && now - shots.peekFirst() > SAMPLE_WINDOW_MS) {
            shots.pollFirst();
        }
        currentAps = shots.size();
    }

    public int getAps() {
        return currentAps;
    }

    public void reset() {
        seenArrows.clear();
        shots.clear();
        currentAps = 0;
    }
}
