package niv.heater.util;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import niv.burning.api.BurningStorage;
import niv.burning.api.FuelVariant;
import niv.heater.block.HeaterBlock;

public class HeaterBurningStorage implements InsertionOnlyStorage<FuelVariant> {

    private final Level level;

    private final BlockPos pos;

    private final ThreadLocal<Boolean> explored = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public HeaterBurningStorage(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Override
    public long insert(FuelVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        if (this.explored.get().booleanValue()) {
            return 0L;
        } else {
            this.explored.set(Boolean.TRUE);
            transaction.addOuterCloseCallback(result -> this.explored.remove());
        }

        var inserted = 0L;

        var state = this.level.getBlockState(this.pos);

        if (state.getBlock() instanceof HeaterBlock heater)
            inserted += heater.getAge().ordinal();

        if (inserted >= maxAmount)
            return maxAmount;

        var dirs = Direction.values();
        for (var i = dirs.length - 1; i > 0; i--) {
            var j = this.level.random.nextInt(i + 1);
            var dir = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = dir;
        }

        for (int i = 0; i < dirs.length && inserted < maxAmount; i++) {
            var storage = BurningStorage.SIDED.find(this.level, this.pos.relative(dirs[i]), dirs[i].getOpposite());
            if (storage != null && storage.supportsInsertion())
                inserted += storage.insert(resource, maxAmount - inserted, transaction);
        }

        return inserted;
    }
}
