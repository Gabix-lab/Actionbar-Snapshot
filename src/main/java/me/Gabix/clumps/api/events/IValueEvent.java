package me.Gabix.clumps.api.events;

import net.minecraft.entity.player.PlayerEntity;

public interface IValueEvent {
    void setValue(int value);
    int getValue();
    PlayerEntity getPlayer();
}