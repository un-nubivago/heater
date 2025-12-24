package niv.heater.registry;

import static net.minecraft.world.level.block.Blocks.COPPER_BLOCK;
import static net.minecraft.world.level.block.Blocks.FURNACE;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy;
import static niv.heater.Heater.MOD_ID;

import java.util.function.Function;

import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopperBlocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import niv.heater.block.HeatPipeBlock;
import niv.heater.block.HeaterBlock;
import niv.heater.block.ThermostatBlock;
import niv.heater.block.WeatheringHeatPipeBlock;
import niv.heater.block.WeatheringHeaterBlock;
import niv.heater.block.WeatheringThermostatBlock;

public class HeaterBlocks {
    private HeaterBlocks() {
    }

    public static final WeatheringCopperBlocks HEATER;
    public static final WeatheringCopperBlocks HEAT_PIPE;
    public static final WeatheringCopperBlocks THERMOSTAT;

    static {
        HEATER = WeatheringCopperBlocks.create(
                "heater", HeaterBlocks::register,
                HeaterBlock::new, WeatheringHeaterBlock::new,
                weathering -> ofFullCopy(FURNACE));
        OxidizableBlocksRegistry.registerCopperBlockSet(HEATER);

        HEAT_PIPE = WeatheringCopperBlocks.create(
                "heat_pipe", HeaterBlocks::register,
                HeatPipeBlock::new, WeatheringHeatPipeBlock::new,
                weathering -> ofFullCopy(COPPER_BLOCK));
        OxidizableBlocksRegistry.registerCopperBlockSet(HEAT_PIPE);

        THERMOSTAT = WeatheringCopperBlocks.create(
                "thermostat", HeaterBlocks::register,
                ThermostatBlock::new, WeatheringThermostatBlock::new,
                weathering -> ofFullCopy(COPPER_BLOCK));
        OxidizableBlocksRegistry.registerCopperBlockSet(THERMOSTAT);
    }

    private static final <T extends Block> T register(String name,
            Function<Properties, T> constructor,
            Properties properties) {
        var blockKey = ResourceKey.create(Registries.BLOCK, ResourceLocation.tryBuild(MOD_ID, name));
        var block = constructor.apply(properties.setId(blockKey));
        block = Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        var itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.tryBuild(MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));

        return block;
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
