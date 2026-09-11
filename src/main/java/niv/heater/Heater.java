package niv.heater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import niv.heater.registry.HeaterBlockEntityTypes;
import niv.heater.registry.HeaterBlockItemIds;
import niv.heater.registry.HeaterBlocks;
import niv.heater.registry.HeaterMenus;
import niv.heater.registry.HeaterRegistrar;
import niv.heater.registry.HeaterTabs;

@SuppressWarnings("java:S2440")
public class Heater implements ModInitializer {

    public static final String MOD_ID = "heater";

    public static final String MOD_NAME = "Heater";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    @SuppressWarnings("java:S1192")
    public void onInitialize() {
        HeaterBlockItemIds.initialize();
        HeaterBlocks.initialize();
        HeaterBlockEntityTypes.initialize();
        HeaterRegistrar.initialize();
        HeaterMenus.initialize();
        HeaterTabs.initialize();

        LOGGER.info("({}) Ready", MOD_NAME);
    }
}
