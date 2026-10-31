package me.Gabix.core;

import me.Gabix.boss.BossMonitor;
import me.Gabix.boss.mixin.BossBarHudAccessor;
import me.Gabix.hud.HudMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.boss.BossBar;

import java.util.Map;
import java.util.UUID;

/** Temporary hot-path smoke test; removed after runtime validation. */
public final class HotPathRuntimeSelfTest {
    private static int titleTicks;
    private static int worldTicks;

    private HotPathRuntimeSelfTest() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen instanceof TitleScreen) {
                titleTicks++;
                if (titleTicks == 40) {
                    System.out.println("[MVP-TEST] Joining world 'Test'");
                    client.createIntegratedServerLoader().start(new TitleScreen(), "Test");
                }
            }

            if (client.world != null && client.player != null) {
                worldTicks++;
                if (worldTicks == 40) {
                    Map<UUID, BossBar> bars = ((BossBarHudAccessor) client.inGameHud.getBossBarHud())
                            .mvp$getBossBars();
                    int bossBars = bars == null ? -1 : bars.size();
                    BossMonitor.onBossBarPacket(client.inGameHud.getBossBarHud());
                    StorageConfigSaver.saveNow(ActionbarStorageClient.getConfig());
                    System.out.println("[MVP-TEST] World loaded, bossBars=" + bossBars
                            + ", arrows=" + HudMod.getArrowCounter().getAps());
                }
                if (worldTicks == 260) {
                    System.out.println("[MVP-TEST] Stopping client");
                    client.scheduleStop();
                }
            }
        });
    }
}
