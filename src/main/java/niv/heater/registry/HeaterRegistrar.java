package niv.heater.registry;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.level.block.Block;
import niv.burning.api.BurningStorage;
import niv.heater.block.HeatPipeBlock;
import niv.heater.block.entity.HeaterBlockEntity;
import niv.heater.block.entity.ThermostatBlockEntity;

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

        BurningStorage.SIDED.registerForBlockEntity(
                ThermostatBlockEntity::getBurningStorage,
                HeaterBlockEntityTypes.THERMOSTAT);

        BurningStorage.SIDED.registerForBlocks(
                (level, pos, state, entity, side) -> state.getBlock() instanceof HeatPipeBlock block
                        ? block.getStatelessStorage(level, pos, state)
                        : null,
                HeaterBlocks.HEAT_PIPE.asList().toArray(Block[]::new));
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
