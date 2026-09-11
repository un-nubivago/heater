package niv.heater.block;

import static niv.heater.registry.HeaterBlockEntityTypes.THERMOSTAT;

import org.jspecify.annotations.NullMarked;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import niv.heater.block.entity.ThermostatBlockEntity;
import niv.heater.registry.HeaterBlocks;

@NullMarked
public class ThermostatBlock extends DirectionalBlock implements EntityBlock {

    @SuppressWarnings({ "null", "java:S1845" })
    public static final MapCodec<ThermostatBlock> CODEC = simpleCodec(ThermostatBlock::new);

    @SuppressWarnings("null")
    public ThermostatBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @SuppressWarnings("null")
    public WeatherState getAge() {
        return ((WeatheringCopper) HeaterBlocks.THERMOSTAT.waxedMapping().inverse()
                .getOrDefault(this, HeaterBlocks.THERMOSTAT.unaffected())).getAge();
    }

    @Override
    public MapCodec<? extends ThermostatBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @SuppressWarnings("null")
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("null")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @SuppressWarnings("null")
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermostatBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            level.getBlockEntity(pos, THERMOSTAT).ifPresent(player::openMenu);
        }
        return InteractionResult.SUCCESS;
    }
}
