package niv.heater.registry;

import static net.minecraft.resources.Identifier.fromNamespaceAndPath;
import static niv.heater.Heater.MOD_ID;

import java.util.ArrayList;

import org.jspecify.annotations.NullMarked;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("null")
@NullMarked
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
                fromNamespaceAndPath(MOD_ID, "tab"),
                FabricCreativeModeTab.builder()
                        .icon(HeaterBlocks.HEATER.waxed().asItem()::getDefaultInstance)
                        .title(Component.translatable(TAB_NAME))
                        .displayItems((parameters, output) -> all.stream().map(Block::asItem).forEach(output::accept))
                        .build());
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
