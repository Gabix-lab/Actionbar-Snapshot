package me.Gabix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
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
    private int newArrowsThisTick = 0;

    public void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        newArrowsThisTick = 0;

        if (client != null && client.player != null && client.world != null) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof ArrowEntity arrow)) continue;
                if (arrow.getOwner() != client.player) continue;

                if (seenArrows.add(arrow.getId())) {
                    shots.addLast(now);
                    newArrowsThisTick++;
                }
            }
        }

        while (!shots.isEmpty() && now - shots.peekFirst() > SAMPLE_WINDOW_MS) {
            shots.pollFirst();
        }
        currentAps = shots.size();
    }

    public int getAps() {
        return currentAps;
    }

    /** Az előző tick óta újonnan megjelent, saját nyilak száma. */
    public int getNewArrowsThisTick() {
        return newArrowsThisTick;
    }

    public void reset() {
        seenArrows.clear();
        shots.clear();
        currentAps = 0;
        newArrowsThisTick = 0;
    }
}
