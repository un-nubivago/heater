package niv.heater.block.entity;

import static niv.burning.api.BurningContext.worldlyContext;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Suppliers;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import niv.burning.api.Burning;
import niv.burning.api.BurningContext;
import niv.burning.api.BurningPropagator;
import niv.burning.api.BurningStorage;
import niv.burning.api.base.BurningStorageBlockEntity;
import niv.heater.block.HeaterBlock;
import niv.heater.registry.HeaterBlockEntityTypes;
import niv.heater.screen.HeaterMenu;
import niv.heater.util.HeaterBurningStorage;

public class HeaterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final String CONTAINER_NAME = "container.heater";

    private static final Supplier<Component> DEFAULT_NAME = Suppliers
            .memoize(() -> Component.translatable(CONTAINER_NAME));

    private final HeaterBurningStorage burningStorage = new HeaterBurningStorage(this);

    private final InventoryStorage[] wrappers = new InventoryStorage[7];

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(HeaterBlockEntityTypes.HEATER, pos, state);
    }

    public boolean isBurning() {
        return this.burningStorage.getCurrentBurning() > 0;
    }

    private void consumeFuel(BurningContext context, Transaction transaction) {
        var fuelStack = this.getItem(0);
        if (!this.isBurning() && !fuelStack.isEmpty()) {
            var fuelItem = fuelStack.getItem();
            var burning = Burning.of(fuelItem, context);
            if (burning != null) {
                fuelStack.shrink(1);
                if (fuelStack.isEmpty()) {
                    var bucketItem = fuelItem.getCraftingRemainder();
                    this.setItem(0, bucketItem == null ? ItemStack.EMPTY : bucketItem);
                }
                this.burningStorage.insert(burning.one(), context, transaction);
            }
        }
    }

    public BurningStorage getBurningStorage() {
        return this.burningStorage;
    }

    public InventoryStorage getInventoryStorage(@Nullable Direction side) {
        InventoryStorage.of(this, side);
        var index = side == null ? 6 : side.ordinal();
        if (wrappers[index] == null)
            wrappers[index] = InventoryStorage.of(this, side);
        return wrappers[index];
    }

    // BaseContainerBlockEntity (required)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME.get();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        return new HeaterMenu(syncId, inventory, this, this.burningStorage);
    }

    // BlockEntity (override)

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        ContainerHelper.loadAllItems(valueInput, this.items);
        this.burningStorage.load(valueInput);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ContainerHelper.saveAllItems(valueOutput, this.items);
        this.burningStorage.save(valueOutput);
    }

    @Override
    public void setChanged() {
        BurningStorageBlockEntity.tryUpdateLitProperty(this, this.burningStorage);
        super.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return worldlyContext(this.level).isFuel(stack.getItem())
                || stack.is(Items.BUCKET) && !this.items.get(0).is(Items.BUCKET);
    }

    // WorldlyContainer

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[] { 0 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != Direction.DOWN || stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET);
    }

    // Static

    public static void tick(Level level, BlockPos pos, BlockState state, HeaterBlockEntity heater) {
        var context = BurningContext.worldlyContext(level);
        try (var transaction = Transaction.openOuter()) {
            if (heater.isBurning())
                statelessPropagation(level, pos, context, transaction);

            if (heater.isBurning() && state.getBlock() instanceof HeaterBlock block)
                heater.burningStorage.extract(
                        heater.burningStorage.getBurning(context).withValue(block.getAge().ordinal() + 1, context),
                        context,
                        transaction);

            heater.consumeFuel(context, transaction);
            transaction.commit();
        }
    }

    private static void statelessPropagation(
            Level level, BlockPos zero, BurningContext context, Transaction transaction) {
        var storages = new BurningStorage[] { null, null };

        searchBurningStorages(level, zero, (pos, storage) -> {
            if (zero.equals(pos)) {
                storages[0] = storage;
            } else if (storage instanceof HeaterBurningStorage) {
                // ignore
            } else if (storage.supportsInsertion() && storage.getBurning(context).getPercent() <= .9) {
                storages[1] = storage;
                return true;
            }
            return false;
        });

        BurningStorage.transfer(storages[0], storages[1], storages[0].getBurning(context), context, transaction);
    }

    private static final void searchBurningStorages(Level level, BlockPos start,
            BiPredicate<BlockPos, BurningStorage> shallReturn) {
        var open = new LinkedList<Triple<Direction, BlockPos, Integer>>();
        var closed = HashSet.newHashSet(64);
        closed.add(start);

        for (var elem = Triple.of((Direction) null, start, 64); elem != null; elem = open.poll()) {
            var from = elem.getLeft();
            var pos = elem.getMiddle();
            var storage = BurningStorage.SIDED.find(level, pos, from);
            if (storage != null && shallReturn.test(pos, storage))
                return;

            BurningPropagator propagator = null;
            int hops = elem.getRight() - 1;
            if (hops > 0) {
                propagator = BurningPropagator.SIDED.find(level, pos, from);
            }

            if (propagator == null)
                continue;

            var dirs = propagator.evalPropagationTargets(level, pos).toArray(Direction[]::new);

            for (int i = dirs.length - 1; i > 0; --i) {
                int j = level.random.nextInt(i + 1);
                var d = dirs[j];
                dirs[j] = dirs[i];
                dirs[i] = d;
            }

            for (var dir : dirs) {
                var relative = pos.relative(dir);
                if (closed.add(relative))
                    open.addFirst(Triple.of(dir.getOpposite(), relative, hops));
            }
        }
    }
}
