package niv.heater.util;

import static net.minecraft.world.level.block.WeatheringCopper.WeatherState.EXPOSED;
import static net.minecraft.world.level.block.WeatheringCopper.WeatherState.OXIDIZED;
import static net.minecraft.world.level.block.WeatheringCopper.WeatherState.UNAFFECTED;
import static net.minecraft.world.level.block.WeatheringCopper.WeatherState.WEATHERED;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.function.TriFunction;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

// Added for backport compatibility
public record WeatheringBlocks(
        Block unaffected, Block exposed, Block weathered, Block oxidized,
        Block waxed, Block waxedExposed, Block waxedWeathered, Block waxedOxidized) {
    public static <A extends Block, B extends Block & WeatheringCopper> WeatheringBlocks create(
            String id,
            TriFunction<String, Function<Properties, Block>, Properties, Block> register,
            Function<Properties, A> waxedBlockGetter,
            BiFunction<WeatherState, Properties, B> weatheredBlockGetter,
            Function<WeatherState, Properties> propertiesGetter) {
        return new WeatheringBlocks(
                // Weathering
                register.apply(id,
                        properties -> weatheredBlockGetter.apply(UNAFFECTED, properties),
                        propertiesGetter.apply(UNAFFECTED)),
                register.apply("exposed_" + id,
                        properties -> weatheredBlockGetter.apply(EXPOSED, properties),
                        propertiesGetter.apply(EXPOSED)),
                register.apply("weathered_" + id,
                        properties -> weatheredBlockGetter.apply(WEATHERED, properties),
                        propertiesGetter.apply(WEATHERED)),
                register.apply("oxidized_" + id,
                        properties -> weatheredBlockGetter.apply(OXIDIZED, properties),
                        propertiesGetter.apply(OXIDIZED)),
                // Waxed
                register.apply("waxed_" + id,
                        waxedBlockGetter::apply,
                        propertiesGetter.apply(UNAFFECTED)),
                register.apply("waxed_exposed_" + id,
                        waxedBlockGetter::apply,
                        propertiesGetter.apply(EXPOSED)),
                register.apply("waxed_weathered_" + id,
                        waxedBlockGetter::apply,
                        propertiesGetter.apply(WEATHERED)),
                register.apply("waxed_oxidized_" + id,
                        waxedBlockGetter::apply,
                        propertiesGetter.apply(OXIDIZED)));
    }

    public ImmutableBiMap<Block, Block> weatheringMapping() {
        return ImmutableBiMap.of(
                this.unaffected, this.exposed,
                this.exposed, this.weathered,
                this.weathered, this.oxidized);
    }

    public ImmutableBiMap<Block, Block> waxedMapping() {
        return ImmutableBiMap.of(
                this.unaffected, this.waxed,
                this.exposed, this.waxedExposed,
                this.weathered, this.waxedWeathered,
                this.oxidized, this.waxedOxidized);
    }

    @SuppressWarnings("java:S4738")
    public ImmutableList<Block> asList() {
        return ImmutableList.of(
                this.unaffected, this.waxed,
                this.exposed, this.waxedExposed,
                this.weathered, this.waxedWeathered,
                this.oxidized, this.waxedOxidized);
    }

    public void forEach(Consumer<Block> consumer) {
        consumer.accept(this.unaffected);
        consumer.accept(this.exposed);
        consumer.accept(this.weathered);
        consumer.accept(this.oxidized);
        consumer.accept(this.waxed);
        consumer.accept(this.waxedExposed);
        consumer.accept(this.waxedWeathered);
        consumer.accept(this.waxedOxidized);
    }

    public WeatheringBlocks register() {
        this.weatheringMapping().forEach(OxidizableBlocksRegistry::registerOxidizableBlockPair);
        this.waxedMapping().forEach(OxidizableBlocksRegistry::registerWaxableBlockPair);
        return this;
    }
}
