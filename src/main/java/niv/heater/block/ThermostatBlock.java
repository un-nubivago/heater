package niv.heater.block;

import static niv.heater.registry.HeaterBlockEntityTypes.THERMOSTAT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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

public class ThermostatBlock extends DirectionalBlock implements EntityBlock {

    public ThermostatBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public WeatherState getAge() {
        return ((WeatheringCopper) HeaterBlocks.THERMOSTAT.waxedMapping().inverse()
                .getOrDefault(this, HeaterBlocks.THERMOSTAT.unaffected())).getAge();
    }

    // DirectionalBlock -- required

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

    @SuppressWarnings("java:S1874")
    @Override
    public InteractionResult use(
            BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        var entity = level.getBlockEntity(pos, THERMOSTAT).orElse(null);
        if (entity == null)
            return InteractionResult.FAIL;

        var stack = player.getItemInHand(hand);

        if (stack.isEmpty()) {
            entity.unsetFilter();
            level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1.2f, 1.2f);
        } else if (entity.setFilter(stack)) {
            level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1.2f, 1.3f);
        }

        return InteractionResult.SUCCESS;
    }

    // EntityBlock -- required

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermostatBlockEntity(pos, state);
    }
}
