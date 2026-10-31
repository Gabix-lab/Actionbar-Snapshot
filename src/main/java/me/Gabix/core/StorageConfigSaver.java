package me.Gabix.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Debounced async config save.
 *
 * <p>A UI toggle-ok és a boss detection gyakran hívják a save()-t. A szinkron
 * JSON írás render thread-en lag spike-ot okoz. Ehelyett a kéréseket
 * összegyűjtjük, és 500 ms csend után egy háttérszálon írjuk ki egyszer.</p>
 */
public final class StorageConfigSaver {
    private static final long DEBOUNCE_MS = 500L;
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MVP-ConfigSaver");
                t.setDaemon(true);
                return t;
            });

    private static final AtomicBoolean PENDING = new AtomicBoolean(false);
    private static volatile ScheduledFuture<?> scheduledTask;
    private static volatile StorageConfig pendingConfig;

    private StorageConfigSaver() {
    }

    /** Kéri a config mentését. A tényleges írás 500 ms csend után történik. */
    public static void requestSave(StorageConfig config) {
        if (config == null) return;
        pendingConfig = config;
        PENDING.set(true);

        ScheduledFuture<?> previous = scheduledTask;
        if (previous != null) previous.cancel(false);

        scheduledTask = EXECUTOR.schedule(StorageConfigSaver::flushPending, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /** Azonnali mentés (pl. UI bezárásakor). */
    public static void saveNow(StorageConfig config) {
        if (config == null) return;

        // A soron következő debounce-olt írás felesleges, ha most azonnal mentünk.
        ScheduledFuture<?> previous = scheduledTask;
        if (previous != null) previous.cancel(false);

        pendingConfig = config;
        // saveNow-nak akkor is mentenie kell, ha éppen nincs függő kérés.
        PENDING.set(true);
        flushPending();
    }

    /**
     * A tényleges írás. Synchronized, mert a debounce-olt háttérszál és a
     * saveNow() render thread egyszerre is elindulhat; a JSON írás nem
     * konkurálhat önmagával.
     */
    private static synchronized void flushPending() {
        if (!PENDING.compareAndSet(true, false)) return;
        StorageConfig config = pendingConfig;
        if (config == null) return;
        try {
            config.save();
        } catch (Throwable t) {
            System.err.println("[MVP] Config save failed: " + t);
        }
    }
}
