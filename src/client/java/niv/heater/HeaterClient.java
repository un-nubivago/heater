package niv.heater;

import org.jspecify.annotations.NullMarked;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import niv.heater.registry.HeaterMenus;

@NullMarked
public class HeaterClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(HeaterMenus.HEATER, HeaterScreen::new);
        MenuScreens.register(HeaterMenus.THERMOSTAT, ThermostatScreen::new);
    }

}
