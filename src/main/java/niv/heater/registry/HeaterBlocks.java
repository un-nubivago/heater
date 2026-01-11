package niv.heater.registry;

import static net.minecraft.world.level.block.Blocks.COPPER_BLOCK;
import static net.minecraft.world.level.block.Blocks.FURNACE;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy;
import static niv.heater.Heater.MOD_ID;

import java.util.function.BiFunction;
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
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import niv.heater.block.HeatPipeBlock;
import niv.heater.block.HeaterBlock;
import niv.heater.block.ThermostatBlock;
import niv.heater.block.WeatheringHeatPipeBlock;
import niv.heater.block.WeatheringHeaterBlock;
import niv.heater.block.WeatheringThermostatBlock;
import niv.heater.util.WeatheringCopperBlocks;

public class HeaterBlocks {
    private HeaterBlocks() {
    }

    public static final WeatheringCopperBlocks HEATER;
    public static final WeatheringCopperBlocks HEAT_PIPE;
    public static final WeatheringCopperBlocks THERMOSTAT;

    static {
        HEATER = register("heater",
                HeaterBlock::new, WeatheringHeaterBlock::new, any -> copy(FURNACE));

        HEAT_PIPE = register("heat_pipe",
                HeatPipeBlock::new, WeatheringHeatPipeBlock::new, any -> copy(COPPER_BLOCK));

        THERMOSTAT = register("thermostat",
                ThermostatBlock::new, WeatheringThermostatBlock::new, any -> copy(COPPER_BLOCK));
    }

    private static <A extends Block, B extends Block & WeatheringCopper> WeatheringCopperBlocks register(
            String name,
            Function<Properties, A> waxedConstructor,
            BiFunction<WeatherState, Properties, B> weatheredConstructor,
            Function<WeatherState, Properties> propertiesBuilder) {
        var result = WeatheringCopperBlocks.create(name, HeaterBlocks::register,
                waxedConstructor, weatheredConstructor, propertiesBuilder);
        result.weatheringMapping().forEach(OxidizableBlocksRegistry::registerOxidizableBlockPair);
        result.waxedMapping().forEach(OxidizableBlocksRegistry::registerWaxableBlockPair);
        return result;
    }

    private static final <T extends Block> T register(
            String name, Function<Properties, T> constructor, Properties properties) {

        var blockKey = ResourceKey.create(Registries.BLOCK, ResourceLocation.tryBuild(MOD_ID, name));
        var block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
                constructor.apply(properties));

        var itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.tryBuild(MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties()));

        return block;
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
