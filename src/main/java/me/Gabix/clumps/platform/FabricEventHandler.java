package me.Gabix.clumps.platform;

import me.Gabix.clumps.api.events.IValueEvent;
import me.Gabix.clumps.api.events.IRepairEvent;
import me.Gabix.clumps.api.events.ValueEvent;
import me.Gabix.clumps.api.events.RepairEvent;
import net.minecraft.entity.player.PlayerEntity;

public class FabricEventHandler {
    public static IValueEvent fireValueEvent(PlayerEntity player, int value) {
        return new ValueEvent(player, value);
    }

    public static IRepairEvent fireRepairEvent(PlayerEntity player, int value) {
        return new RepairEvent(player, value);
    }
}