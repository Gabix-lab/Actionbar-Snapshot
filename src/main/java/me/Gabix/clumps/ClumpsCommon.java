package me.Gabix.clumps;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiPredicate;

public class ClumpsCommon {
    public static final Logger LOG = LogManager.getLogger("clumps");
    public static BiPredicate<PlayerEntity, ExperienceOrbEntity> pickupXPEvent = (player, experienceOrb) -> false;
}