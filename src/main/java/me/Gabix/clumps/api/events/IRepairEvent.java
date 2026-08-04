package me.Gabix.clumps.api.events;

import net.minecraft.entity.player.PlayerEntity;

public interface IRepairEvent {
    void setValue(int value);
    int getValue();
    PlayerEntity getPlayer();
}