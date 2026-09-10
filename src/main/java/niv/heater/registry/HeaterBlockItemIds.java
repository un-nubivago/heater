package niv.heater.registry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import niv.heater.Heater;

@NullMarked
public final class HeaterBlockItemIds {
    private HeaterBlockItemIds() {
    }

    public static final WeatheringCopperCollection<@NonNull BlockItemId> HEATER;

    public static final WeatheringCopperCollection<@NonNull BlockItemId> HEAT_PIPE;

    public static final WeatheringCopperCollection<@NonNull BlockItemId> THERMOSTAT;

    static {
        HEATER = createSimpleCopper("heater");

        HEAT_PIPE = createSimpleCopper("heat_pipe");

        THERMOSTAT = createSimpleCopper("thermostat");
    }

    @SuppressWarnings("null")
    private static WeatheringCopperCollection<@NonNull BlockItemId> createSimpleCopper(final String baseName) {
        return WeatheringCopperCollection.prefixWithState(WeatheringCopperCollection.create(baseName))
                .map(name -> Identifier.fromNamespaceAndPath(Heater.MOD_ID, name))
                .map(id -> BlockItemId.create(id, id));
    }

    public static final void initialize() {
        // Trigger static initialization
    }
}
