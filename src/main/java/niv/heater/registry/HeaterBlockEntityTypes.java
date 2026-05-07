package niv.heater.registry;

import static net.minecraft.resources.Identifier.fromNamespaceAndPath;
import static niv.heater.Heater.MOD_ID;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import niv.heater.block.entity.HeaterBlockEntity;
import niv.heater.block.entity.ThermostatBlockEntity;

@SuppressWarnings("null")
@NullMarked
public class HeaterBlockEntityTypes {
    private HeaterBlockEntityTypes() {
    }

    public static final BlockEntityType<@NonNull HeaterBlockEntity> HEATER;

    public static final BlockEntityType<@NonNull ThermostatBlockEntity> THERMOSTAT;

    static {
        HEATER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                fromNamespaceAndPath(MOD_ID, "heater"),
                FabricBlockEntityTypeBuilder.create(
                        HeaterBlockEntity::new,
                        HeaterBlocks.HEATER.asList().toArray(Block[]::new))
                        .build());

        THERMOSTAT = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                fromNamespaceAndPath(MOD_ID, "thermostat"),
                FabricBlockEntityTypeBuilder.create(
                        ThermostatBlockEntity::new,
                        HeaterBlocks.THERMOSTAT.asList().toArray(Block[]::new))
                        .build());
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
