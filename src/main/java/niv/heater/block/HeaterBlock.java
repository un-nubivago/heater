package niv.heater.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import niv.heater.api.Connector;
import niv.heater.block.entity.HeaterBlockEntity;
import niv.heater.registry.HeaterBlockEntityTypes;

public class HeaterBlock extends AbstractFurnaceBlock implements Connector, WeatheringCopper {

    @SuppressWarnings("java:S1845")
    public static final MapCodec<HeaterBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WeatherState.CODEC.fieldOf("weathering_state").forGetter(HeaterBlock::getAge),
            Properties.CODEC.fieldOf("properties").forGetter(BlockBehaviour::properties))
            .apply(instance, HeaterBlock::new));

    private final WeatherState weatherState;

    public HeaterBlock(WeatherState weatherState, Properties properties) {
        super(properties);
        this.weatherState = weatherState;
    }

    @Override
    public MapCodec<? extends HeaterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeaterBlockEntity(pos, state);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof HeaterBlockEntity heater) {
            player.openMenu(heater);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, HeaterBlockEntityTypes.HEATER, HeaterBlockEntity::tick);
    }

    @Override
    protected void neighborChanged(BlockState blockState, Level level, BlockPos pos,
            Block sourceBlock, Orientation orientation, boolean notify) {
        if (level.isClientSide()) {
            return;
        }
        level.getBlockEntity(pos, HeaterBlockEntityTypes.HEATER).ifPresent(HeaterBlockEntity::makeDirty);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT).booleanValue()) {
            double x = pos.getX() + .5d;
            double y = pos.getY() + .0d;
            double z = pos.getZ() + .5d;
            if (random.nextDouble() < 0.1) {
                level.playLocalSound(x, y, z,
                        SoundEvents.BLASTFURNACE_FIRE_CRACKLE,
                        SoundSource.BLOCKS,
                        1.0F, 1.0F, false);
            }
            Direction direction = state.getValue(FACING);
            Direction.Axis axis = direction.getAxis();
            double r = random.nextDouble() * .6d - .3d;
            double dx = axis == Direction.Axis.X ? direction.getStepX() * .52d : r;
            double dy = random.nextDouble() * 9.0d / 16.0d;
            double dz = axis == Direction.Axis.Z ? direction.getStepZ() * .52d : r;
            level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, .0d, .0d, .0d);
        }
    }

    @Override
    public WeatherState getAge() {
        return weatherState;
    }
}
