package niv.heater.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import niv.heater.api.Connector;
import niv.heater.block.entity.HeaterBlockEntity;

public class ThermostatBlock extends DirectionalBlock implements Connector, WeatheringCopper {

    @SuppressWarnings("java:S1845")
    public static final MapCodec<ThermostatBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WeatherState.CODEC.fieldOf("weathering_state").forGetter(ThermostatBlock::getAge),
            Properties.CODEC.fieldOf("properties").forGetter(BlockBehaviour::properties))
            .apply(instance, ThermostatBlock::new));

    private final WeatherState weatherState;

    public ThermostatBlock(WeatherState weatherState, Properties settings) {
        super(settings);
        this.weatherState = weatherState;
    }

    @Override
    public MapCodec<? extends ThermostatBlock> codec() {
        return CODEC;
    }

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

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block sourceBlock, Orientation orientation, boolean notify) {
        if (level.isClientSide()) {
            return;
        }
        HeaterBlockEntity.updateConnectedHeaters(level, pos, level.getBlockState(pos));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        HeaterBlockEntity.updateConnectedHeaters(level, pos, state);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        HeaterBlockEntity.updateConnectedHeaters(level, pos, state);
    }

    @Override
    public boolean canPropagate(Level level, BlockPos pos, BlockState state, Direction direction) {
        return level.hasNeighborSignal(pos)
                && state.getOptionalValue(FACING).filter(direction::equals).isPresent()
                && (Connector.isConnector(level, pos, direction) || Connector.isBurningStorage(level, pos, direction));
    }

    @Override
    public WeatherState getAge() {
        return weatherState;
    }
}
