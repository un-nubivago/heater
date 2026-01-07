package niv.heater.registry;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import niv.burning.api.BurningStorage;
import niv.heater.block.entity.HeaterBlockEntity;

public class HeaterRegistrar {
    private HeaterRegistrar() {
    }

    static {
        ItemStorage.SIDED.registerForBlockEntity(
                HeaterBlockEntity::getInventoryStorage,
                HeaterBlockEntityTypes.HEATER);

        BurningStorage.SIDED.registerForBlockEntity(
                (entity, side) -> entity.getBurningStorage(),
                HeaterBlockEntityTypes.HEATER);
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
