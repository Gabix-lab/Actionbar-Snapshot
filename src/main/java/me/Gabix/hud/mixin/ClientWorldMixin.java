package me.Gabix.hud.mixin;

import me.Gabix.hud.HudMod;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Új entitás spawn hook.
 *
 * <p>A TAIL-en az entity már a worldben van, és a projectile owner-e is
 * beállítódott az {@code onSpawnPacket} során. Csak a nyilakat adjuk tovább az
 * ArrowCounternek, így a tick már nem iterálja a teljes entitáslistát.</p>
 */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(
            method = "addEntity(ILnet/minecraft/entity/Entity;)V",
            at = @At("TAIL"),
            require = 0
    )
    private void mvp$onEntityAdded(int id, Entity entity, CallbackInfo ci) {
        if (entity instanceof ArrowEntity arrow) {
            HudMod.getArrowCounter().onArrowSpawned(arrow);
        }
    }
}
