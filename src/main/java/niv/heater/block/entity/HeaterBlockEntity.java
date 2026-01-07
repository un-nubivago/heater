package niv.heater.block.entity;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Suppliers;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
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
import niv.burning.api.FuelVariant;
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
        return this.burningStorage.getCapacity() > 0;
    }

    private void consumeFuel(Transaction transaction) {
        var fuelStack = this.getItem(0);
        if (!this.isBurning() && !fuelStack.isEmpty()) {
            var fuelItem = fuelStack.getItem();
            var resource = FuelVariant.of(fuelItem);
            if (!resource.isBlank()) {
                fuelStack.shrink(1);
                if (fuelStack.isEmpty()) {
                    var bucketItem = fuelItem.getCraftingRemainder();
                    this.setItem(0, bucketItem == null ? ItemStack.EMPTY : bucketItem);
                }
                this.burningStorage.insert(resource, resource.getDuration(), transaction);
            }
        }
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
        SingleVariantStorage.readData(this.burningStorage, FuelVariant.CODEC, () -> FuelVariant.BLANK, valueInput);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ContainerHelper.saveAllItems(valueOutput, this.items);
        SingleVariantStorage.writeData(this.burningStorage, FuelVariant.CODEC, valueOutput);
    }

    @Override
    public void setChanged() {
        BurningStorageBlockEntity.tryUpdateLitProperty(this, isBurning());
        super.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack != null &&
                (!FuelVariant.of(stack.getItem()).isBlank()
                        || stack.is(Items.BUCKET) && !this.items.get(0).is(Items.BUCKET));
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
        try (var transaction = Transaction.openOuter()) {
            if (heater.isBurning()) {
                // TODO Propagation
            }

            if (heater.isBurning() && state.getBlock() instanceof HeaterBlock block)
                heater.burningStorage.extract(heater.burningStorage.variant, block.getAge().ordinal(), transaction);

            heater.consumeFuel(transaction);
            transaction.commit();
        }
    }
}
