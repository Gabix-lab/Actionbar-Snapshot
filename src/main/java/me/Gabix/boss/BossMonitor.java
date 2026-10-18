package me.Gabix.boss;

import me.Gabix.core.ActionbarStorageClient;
import me.Gabix.core.StorageConfig;
import me.Gabix.hud.HudMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.Map;

public class BossMonitor {
    private static boolean continuousTitle = true;
    private static int currentDamage = 0;
    private static boolean wasDamageReady = false;
    private static long lastDamageUpdateTime = 0;
    private static Field cachedBossBarsField = null;

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        try {
            if (client.inGameHud != null) {
                BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
                if (cachedBossBarsField == null) {
                    for (Field field : BossBarHud.class.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            cachedBossBarsField = field;
                            break;
                        }
                    }
                }

                if (cachedBossBarsField != null) {
                    Map<?, ?> bossBars = (Map<?, ?>) cachedBossBarsField.get(bossBarHud);
                    if (bossBars != null && !bossBars.isEmpty()) {
                        for (Object value : bossBars.values()) {
                            if (value instanceof BossBar bossBar) {
                                String cleanBossName = bossBar.getName().getString().toLowerCase();
                                StorageConfig config = ActionbarStorageClient.getConfig();
                                if (config != null && config.autoBossDetection) {
                                    String targetBoss = detectBoss(cleanBossName);
                                    if (targetBoss != null && !targetBoss.equals(config.selectedBossNotification)) {
                                        config.selectedBossNotification = targetBoss;
                                        currentDamage = 0;
                                        wasDamageReady = false;
                                        client.inGameHud.setTitleTicks(5, 20, 5);
                                        client.inGameHud.setTitle(Text.literal("§d§lMÓD VÁLTVA"));
                                        client.inGameHud.setSubtitle(Text.literal("§eProfil automatikusan átállítva: §b" + targetBoss));
                                        config.save();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

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