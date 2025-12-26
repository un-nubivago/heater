package niv.heater.screen;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import niv.heater.registry.HeaterMenus;

public class HeaterMenu extends AbstractContainerMenu {

    public static final int SLOT_COUNT = 1;

    private final Container container;
    private final ContainerData containerData;

    public HeaterMenu(int syncId, Inventory inventory) {
        this(syncId, inventory, new SimpleContainer(1), new SimpleContainerData(2));
    }

    public HeaterMenu(int syncId, Inventory inventory,
            Container container, ContainerData containerData) {
        super(HeaterMenus.HEATER, syncId);
        checkContainerSize(container, 1);
        checkContainerDataCount(containerData, 2);
        this.container = container;
        this.containerData = containerData;

        addSlot(new HeaterFuelSlot(container, 0, 80, 44));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }

        addDataSlots(containerData);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        var itemStack = ItemStack.EMPTY;
        var slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            var slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (slotIndex == 0 ? !moveItemStackTo(slotStack, container.getContainerSize(), slots.size(), true)
                    : !moveItemStackTo(slotStack, 0, container.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    public boolean isLit() {
        return this.containerData.get(0) > 0;
    }

    public float getLitProgress() {
        float i = this.containerData.get(1);
        if (i == 0) {
            i = 200;
        }
        return Mth.clamp(this.containerData.get(0) / i, 0f, 1f);
    }

    private final class HeaterFuelSlot extends Slot {

        public HeaterFuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AbstractFurnaceBlockEntity.isFuel(stack) || FurnaceFuelSlot.isBucket(stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return FurnaceFuelSlot.isBucket(stack) ? 1 : super.getMaxStackSize(stack);
        }
    }
}
