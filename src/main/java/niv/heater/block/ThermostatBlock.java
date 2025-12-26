package niv.heater.block;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import niv.burning.api.BurningPropagator;
import niv.heater.registry.HeaterBlocks;

public class ThermostatBlock extends DirectionalBlock implements BurningPropagator {

    public ThermostatBlock(Properties settings) {
        super(settings);
    }

    public WeatherState getAge() {
        return ((WeatheringCopper) HeaterBlocks.THERMOSTAT.waxedMapping().inverse()
                .getOrDefault(this, HeaterBlocks.THERMOSTAT.unaffected())).getAge();
    }

    // DirectionalBlock

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    // BurningPropagator

    @Override
    public Set<Direction> evalPropagationTargets(Level level, BlockPos pos) {
        if (!level.hasNeighborSignal(pos))
            return EnumSet.noneOf(Direction.class);

        var direction = level.getBlockState(pos).getOptionalValue(FACING).orElse(null);
        if (direction == null)
            return EnumSet.noneOf(Direction.class);

        return EnumSet.of(direction);
    }
}
