package niv.heater.registry;

import static niv.heater.Heater.MOD_ID;

import java.util.ArrayList;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;

public class HeaterTabs {
    private HeaterTabs() {
    }

    public static final String TAB_NAME;

    public static final CreativeModeTab HEATER_TAB;

    static {
        TAB_NAME = "creative.heater.tab";

        var all = new ArrayList<Block>(24);
        HeaterBlocks.HEATER.forEach(all::add);
        HeaterBlocks.THERMOSTAT.forEach(all::add);
        HeaterBlocks.HEAT_PIPE.forEach(all::add);

        HEATER_TAB = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.tryBuild(MOD_ID, "tab"),
                FabricItemGroup.builder()
                        .icon(HeaterBlocks.HEATER.waxed().asItem()::getDefaultInstance)
                        .title(Component.translatable(TAB_NAME))
                        .displayItems((parameters, output) -> all.stream().map(Block::asItem).forEach(output::accept))
                        .build());
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
