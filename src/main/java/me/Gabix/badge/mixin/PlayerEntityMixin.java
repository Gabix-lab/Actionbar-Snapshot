package me.Gabix.badge.mixin;

import me.Gabix.badge.ModPlayerTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void addIconToDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (ModPlayerTracker.usesMod(player.getUuid(), player.getGameProfile().getName())) {
            Text originalName = cir.getReturnValue();
            if (originalName != null && !originalName.getString().contains("\ue001")) {
                MutableText nameWithIcon = Text.literal("\ue001 ").append(originalName);
                cir.setReturnValue(nameWithIcon);
            }
        }
    }
}