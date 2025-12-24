package niv.heater.util;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public abstract class HeaterContainer extends SimpleContainer implements SidedStorageBlockEntity, WorldlyContainer {

    private static final int[] SLOTS = new int[] { 0 };

    private final SingleStackStorage fullStorage = new SingleStackStorage() {
        @Override
        protected ItemStack getStack() {
            return HeaterContainer.this.getItem(0);
        }

        @Override
        protected void setStack(ItemStack stack) {
            HeaterContainer.this.setItem(0, stack);
        }

        @Override
        protected boolean canInsert(ItemVariant itemVariant) {
            return HeaterContainer.this.canPlaceItem(0, itemVariant.toStack());
        }

        @Override
        protected boolean canExtract(ItemVariant itemVariant) {
            return HeaterContainer.this.canTakeItemThroughFace(0, itemVariant.toStack(), Direction.DOWN);
        }

        @Override
        protected void onFinalCommit() {
            HeaterContainer.this.setChanged();
        }
    };

    private final SingleStackStorage insertStorage = new SingleStackStorage() {
        @Override
        protected ItemStack getStack() {
            return HeaterContainer.this.getItem(0);
        }

        @Override
        protected void setStack(ItemStack stack) {
            HeaterContainer.this.setItem(0, stack);
        }

        @Override
        protected boolean canInsert(ItemVariant itemVariant) {
            return HeaterContainer.this.canPlaceItem(0, itemVariant.toStack());
        }

        @Override
        protected boolean canExtract(ItemVariant itemVariant) {
            return false;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            HeaterContainer.this.setChanged();
        }
    };

    private final SingleStackStorage extractStorage = new SingleStackStorage() {
        @Override
        protected ItemStack getStack() {
            return HeaterContainer.this.getItem(0);
        }

        @Override
        protected void setStack(ItemStack stack) {
            HeaterContainer.this.setItem(0, stack);
        }

        @Override
        protected boolean canInsert(ItemVariant itemVariant) {
            return false;
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        protected boolean canExtract(ItemVariant itemVariant) {
            return HeaterContainer.this.canTakeItemThroughFace(0, itemVariant.toStack(), Direction.DOWN);
        }

        @Override
        protected void onFinalCommit() {
            HeaterContainer.this.setChanged();
        }
    };

    protected HeaterContainer() {
        super(1);
    }

    protected abstract boolean isFuel(ItemStack itemStack);

    // SimpleContainer

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return this.isFuel(itemStack) || itemStack.is(Items.BUCKET) && !this.items.get(slot).is(Items.BUCKET);
    }

    @Override
    public abstract void setChanged();

    // SidedStorageBlockEntity

    @Override
    public @Nullable Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        if (side == null) {
            return this.fullStorage;
        } else if (side == Direction.DOWN) {
            return this.extractStorage;
        } else {
            return this.insertStorage;
        }
    }

    // WorldlyContainer

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return this.canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return direction == Direction.DOWN && (itemStack.is(Items.WATER_BUCKET) || itemStack.is(Items.BUCKET));
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }
}
