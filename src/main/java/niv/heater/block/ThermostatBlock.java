package niv.heater.block;

import static niv.heater.registry.HeaterBlockEntityTypes.HEATER;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
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
import niv.burning.api.BurningStorage;
import niv.burning.api.FuelVariant;
import niv.heater.registry.HeaterBlocks;

public class ThermostatBlock extends DirectionalBlock {

    private static final ThreadLocal<Set<Pair<Level, BlockPos>>> EXPLORED_SET = ThreadLocal.withInitial(HashSet::new);

    public ThermostatBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public WeatherState getAge() {
        return ((WeatheringCopper) HeaterBlocks.THERMOSTAT.waxedMapping().inverse()
                .getOrDefault(this, HeaterBlocks.THERMOSTAT.unaffected())).getAge();
    }

    public InsertionOnlyStorage<FuelVariant> getStatelessStorage(
            Level level, BlockPos pos, BlockState state, @Nullable Direction direction) {
        return (resource, maxAmount, transaction) -> {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            var facing = state.getOptionalValue(FACING).orElseThrow(IllegalStateException::new);
            if (facing.equals(direction))
                return 0L;

            if (tryAdd(level, pos)) {
                transaction.addOuterCloseCallback(result -> doRemove(level, pos));
            } else {
                return 0L;
            }

            var inserted = this.getAge().ordinal();
            if (inserted >= maxAmount)
                return maxAmount;

            if (level.hasNeighborSignal(pos) && facing != null) {
                var rel = pos.relative(facing);
                var storage = BurningStorage.SIDED.find(level, rel, facing.getOpposite());
                if (storage != null
                        && (storage.supportsInsertion() || level.getBlockEntity(rel, HEATER).isPresent()))
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
}
