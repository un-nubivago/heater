package niv.heater.util;

import net.minecraft.world.inventory.ContainerData;
import niv.burning.api.base.SimpleBurningStorage;
import niv.heater.block.entity.HeaterBlockEntity;

public class HeaterBurningStorage extends SimpleBurningStorage implements ContainerData {

    private final HeaterBlockEntity owner;

    public HeaterBurningStorage(HeaterBlockEntity owner) {
        this.owner = owner;
    }

    // SimpleBurningStorage

    @Override
    protected void onFinalCommit() {
        this.owner.setChanged();
    }

    // ContainerData

    @Override
    public int get(int index) {
        switch (index) {
            case 0:
                return this.getCurrentBurning();
            case 1:
                return this.getMaxBurning();
            default:
                throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case 0:
                this.setCurrentBurning(value);
                break;
            case 1:
                this.setMaxBurning(value);
                break;
            default:
                throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}
