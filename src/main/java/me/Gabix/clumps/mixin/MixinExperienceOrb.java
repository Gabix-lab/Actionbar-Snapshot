package me.Gabix.clumps.mixin;

import me.Gabix.clumps.ClumpsCommon;
import me.Gabix.clumps.api.events.IRepairEvent;
import me.Gabix.clumps.api.events.IValueEvent;
import me.Gabix.clumps.helper.IClumpedOrb;
import me.Gabix.clumps.platform.FabricEventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = ExperienceOrbEntity.class, priority = 1001)
public abstract class MixinExperienceOrb extends Entity implements IClumpedOrb {
    @Shadow private int amount;
    @Shadow private int pickingCount;
    @Shadow private int health;

    @Unique public Map<Integer, Integer> clumps$clumpedMap;

    @Shadow protected abstract int repairPlayerGears(PlayerEntity player, int amount);

    public MixinExperienceOrb(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "isMergeable(Lnet/minecraft/entity/ExperienceOrbEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void canMerge(ExperienceOrbEntity experienceOrb, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(experienceOrb.isAlive() && !this.equals(experienceOrb));
    }

    @Inject(method = "isMergeable(Lnet/minecraft/entity/ExperienceOrbEntity;II)Z", at = @At("HEAD"), cancellable = true)
    private static void canMerge(ExperienceOrbEntity experienceOrb, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(experienceOrb.isAlive());
    }

    @Inject(method = "onPlayerCollision(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    public void onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        if (!this.world.isClient) {
            if (ClumpsCommon.pickupXPEvent.test(player, (ExperienceOrbEntity) (Object) this)) {
                return;
            }

            player.experiencePickUpDelay = 0;
            player.sendPickup(this, 1);
            if (this.pickingCount != 0 || this.clumps$resolve()) {
                int toGive = 0;
                Map<Integer, Integer> map = this.clumps$getClumpedMap();
                for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                    int value = entry.getKey();
                    int amount = entry.getValue();
                    IValueEvent valueEvent = FabricEventHandler.fireValueEvent(player, value);
                    int actualValue = valueEvent.getValue();

                    for (int i = 0; i < amount; i++) {
                        IRepairEvent repairEvent = FabricEventHandler.fireRepairEvent(player, actualValue);
                        int leftOver = repairEvent.getValue();
                        if (leftOver == actualValue) {
                            leftOver = this.repairPlayerGears(player, actualValue);
                        }
                        if (leftOver > 0) {
                            toGive += leftOver;
                        }
                    }
                }
                if (toGive > 0) {
                    player.addExperience(toGive);
                }
            }
            this.discard();
            ci.cancel();
        }
    }

    @Inject(
            method = "merge(Lnet/minecraft/entity/ExperienceOrbEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ExperienceOrbEntity;discard()V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    public void merge(ExperienceOrbEntity secondaryOrb, CallbackInfo ci) {
        Map<Integer, Integer> otherMap = ((IClumpedOrb) secondaryOrb).clumps$getClumpedMap();
        int sum = 0;
        for (int v : this.clumps$getClumpedMap().values()) sum += v;
        this.amount = sum;
        this.health = Math.min(this.health, ((ExperienceOrbAccess) secondaryOrb).clumps$getHealth());

        Map<Integer, Integer> merged = new HashMap<>(this.clumps$getClumpedMap());
        for (Map.Entry<Integer, Integer> entry : otherMap.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        this.clumps$setClumpedMap(merged);

        secondaryOrb.discard();
        ci.cancel();
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeCustomDataToNbt(NbtCompound compoundTag, CallbackInfo ci) {
        NbtCompound map = new NbtCompound();
        for (Map.Entry<Integer, Integer> entry : this.clumps$getClumpedMap().entrySet()) {
            map.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }
        compoundTag.put("clumpedMap", map);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readCustomDataFromNbt(NbtCompound compoundTag, CallbackInfo ci) {
        Map<Integer, Integer> map = new HashMap<>();
        if (compoundTag.contains("clumpedMap")) {
            NbtCompound clumpedMap = compoundTag.getCompound("clumpedMap");
            for (String s : clumpedMap.getKeys()) {
                map.put(Integer.parseInt(s), clumpedMap.getInt(s));
            }
        } else {
            map.put(this.pickingCount, this.amount);
        }
        this.clumps$setClumpedMap(map);
    }

    @Override
    public Map<Integer, Integer> clumps$getClumpedMap() {
        if (this.clumps$clumpedMap == null) {
            this.clumps$clumpedMap = new HashMap<>(1);
            this.clumps$clumpedMap.put(this.pickingCount, 1);
        }
        return this.clumps$clumpedMap;
    }

    @Override
    public void clumps$setClumpedMap(Map<Integer, Integer> map) {
        this.clumps$clumpedMap = map;
        this.clumps$resolve();
    }

    @Override
    public boolean clumps$resolve() {
        int sum = 0;
        for (Map.Entry<Integer, Integer> entry : this.clumps$getClumpedMap().entrySet()) {
            sum += entry.getKey() * entry.getValue();
        }
        this.pickingCount = sum > 0 ? sum : 1;
        return this.pickingCount > 0;
    }
}