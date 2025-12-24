package niv.heater.registry;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import niv.burning.api.BurningPropagator;
import niv.burning.api.BurningStorage;
import niv.burning.api.base.DelegatingBurningStorage;

public class HeaterRegistrar {
    private HeaterRegistrar() {
    }

    static {
        BurningStorage.SIDED.registerForBlockEntity(
                (entity, side) -> entity.getBurningStorage(),
                HeaterBlockEntityTypes.HEATER);

        BurningPropagator.SIDED.registerForBlocks(
                (level, pos, state, entity, side) -> (BurningPropagator) state.getBlock(),
                ImmutableList.builder()
                        .addAll(HeaterBlocks.HEATER.asList())
                        .addAll(HeaterBlocks.HEAT_PIPE.asList())
                        .addAll(HeaterBlocks.THERMOSTAT.asList())
                        .build().toArray(Block[]::new));

        BurningStorage.SIDED.registerForBlocks(
                (level, pos, state, entity, side) -> {
                    var facing = state.getValueOrElse(BlockStateProperties.FACING, null);
                    if (facing == null || !level.hasNeighborSignal(pos))
                        return null;

                    var heater = level.getBlockEntity(pos.relative(facing), HeaterBlockEntityTypes.HEATER).orElse(null);
                    if (heater == null)
                        return null;

                    return new DelegatingBurningStorage(heater.getBurningStorage(), null);
                },
                HeaterBlocks.THERMOSTAT.asList().toArray(Block[]::new));
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
