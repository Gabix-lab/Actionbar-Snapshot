package me.Gabix.boss.mixin;

import me.Gabix.boss.BossMonitor;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Boss bar esemény hook.
 *
 * <p>1.19.4-ben nincs {@code setBossBar}; a szerver minden boss bar
 * változást {@link BossBarHud#handlePacket(BossBarS2CPacket)}-en keresztül
 * küld. A TAIL injection után a map már friss, így a BossMonitor a
 * név-referenciák alapján eldöntheti, hogy tényleg történt-e érdemi változás.</p>
 */
@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Inject(method = "handlePacket", at = @At("TAIL"), require = 0)
    private void mvp$onBossBarPacket(BossBarS2CPacket packet, CallbackInfo ci) {
        BossMonitor.onBossBarPacket((BossBarHud) (Object) this);
    }

    /** World váltáskor a HUD map-je ürül; a név-cache is törlendő. */
    @Inject(method = "clear", at = @At("TAIL"), require = 0)
    private void mvp$onClear(CallbackInfo ci) {
        BossMonitor.onBossBarsCleared();
    }
}
