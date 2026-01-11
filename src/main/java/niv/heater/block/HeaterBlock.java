package niv.heater.block;

import static niv.heater.registry.HeaterBlockEntityTypes.HEATER;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import niv.burning.api.BurningStorage;
import niv.burning.api.FuelVariant;
import niv.heater.block.entity.HeaterBlockEntity;
import niv.heater.registry.HeaterBlockEntityTypes;
import niv.heater.registry.HeaterBlocks;

public class HeaterBlock extends AbstractFurnaceBlock {

    public HeaterBlock(Properties properties) {
        super(properties);
    }

    public WeatherState getAge() {
        return ((WeatheringCopper) HeaterBlocks.HEATER.waxedMapping().inverse()
                .getOrDefault(this, HeaterBlocks.HEATER.unaffected())).getAge();
    }

    public InsertionOnlyStorage<FuelVariant> getStatelessStorage(Level level, BlockPos pos) {
        return (resource, maxAmount, transaction) -> {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            var inserted = this.getAge().ordinal() + 1;

            if (inserted >= maxAmount)
                return maxAmount;

            var dirs = getConnectedDirection(level.random);

            for (int i = 0; i < dirs.length && inserted < maxAmount; i++) {
                var dir = dirs[i];
                var storage = BurningStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (storage != null && storage.supportsInsertion())
                    inserted += storage.insert(resource, maxAmount - inserted, transaction);
            }

            return inserted;
        };
    }

    private Direction[] getConnectedDirection(@Nullable RandomSource random) {
        var result = Direction.values();
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

    // AbstractFurnaceBlock

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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            var heater = level.getBlockEntity(pos, HEATER).orElse(null);
            if (heater != null)
                heater.setCustomName(stack.getHoverName());
        }
    }

    @Override
    @SuppressWarnings("java:S1874")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() instanceof HeaterBlock && newState.getBlock() instanceof HeaterBlock)
            return;

        var heater = level.getBlockEntity(pos, HEATER).orElse(null);
        if (heater != null) {
            if (level instanceof ServerLevel)
                Containers.dropContents(level, pos, heater);
            level.updateNeighbourForOutputSignal(pos, this);
        }

        if (state.hasBlockEntity() && !state.is(newState.getBlock()))
            level.removeBlockEntity(pos);
    }
}
