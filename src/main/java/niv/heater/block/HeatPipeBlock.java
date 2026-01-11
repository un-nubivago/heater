package niv.heater.block;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import niv.burning.api.BurningStorage;
import niv.burning.api.FuelVariant;
import niv.heater.registry.HeaterBlocks;

public class HeatPipeBlock extends PipeBlock implements SimpleWaterloggedBlock {

    private static final ThreadLocal<Set<Pair<Level, BlockPos>>> EXPLORED_SET = ThreadLocal.withInitial(HashSet::new);

    public HeatPipeBlock(Properties settings) {
        super(.1875F, settings);
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

    public InsertionOnlyStorage<FuelVariant> getStatelessStorage(Level level, BlockPos pos, BlockState state) {
        return (resource, maxAmount, transaction) -> {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            if (tryAdd(level, pos)) {
                transaction.addOuterCloseCallback(result -> doRemove(level, pos));
            } else {
                return 0L;
            }

            var inserted = this.getAge().ordinal() + 1;

            if (inserted >= maxAmount)
                return maxAmount;

            var dirs = getConnectedDirection(state, level.random);

            for (int i = 0; i < dirs.length && inserted < maxAmount; i++) {
                var dir = dirs[i];
                var storage = BurningStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (storage != null && storage.supportsInsertion())
                    inserted += storage.insert(resource, maxAmount - inserted, transaction);
            }

            return inserted;
        };
    }

    private boolean tryAdd(Level level, BlockPos pos) {
        return EXPLORED_SET.get().add(Pair.of(level, pos));
    }

    private void doRemove(Level level, BlockPos pos) {
        EXPLORED_SET.get().remove(Pair.of(level, pos));
        if (EXPLORED_SET.get().isEmpty())
            EXPLORED_SET.remove();
    }

    private Direction[] getConnectedDirection(
            BlockState state, @Nullable RandomSource random) {
        var result = Direction.stream()
                .filter(value -> state.getOptionalValue(PROPERTY_BY_DIRECTION.get(value)).orElse(false).booleanValue())
                .toArray(Direction[]::new);

        if (result.length >= 1 && random != null) {
            for (var i = result.length - 1; i > 0; i--) {
                var j = random.nextInt(i + 1);
                var dir = result[i];
                result[i] = result[j];
                result[j] = dir;
            }
        }

        return result;
    }

    // PipeBlock

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
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
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (level instanceof Level world) {
            return state.trySetValue(PROPERTY_BY_DIRECTION.get(direction), canConnect(world, pos, direction));
        } else {
            return state;
        }
    }

    private boolean canConnect(Level level, BlockPos pos, Direction direction) {
        return BurningStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite()) != null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }
}
