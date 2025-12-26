package niv.heater.block.entity;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
                    var bucketItem = fuelItem.getCraftingRemainingItem();
                    this.setItem(0, bucketItem == null ? ItemStack.EMPTY : new ItemStack(bucketItem));
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
    protected Component getDefaultName() {
        return DEFAULT_NAME.get();
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        return new HeaterMenu(syncId, inventory, this, this.burningStorage);
    }

    // BlockEntity (override)

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.items);
        this.burningStorage.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        this.burningStorage.save(tag);
    }

    @Override
    public void setChanged() {
        BurningStorageBlockEntity.tryUpdateLitProperty(this, this.burningStorage);
        super.setChanged();
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

    // Container

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        for (var stack : this.items)
            if (stack.isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        var stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty())
            this.setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.setChanged();
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
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
        var threshold = new double[] { 1d };

        searchBurningStorages(level, zero, (pos, storage) -> {
            if (zero.equals(pos)) {
                storages[0] = storage;
                threshold[0] = storage.getBurning(context).getPercent();
            } else if (storage instanceof HeaterBurningStorage) {
                // ignore
            } else if (storage.supportsInsertion() && storage.getBurning(context).getPercent() <= threshold[0]) {
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
        var closed = new HashSet<BlockPos>(64);
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
