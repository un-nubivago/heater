package niv.heater.block;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import niv.burning.api.BurningPropagator;
import niv.burning.api.BurningStorage;
import niv.heater.registry.HeaterBlocks;

public class HeatPipeBlock extends PipeBlock implements BurningPropagator, SimpleWaterloggedBlock {

    @SuppressWarnings("java:S1845")
    public static final MapCodec<HeatPipeBlock> CODEC = simpleCodec(HeatPipeBlock::new);

    public HeatPipeBlock(Properties settings) {
        super(6.0F, settings);
        this.registerDefaultState(stateDefinition.any()
                .setValue(DOWN, false)
                .setValue(UP, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false)
                .setValue(WATERLOGGED, false));
    }

    public WeatherState getAge() {
        return ((WeatheringCopper) HeaterBlocks.HEAT_PIPE.waxedMapping().inverse()
                .getOrDefault(this, HeaterBlocks.HEAT_PIPE.unaffected())).getAge();
    }

    // PipeBlock

    @Override
    public MapCodec<? extends HeatPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !state.getValue(WATERLOGGED).booleanValue();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return state.getFluidState();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        return this.defaultBlockState()
                .trySetValue(DOWN, canConnect(level, pos, Direction.DOWN))
                .trySetValue(UP, canConnect(level, pos, Direction.UP))
                .trySetValue(NORTH, canConnect(level, pos, Direction.NORTH))
                .trySetValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
                .trySetValue(WEST, canConnect(level, pos, Direction.WEST))
                .trySetValue(EAST, canConnect(level, pos, Direction.EAST))
                .trySetValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(
            BlockState state, LevelReader level, ScheduledTickAccess scheduler, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            scheduler.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (level instanceof Level world) {
            return state.trySetValue(PROPERTY_BY_DIRECTION.get(direction), canConnect(world, pos, direction));
        } else {
            return state;
        }
    }

    private boolean canConnect(Level level, BlockPos pos, Direction direction) {
        return BurningPropagator.SIDED.find(level, pos.relative(direction), direction.getOpposite()) != null
                || BurningStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite()) != null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }

    // BurningPropagator

    @Override
    public Set<Direction> evalPropagationTargets(Level level, BlockPos pos) {
        var ordinal = this.getAge().ordinal();
        var power = ordinal == 0 ? 0 : (1 << ordinal - 1) - 1; // 0 : 0 1 3
        return Direction.stream()
                .filter(direction -> level.getBlockState(pos)
                        .getValueOrElse(PROPERTY_BY_DIRECTION.get(direction), Boolean.FALSE))
                .filter(direction -> ordinal == 0 || level.random.nextInt(64) <= power)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Direction.class)));
    }
}
