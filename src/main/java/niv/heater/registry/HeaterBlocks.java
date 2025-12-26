package niv.heater.registry;

import static net.minecraft.world.level.block.Blocks.COPPER_BLOCK;
import static net.minecraft.world.level.block.Blocks.FURNACE;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy;
import static niv.heater.Heater.MOD_ID;

import java.util.function.Function;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import niv.heater.block.HeatPipeBlock;
import niv.heater.block.HeaterBlock;
import niv.heater.block.ThermostatBlock;
import niv.heater.block.WeatheringHeatPipeBlock;
import niv.heater.block.WeatheringHeaterBlock;
import niv.heater.block.WeatheringThermostatBlock;
import niv.heater.util.WeatheringBlocks;

public class HeaterBlocks {
    private HeaterBlocks() {
    }

    public static final WeatheringBlocks HEATER;
    public static final WeatheringBlocks HEAT_PIPE;
    public static final WeatheringBlocks THERMOSTAT;

    static {
        HEATER = WeatheringBlocks.create(
                "heater", HeaterBlocks::register,
                HeaterBlock::new, WeatheringHeaterBlock::new,
                weathering -> copy(FURNACE)).register();

        HEAT_PIPE = WeatheringBlocks.create(
                "heat_pipe", HeaterBlocks::register,
                HeatPipeBlock::new, WeatheringHeatPipeBlock::new,
                weathering -> copy(COPPER_BLOCK)).register();

        THERMOSTAT = WeatheringBlocks.create(
                "thermostat", HeaterBlocks::register,
                ThermostatBlock::new, WeatheringThermostatBlock::new,
                weathering -> copy(COPPER_BLOCK)).register();
    }

    private static final <T extends Block> T register(
            String name, Function<Properties, T> constructor, Properties properties) {

        var blockKey = ResourceKey.create(Registries.BLOCK, ResourceLocation.tryBuild(MOD_ID, name));
        var block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
                constructor.apply(properties));

        var itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.tryBuild(MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new FabricItemSettings()));

        return block;
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
