package me.Gabix;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionbarStorage implements ModInitializer {
    public static final String MOD_ID = "actionbarstorage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ActionbarStorage mod elindult!");
    }
}