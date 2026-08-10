package me.Gabix.mixin;

import me.Gabix.tracker.ModPlayerTracker;
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

        if (ModPlayerTracker.usesMod(player.getUuid())) {
            Text originalName = cir.getReturnValue();

            MutableText nameWithIcon = Text.literal("\uE001 ").append(originalName);
            cir.setReturnValue(nameWithIcon);
        }
    }
}