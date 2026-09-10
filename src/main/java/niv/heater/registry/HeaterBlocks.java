package niv.heater.registry;

import static net.minecraft.world.level.block.Blocks.FURNACE;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy;

import org.jspecify.annotations.NullMarked;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import niv.heater.block.HeatPipeBlock;
import niv.heater.block.HeaterBlock;
import niv.heater.block.ThermostatBlock;
import niv.heater.block.WeatheringHeatPipeBlock;
import niv.heater.block.WeatheringHeaterBlock;
import niv.heater.block.WeatheringThermostatBlock;

@NullMarked
@SuppressWarnings("null")
public class HeaterBlocks {
    private HeaterBlocks() {
    }

    public static final WeatheringCopperCollection<Block> HEATER;
    public static final WeatheringCopperCollection<Block> HEAT_PIPE;
    public static final WeatheringCopperCollection<Block> THERMOSTAT;

    static {
        HEATER = WeatheringCopperCollection.registerBlocks(HeaterBlockItemIds.HEATER,
                (id, factory, properties) -> Blocks.register(id.block(), factory, properties),
                (state, properties) -> new HeaterBlock(properties), WeatheringHeaterBlock::new,
                any -> ofFullCopy(FURNACE));

        WeatheringCopperCollection.registerItems(HeaterBlockItemIds.HEATER, HEATER,
                (id, block) -> Registry.register(BuiltInRegistries.ITEM, id.item(),
                        new BlockItem(block, new Item.Properties().setId(id.item()))));

        HEAT_PIPE = WeatheringCopperCollection.registerBlocks(HeaterBlockItemIds.HEAT_PIPE,
                (id, factory, properties) -> Blocks.register(id.block(), factory, properties),
                (state, properties) -> new HeatPipeBlock(properties), WeatheringHeatPipeBlock::new,
                HeaterBlocks::getCopperProperties);

        WeatheringCopperCollection.registerItems(HeaterBlockItemIds.HEAT_PIPE, HEAT_PIPE,
                (id, block) -> Registry.register(BuiltInRegistries.ITEM, id.item(),
                        new BlockItem(block, new Item.Properties().setId(id.item()))));

        THERMOSTAT = WeatheringCopperCollection.registerBlocks(HeaterBlockItemIds.THERMOSTAT,
                (id, factory, properties) -> Blocks.register(id.block(), factory, properties),
                (state, properties) -> new ThermostatBlock(properties), WeatheringThermostatBlock::new,
                HeaterBlocks::getCopperProperties);

        WeatheringCopperCollection.registerItems(HeaterBlockItemIds.THERMOSTAT, THERMOSTAT,
                (id, block) -> Registry.register(BuiltInRegistries.ITEM, id.item(),
                        new BlockItem(block, new Item.Properties().setId(id.item()))));
    }

    private static BlockBehaviour.Properties getCopperProperties(WeatherState state) {
        return BlockBehaviour.Properties.of()
                .mapColor(switch (state) {
                    case UNAFFECTED -> MapColor.COLOR_ORANGE;
                    case EXPOSED -> MapColor.TERRACOTTA_LIGHT_GRAY;
                    case WEATHERED -> MapColor.WARPED_STEM;
                    case OXIDIZED -> MapColor.WARPED_NYLIUM;
                })
                .requiresCorrectToolForDrops()
                .strength(3.0F, 6.0F)
                .instrument(switch (state) {
                    case UNAFFECTED -> NoteBlockInstrument.TRUMPET;
                    case EXPOSED -> NoteBlockInstrument.TRUMPET_EXPOSED;
                    case WEATHERED -> NoteBlockInstrument.TRUMPET_WEATHERED;
                    case OXIDIZED -> NoteBlockInstrument.TRUMPET_OXIDIZED;
                })
                .sound(SoundType.COPPER);
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
