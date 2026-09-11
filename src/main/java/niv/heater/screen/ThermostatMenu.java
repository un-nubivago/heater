package niv.heater.screen;

import static niv.burning.api.FuelVariant.isFuel;

import org.jspecify.annotations.NullMarked;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import niv.heater.registry.HeaterMenus;

@NullMarked
public class ThermostatMenu extends AbstractContainerMenu {

    private final Container container;

    public ThermostatMenu(int syncId, Inventory inventory) {
        this(syncId, inventory, new SimpleContainer(1));
    }

    public ThermostatMenu(int syncId, Inventory inventory, Container container) {
        super(HeaterMenus.THERMOSTAT, syncId);
        this.container = container;
        this.addSlot(new PhantomSlot(container, 0, 80, 20));
        this.addStandardInventorySlots(inventory, 8, 51);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (containerInput == ContainerInput.PICKUP && slotIndex >= 0 && slotIndex < this.slots.size()
                && this.slots.get(slotIndex) instanceof PhantomSlot slot) {
            var carried = this.getCarried();
            if (carried.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else if (isFuel(carried)) {
                slot.set(carried.copyWithCount(1));
            }
        } else {
            super.clicked(slotIndex, buttonNum, containerInput, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        this.slots.get(0).set(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private static final class PhantomSlot extends Slot {
        public PhantomSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
