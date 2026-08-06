package me.Gabix.clumps.mixin;

import net.minecraft.entity.ExperienceOrbEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExperienceOrbEntity.class)
public interface ExperienceOrbAccess {
    @Accessor("health")
    int clumps$getHealth();

    @Accessor("health")
    void clumps$setHealth(int health);

    @Accessor("amount")
    void clumps$setAmount(int amount);
}