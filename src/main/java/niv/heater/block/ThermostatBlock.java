package niv.heater.block;

import static niv.heater.registry.HeaterBlockEntityTypes.THERMOSTAT;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import static niv.burning.api.FuelVariant.isFuel;

public class ThermostatBlock extends DirectionalBlock implements EntityBlock {

    @SuppressWarnings("java:S1845")
    public static final MapCodec<ThermostatBlock> CODEC = simpleCodec(ThermostatBlock::new);

    public ThermostatBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

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
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide())
            return stack.isEmpty() || isFuel(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;

        var entity = level.getBlockEntity(pos, THERMOSTAT).orElse(null);
        if (entity == null)
            return InteractionResult.PASS;

        if (stack.isEmpty()) {
            entity.unsetFilter();
            level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1.2f, 1.2f);
        } else if (entity.setFilter(stack)) {
            level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1.2f, 1.3f);
        } else {
            return InteractionResult.PASS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermostatBlockEntity(pos, state);
    }
}
