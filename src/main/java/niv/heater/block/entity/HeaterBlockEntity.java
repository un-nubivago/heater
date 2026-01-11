package niv.heater.block.entity;

import static net.minecraft.world.inventory.FurnaceFuelSlot.isBucket;
import static niv.burning.api.FuelVariant.isFuel;
import static niv.heater.Heater.MOD_ID;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import niv.burning.api.FuelVariant;
import niv.burning.api.base.BurningStorageBlockEntity;
import niv.burning.api.base.SimpleBurningStorage;
import niv.heater.block.HeaterBlock;
import niv.heater.registry.HeaterBlockEntityTypes;
import niv.heater.screen.HeaterMenu;

public class HeaterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final String CONTAINER_NAME;

    private static final Component DEFAULT_NAME;

    static {
        CONTAINER_NAME = "container." + MOD_ID + ".heater";
        DEFAULT_NAME = Component.translatable(CONTAINER_NAME);
    }

    private final SimpleBurningStorage burningStorage = new SimpleBurningStorage() {
        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            HeaterBlockEntity.this.setChanged();
        }
    };

    private final ContainerData burningData = new ContainerData() {
        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return (int) HeaterBlockEntity.this.burningStorage.getAmount();
                case 1:
                    return (int) HeaterBlockEntity.this.burningStorage.getCapacity();
                default:
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    HeaterBlockEntity.this.burningStorage.amount = value;
                    break;
                case 1:
                    throw new UnsupportedOperationException();
                default:
                    throw new IndexOutOfBoundsException(index);
            }
        }
    };

    private final InventoryStorage[] wrappers = new InventoryStorage[7];

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(HeaterBlockEntityTypes.HEATER, pos, state);
    }

    public boolean isBurning() {
        return this.burningStorage.getCapacity() > 0;
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        try (var transaction = Transaction.openOuter()) {
            tryPropagate(level, pos, (HeaterBlock) state.getBlock(), transaction);
            tryConsumeFuel(transaction);
            transaction.commit();
        }
    }

    private boolean tryPropagate(Level level, BlockPos pos, HeaterBlock block, Transaction transaction) {
        var resource = this.burningStorage.getResource();
        var amount = this.burningStorage.getAmount();

        if (resource.isBlank() || this.burningStorage.getAmount() <= 0)
            return false;

        try (var nested = Transaction.openNested(transaction)) {
            var accepted = block.getStatelessStorage(level, pos).insert(resource, amount, nested);
            if (this.burningStorage.extract(resource, accepted, nested) == accepted)
                nested.commit();
        }
        return true;
    }

    private boolean tryConsumeFuel(Transaction transaction) {
        var fuelStack = this.getItem(0);

        if (this.isBurning() || fuelStack.isEmpty())
            return false;

        var fuelItem = fuelStack.getItem();
        var resource = FuelVariant.of(fuelItem);

        if (resource.isBlank())
            return false;

        fuelStack.shrink(1);

        if (fuelStack.isEmpty()) {
            var bucketItem = fuelItem.getCraftingRemainingItem();
            this.setItem(0, bucketItem == null ? ItemStack.EMPTY : new ItemStack(bucketItem));
        }

        return this.burningStorage.insert(resource, resource.getDuration(), transaction) > 0;
    }

    public Storage<FuelVariant> getBurningStorage() {
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
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        return new HeaterMenu(syncId, inventory, this, this.burningData);
    }

    // BlockEntity (override)

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        ContainerHelper.loadAllItems(compoundTag, this.items);
        this.burningStorage.readNbt(compoundTag);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        ContainerHelper.saveAllItems(compoundTag, this.items);
        this.burningStorage.writeNbt(compoundTag);
    }

    @Override
    public void setChanged() {
        BurningStorageBlockEntity.tryUpdateLitProperty(this, isBurning());
        super.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isFuel(stack) || (isBucket(stack) && !isBucket(this.items.get(0)));
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
        heater.tick(level, pos, state);
    }
}
