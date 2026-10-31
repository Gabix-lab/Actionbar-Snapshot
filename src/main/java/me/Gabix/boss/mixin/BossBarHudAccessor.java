package me.Gabix.boss.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * Reflection nélküli hozzáférés a BossBarHud belső bossBars map-jéhez.
 *
 * <p>1.19.4-ben a mező típusa {@code Map<UUID, ClientBossBar>}; a
 * {@code ClientBossBar} a {@code BossBar} leszármazottja, ezért a generikus
 * return típus biztonságosan használható.</p>
 */
@Mixin(BossBarHud.class)
public interface BossBarHudAccessor {

    @Accessor("bossBars")
    Map<UUID, BossBar> mvp$getBossBars();
}
