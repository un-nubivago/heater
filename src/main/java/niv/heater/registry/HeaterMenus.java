package niv.heater.registry;

import static net.minecraft.resources.Identifier.fromNamespaceAndPath;
import static niv.heater.Heater.MOD_ID;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import niv.heater.screen.HeaterMenu;

@SuppressWarnings("null")
@NullMarked
public class HeaterMenus {
    private HeaterMenus() {
    }

    public static final MenuType<@NonNull HeaterMenu> HEATER;

    static {
        HEATER = Registry.register(BuiltInRegistries.MENU, fromNamespaceAndPath(MOD_ID, "heater"),
                new MenuType<>(HeaterMenu::new, FeatureFlags.VANILLA_SET));
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
