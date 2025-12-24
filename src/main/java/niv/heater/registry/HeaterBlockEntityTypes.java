package niv.heater.registry;

import static niv.heater.Heater.MOD_ID;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import niv.heater.block.entity.HeaterBlockEntity;

public class HeaterBlockEntityTypes {
    private HeaterBlockEntityTypes() {
    }

    public static final BlockEntityType<HeaterBlockEntity> HEATER;

    static {
        HEATER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.tryBuild(MOD_ID, "heater"),
                FabricBlockEntityTypeBuilder.create(
                        HeaterBlockEntity::new,
                        HeaterBlocks.HEATER.asList().toArray(Block[]::new))
                        .build());
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
