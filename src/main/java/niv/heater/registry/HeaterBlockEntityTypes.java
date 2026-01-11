package niv.heater.registry;

import static niv.heater.Heater.MOD_ID;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import niv.heater.block.entity.HeaterBlockEntity;
import niv.heater.block.entity.ThermostatBlockEntity;

public class HeaterBlockEntityTypes {
    private HeaterBlockEntityTypes() {
    }

    public static final BlockEntityType<HeaterBlockEntity> HEATER;

    public static final BlockEntityType<ThermostatBlockEntity> THERMOSTAT;

    static {
        HEATER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.tryBuild(MOD_ID, "heater"),
                BlockEntityType.Builder.of(
                        HeaterBlockEntity::new,
                        HeaterBlocks.HEATER.asList().toArray(Block[]::new))
                        .build());

        THERMOSTAT = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.tryBuild(MOD_ID, "thermostat"),
                BlockEntityType.Builder.of(
                        ThermostatBlockEntity::new,
                        HeaterBlocks.THERMOSTAT.asList().toArray(Block[]::new))
                        .build());
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
