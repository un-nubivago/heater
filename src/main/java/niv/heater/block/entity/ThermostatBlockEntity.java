package niv.heater.block.entity;

import static niv.heater.registry.HeaterBlockEntityTypes.HEATER;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import niv.burning.api.BurningStorage;
import niv.burning.api.FuelVariant;
import niv.heater.block.ThermostatBlock;
import niv.heater.registry.HeaterBlockEntityTypes;

public class ThermostatBlockEntity extends BlockEntity {

    private static final String TAG_FILTER = "filter";

    private final ThreadLocal<Boolean> hasBeenExploredAlready = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private FuelVariant filter = FuelVariant.BLANK;

    public ThermostatBlockEntity(BlockPos pos, BlockState state) {
        super(HeaterBlockEntityTypes.THERMOSTAT, pos, state);
    }

    public boolean setFilter(ItemStack stack) {
        if (stack == null)
            return false;

        var result = FuelVariant.of(stack);
        if (result.isBlank())
            return false;

        this.filter = result;
        return true;
    }

    public void unsetFilter() {
        this.filter = FuelVariant.BLANK;
    }

    private long tryInsert(Direction side, FuelVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        var facing = getBlockState().getOptionalValue(DirectionalBlock.FACING).orElseThrow(IllegalStateException::new);
        if (facing.equals(side))
            return 0L;

        if (this.hasBeenExploredAlready.get().booleanValue()) {
            return 0L;
        } else {
            this.hasBeenExploredAlready.set(Boolean.TRUE);
            transaction.addOuterCloseCallback(result -> this.hasBeenExploredAlready.remove());
        }

        var inserted = this.getBlockState().getBlock() instanceof ThermostatBlock block
                ? block.getAge().ordinal() + 1
                : 0;
        if (inserted >= maxAmount)
            return maxAmount;

        if (level.hasNeighborSignal(getBlockPos()) || this.filter == resource) {
            resource = this.filter.isBlank() ? resource : this.filter;

            var rel = getBlockPos().relative(facing);
            var storage = BurningStorage.SIDED.find(level, rel, facing.getOpposite());
            if (storage != null && (storage.supportsInsertion() || level.getBlockEntity(rel, HEATER).isPresent()))
                inserted += storage.insert(resource, maxAmount - inserted, transaction);
        }

        return inserted;
    }

    public InsertionOnlyStorage<FuelVariant> getBurningStorage(@Nullable Direction side) {
        return (resource, maxAmount, transaction) -> tryInsert(side, resource, maxAmount, transaction);
    }

    // BlockEntity

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.filter = FuelVariant.fromNbt(compoundTag.getCompound(TAG_FILTER));
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        compoundTag.put(TAG_FILTER, this.filter.toNbt());
    }
}
