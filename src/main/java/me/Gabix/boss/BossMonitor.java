package me.Gabix.boss;

import me.Gabix.boss.mixin.BossBarHudAccessor;
import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import me.Gabix.core.StorageConfigSaver;
import me.Gabix.hud.HudMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossMonitor {
    private static boolean continuousTitle = true;
    private static int currentDamage = 0;
    private static boolean wasDamageReady = false;
    private static long lastDamageUpdateTime = 0;
    private static Field cachedBossBarsField = null;

    /** Utolsó látott boss bar név UUID-nként; csak referencia-összehasonlítás. */
    private static final Map<UUID, Text> lastBossNames = new HashMap<>();
    /** 0 = unknown, 1 = accessor működik, 2 = reflection fallback. */
    private static int bossBarsAccessorState = 0;

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        // A boss detection már nem poll: a BossBarHudMixin eseményre hívja.
        if (currentDamage > 0 && System.currentTimeMillis() - lastDamageUpdateTime > 30000) {
            currentDamage = 0;
            HudMod.getManaManager().setCurrentDamage(0);
        }

        int targetDamage = 5000;
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config != null) {
            targetDamage = config.getTargetBossDamage();
        }

        boolean isNowReady = (targetDamage != Integer.MAX_VALUE)
                && currentDamage >= targetDamage
                && currentDamage > 0;

        if (isNowReady && !wasDamageReady) {
            if (client.inGameHud != null) {
                if (continuousTitle) {
                    client.inGameHud.setTitleTicks(5, 72000, 5);
                } else {
                    client.inGameHud.setTitleTicks(5, 20, 5);
                }
                client.inGameHud.setTitle(Text.literal("§a§l★ KÉSZ ★"));
            }
        } else if (!isNowReady && wasDamageReady) {
            if (client.inGameHud != null) {
                client.inGameHud.setTitleTicks(0, 0, 0);
                client.inGameHud.setTitle(Text.empty());
            }
        }
        wasDamageReady = isNowReady;

        while (BossMod.toggleContinuousKeyBinding.wasPressed()) {
            continuousTitle = !continuousTitle;
            if (isNowReady) {
                if (client.inGameHud != null) {
                    if (continuousTitle) {
                        client.inGameHud.setTitleTicks(0, 72000, 5);
                    } else {
                        client.inGameHud.setTitleTicks(0, 20, 5);
                    }
                    client.inGameHud.setTitle(Text.literal("§a§l★ KÉSZ ★"));
                }
            } else {
                wasDamageReady = false;
                if (client.inGameHud != null) {
                    client.inGameHud.setTitleTicks(0, 0, 0);
                    client.inGameHud.setTitle(Text.empty());
                    client.inGameHud.setSubtitle(Text.empty());
                }
            }
            if (client.player != null) {
                if (continuousTitle) {
                    client.player.sendMessage(Text.literal("§a[Mod] Kész felirat: §eFolyamatosan a képernyőn marad"), false);
                } else {
                    client.player.sendMessage(Text.literal("§a[Mod] Kész felirat: §eCsak egyszer villan fel"), false);
                }
            }
        }
    }

    /**
     * A BossBarHudMixin hívja minden boss bar packet után. A map bejárása
     * olcsó referencia-összehasonlítás; a drága {@code getString().toLowerCase()}
     * és {@code detectBoss()} csak akkor fut, ha egy név Text példánya változott.
     */
    public static void onBossBarPacket(BossBarHud hud) {
        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null || !config.autoBossDetection) {
            lastBossNames.clear();
            return;
        }

        Map<UUID, BossBar> bossBars = getBossBars(hud);
        if (bossBars == null) return;
        if (bossBars.isEmpty()) {
            lastBossNames.clear();
            return;
        }

        BossBar changed = null;
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            BossBar bar = entry.getValue();
            if (bar == null) continue;

            Text name = bar.getName();
            Text previous = lastBossNames.get(entry.getKey());
            // A Text immutable: azonos referencia = azonos tartalom. Progress
            // frissítésnél ugyanaz a példány jön, tehát nincs string-munka.
            if (previous != name) {
                lastBossNames.put(entry.getKey(), name);
                changed = bar;
            }
        }

        // Eltávolított boss barok ne maradjanak a cache-ben.
        if (lastBossNames.size() != bossBars.size()) {
            lastBossNames.keySet().retainAll(bossBars.keySet());
        }

        if (changed != null) {
            onBossBarChanged(changed);
        }
    }

    /** A BossBarHudMixin clear() hookja: world váltáskor a név-cache törlése. */
    public static void onBossBarsCleared() {
        lastBossNames.clear();
    }

    /**
     * Csak akkor hívódik, amikor egy boss bar megjelenik vagy a neve változik.
     * A régi tickenkénti reflection + teljes string-feldolgozás helyett ez a
     * ritka esemény végzi a profilmód-váltást.
     */
    public static void onBossBarChanged(BossBar bossBar) {
        if (bossBar == null) return;

        StorageConfig config = ActionbarStorageClient.getConfig();
        if (config == null || !config.autoBossDetection) return;

        String cleanBossName = bossBar.getName().getString().toLowerCase();
        String targetBoss = detectBoss(cleanBossName);
        if (targetBoss == null || targetBoss.equals(config.selectedBossNotification)) return;

        config.selectedBossNotification = targetBoss;
        currentDamage = 0;
        wasDamageReady = false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.setTitleTicks(5, 20, 5);
            client.inGameHud.setTitle(Text.literal("§d§lMÓD VÁLTVA"));
            client.inGameHud.setSubtitle(Text.literal("§eProfil automatikusan átállítva: §b" + targetBoss));
        }
        StorageConfigSaver.requestSave(config);
    }

    /**
     * Accessor first, reflection csak fallbackként. A state cache megakadályozza,
     * hogy minden boss bar packetnél try/catch vagy reflection fusson.
     */
    @SuppressWarnings("unchecked")
    private static Map<UUID, BossBar> getBossBars(BossBarHud hud) {
        if (hud == null) return null;

        if (bossBarsAccessorState != 2) {
            try {
                Map<UUID, BossBar> bars = ((BossBarHudAccessor) hud).mvp$getBossBars();
                bossBarsAccessorState = 1;
                return bars;
            } catch (Throwable ignored) {
                // Accessor mixin hiányzik / más verzió: marad a régi reflection.
                bossBarsAccessorState = 2;
            }
        }

        try {
            if (cachedBossBarsField == null) {
                for (Field field : BossBarHud.class.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        cachedBossBarsField = field;
                        break;
                    }
                }
            }
            if (cachedBossBarsField == null) return null;
            return (Map<UUID, BossBar>) cachedBossBarsField.get(hud);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String detectBoss(String cleanBossName) {
        if (cleanBossName.contains("endermágus")) return "Endermágus";
        if (cleanBossName.contains("trex") || cleanBossName.contains("t-rex")) return "Trex";
        if (cleanBossName.contains("warden")) return "Warden";
        if (cleanBossName.contains("radio") && (cleanBossName.contains("pók"))) return "RadioPók";
        if (cleanBossName.contains("icedragon")) return "Jégsárkány";
        if (cleanBossName.contains("kraken")) return "Kraken";
        if (cleanBossName.contains("nyálkakirály")) return "Nyálkakirály";
        return null;
    }

    public static void setCurrentDamage(int damage) {
        currentDamage = damage;
        lastDamageUpdateTime = System.currentTimeMillis();
    }
}