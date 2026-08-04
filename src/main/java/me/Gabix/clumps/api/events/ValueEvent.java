package me.Gabix.clumps.api.events;

import net.minecraft.entity.player.PlayerEntity;

public class ValueEvent implements IValueEvent {
    private final PlayerEntity player;
    private int value;

    public ValueEvent(PlayerEntity player, int value) {
        this.player = player;
        this.value = value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public PlayerEntity getPlayer() {
        return this.player;
    }
}