package niv.heater;

import static net.minecraft.client.data.models.BlockModelGenerators.NOP;
import static net.minecraft.client.data.models.BlockModelGenerators.X_ROT_270;
import static net.minecraft.client.data.models.BlockModelGenerators.X_ROT_90;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_180;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_270;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_90;
import static net.minecraft.client.data.models.BlockModelGenerators.condition;
import static net.minecraft.client.data.models.model.TexturedModel.ORIENTABLE_ONLY_TOP;
import static net.minecraft.resources.Identifier.fromNamespaceAndPath;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.DOWN;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.UP;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;
import static niv.heater.Heater.MOD_ID;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.google.common.collect.ImmutableMap;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import niv.heater.block.entity.HeaterBlockEntity;
import niv.heater.registry.HeaterBlockItemIds;
import niv.heater.registry.HeaterBlocks;
import niv.heater.registry.HeaterTabs;

@NullMarked
public class HeaterDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        HeaterBlockItemIds.initialize();
        HeaterBlocks.initialize();
        HeaterTabs.initialize();

        var pack = fabricDataGenerator.createPack();

        pack.addProvider(HeaterModelProvider::new);
        pack.addProvider(HeaterEnglishLanguageProvider::new);
        pack.addProvider(HeaterLootTableProvider::new);
        pack.addProvider(HeaterRecipeProvider::new);
        pack.addProvider(HeaterTagProvider::new);
    }

    private static class HeaterModelTemplates {
        private HeaterModelTemplates() {
        }

        public static final ModelTemplate THERMOSTAT = create("template_thermostat", null,
                TextureSlot.TOP, TextureSlot.SIDE, TextureSlot.BOTTOM);
        public static final ModelTemplate PIPE_CORE = create("pipe_core", "_core",
                TextureSlot.TEXTURE);
        public static final ModelTemplate PIPE_ARM = create("pipe_arm", "_arm",
                TextureSlot.TEXTURE);

        private static ModelTemplate create(String template, @Nullable String suffix, TextureSlot... textureSlots) {
            return new ModelTemplate(
                    Optional.of(fromNamespaceAndPath(MOD_ID, "block/" + template)),
                    Optional.ofNullable(suffix),
                    textureSlots);
        }
    }

    private static class HeaterModelProvider extends FabricModelProvider {

        private static final PropertyDispatch<VariantMutator> ROTATION_FACING = PropertyDispatch
                .modify(BlockStateProperties.FACING)
                .select(Direction.DOWN, X_ROT_90)
                .select(Direction.UP, X_ROT_270)
                .select(Direction.NORTH, NOP)
                .select(Direction.EAST, Y_ROT_90)
                .select(Direction.SOUTH, Y_ROT_180)
                .select(Direction.WEST, Y_ROT_270);

        private static final PropertyDispatch<VariantMutator> ROTATION_HORIZONTAL_FACING = PropertyDispatch
                .modify(BlockStateProperties.HORIZONTAL_FACING)
                .select(Direction.NORTH, NOP)
                .select(Direction.EAST, Y_ROT_90)
                .select(Direction.SOUTH, Y_ROT_180)
                .select(Direction.WEST, Y_ROT_270);

        private static final TexturedModel.Provider THERMOSTAT = TexturedModel
                .createDefault(HeaterModelProvider::thermostatFullTilt, HeaterModelTemplates.THERMOSTAT);

        private static final TexturedModel.Provider PIPE_CORE = TexturedModel
                .createDefault(HeaterModelProvider::pipeCore, HeaterModelTemplates.PIPE_CORE);

        private static final TexturedModel.Provider PIPE_ARM = TexturedModel
                .createDefault(HeaterModelProvider::pipeArm, HeaterModelTemplates.PIPE_ARM);

        public HeaterModelProvider(FabricPackOutput output) {
            super(output);
        }

        @Override
        public void generateBlockStateModels(BlockModelGenerators generator) {
            for (var state : WeatherState.values()) {
                createWaxingFurnace(generator,
                        HeaterBlocks.HEATER.weathering().pick(state),
                        HeaterBlocks.HEATER.waxed().pick(state));
                createWaxingThermostat(generator,
                        HeaterBlocks.THERMOSTAT.weathering().pick(state),
                        HeaterBlocks.THERMOSTAT.waxed().pick(state));
                createWaxingPipe(generator,
                        HeaterBlocks.HEAT_PIPE.weathering().pick(state),
                        HeaterBlocks.HEAT_PIPE.waxed().pick(state));
            }
        }

        @Override
        public void generateItemModels(ItemModelGenerators generator) {
            // no-op
        }

        private static final void createWaxingFurnace(BlockModelGenerators generator, Block block, Block waxed) {
            var unlit = BlockModelGenerators.plainVariant(ORIENTABLE_ONLY_TOP.create(block, generator.modelOutput));
            var lit = BlockModelGenerators.plainVariant(ORIENTABLE_ONLY_TOP.get(block)
                    .updateTextures(mapping -> mapping.put(TextureSlot.FRONT,
                            TextureMapping.getBlockTexture(block, "_front_on")))
                    .createWithSuffix(block, "_on", generator.modelOutput));

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .dispatch(block)
                    .with(BlockModelGenerators.createBooleanModelDispatch(LIT, lit, unlit))
                    .with(ROTATION_HORIZONTAL_FACING));

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .dispatch(waxed)
                    .with(BlockModelGenerators.createBooleanModelDispatch(LIT, lit, unlit))
                    .with(ROTATION_HORIZONTAL_FACING));

            generator.itemModelOutput.copy(block.asItem(), waxed.asItem());
        }

        private static final void createWaxingThermostat(BlockModelGenerators generator, Block block, Block waxed) {
            var model = THERMOSTAT.create(block, generator.modelOutput);
            var variant = BlockModelGenerators.plainVariant(model);

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .dispatch(block, variant)
                    .with(ROTATION_FACING));

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .dispatch(waxed, variant)
                    .with(ROTATION_FACING));

            generator.itemModelOutput.copy(block.asItem(), waxed.asItem());
        }

        private static final void createWaxingPipe(BlockModelGenerators generator, Block block, Block waxed) {
            var core = PIPE_CORE.create(block, generator.modelOutput);
            var arm = PIPE_ARM.create(block, generator.modelOutput);

            var coreVariant = BlockModelGenerators.plainVariant(core);
            var armVariant = BlockModelGenerators.plainVariant(arm);

            generator.blockStateOutput.accept(MultiPartGenerator.multiPart(block)
                    .with(coreVariant)
                    .with(condition().term(NORTH, true), armVariant)
                    .with(condition().term(EAST, true), armVariant.with(Y_ROT_90))
                    .with(condition().term(SOUTH, true), armVariant.with(Y_ROT_180))
                    .with(condition().term(WEST, true), armVariant.with(Y_ROT_270))
                    .with(condition().term(DOWN, true), armVariant.with(X_ROT_90))
                    .with(condition().term(UP, true), armVariant.with(X_ROT_270)));

            generator.blockStateOutput.accept(MultiPartGenerator.multiPart(waxed)
                    .with(coreVariant)
                    .with(condition().term(NORTH, true), armVariant)
                    .with(condition().term(EAST, true), armVariant.with(Y_ROT_90))
                    .with(condition().term(SOUTH, true), armVariant.with(Y_ROT_180))
                    .with(condition().term(WEST, true), armVariant.with(Y_ROT_270))
                    .with(condition().term(DOWN, true), armVariant.with(X_ROT_90))
                    .with(condition().term(UP, true), armVariant.with(X_ROT_270)));

            generator.registerSimpleItemModel(block, core);
            generator.registerSimpleItemModel(waxed, core);
        }

        private static TextureMapping thermostatFullTilt(Block block) {
            return new TextureMapping()
                    .put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"))
                    .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"))
                    .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"));
        }

        private static TextureMapping pipeCore(Block block) {
            return new TextureMapping()
                    .put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(block));
        }

        private static TextureMapping pipeArm(Block block) {
            return new TextureMapping()
                    .put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(block));
        }
    }

    private static class HeaterEnglishLanguageProvider extends FabricLanguageProvider {

        private static final ImmutableMap<WeatherState, String> BY_STATE = ImmutableMap.<WeatherState, String>builder()
                .put(WeatherState.UNAFFECTED, "")
                .put(WeatherState.EXPOSED, "Exposed ")
                .put(WeatherState.WEATHERED, "Weathered ")
                .put(WeatherState.OXIDIZED, "Oxidized ")
                .build();

        private static final String WAXED = "Waxed ";

        private HeaterEnglishLanguageProvider(FabricPackOutput dataOutput, CompletableFuture<Provider> registryLookup) {
            super(dataOutput, registryLookup);
        }

        @Override
        public void generateTranslations(Provider registryLookup, TranslationBuilder builder) {
            addAll(builder, "Heater", HeaterBlocks.HEATER);
            addAll(builder, "Heat Pipe", HeaterBlocks.HEAT_PIPE);
            addAll(builder, "Thermostat", HeaterBlocks.THERMOSTAT);

            builder.add(HeaterBlockEntity.CONTAINER_NAME, Heater.MOD_NAME);
            builder.add(HeaterTabs.TAB_NAME, Heater.MOD_NAME);
        }

        private void addAll(TranslationBuilder builder, String name, WeatheringCopperCollection<Block> blocks) {
            for (var state : WeatherState.values()) {
                var prefix = BY_STATE.get(state);

                var weathering = blocks.weathering().pick(state);
                builder.add(weathering, prefix + name);
                builder.add(weathering.asItem(), prefix + name);

                var waxed = blocks.waxed().pick(state);
                builder.add(waxed, WAXED + prefix + name);
                builder.add(waxed.asItem(), WAXED + prefix + name);
            }
        }
    }

    private static class HeaterLootTableProvider extends FabricBlockLootSubProvider {

        private HeaterLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<Provider> registryLookup) {
            super(dataOutput, registryLookup);
        }

        @Override
        public void generate() {
            HeaterBlocks.HEATER.forEach(block -> this.add(block, this::createNameableBlockEntityTable));
            HeaterBlocks.THERMOSTAT.forEach(this::dropSelf);
            HeaterBlocks.HEAT_PIPE.forEach(this::dropSelf);
        }
    }

    private static final class HeaterRecipeProvider extends FabricRecipeProvider {

        public HeaterRecipeProvider(FabricPackOutput output, CompletableFuture<Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        public String getName() {
            return "Recipe";
        }

        @Override
        protected RecipeProvider createRecipeProvider(Provider registryLookup, RecipeOutput exporter) {
            return new InnerRecipeProvider(registryLookup, exporter);
        }
    }

    private static final class InnerRecipeProvider extends RecipeProvider {

        protected InnerRecipeProvider(Provider registries, RecipeOutput output) {
            super(registries, output);
        }

        @Override
        public void buildRecipes() {
            shaped(RecipeCategory.MISC, HeaterBlocks.HEATER.weathering().unaffected())
                    .pattern("ccc")
                    .pattern("cfc")
                    .pattern("ccc")
                    .define('c', Items.COPPER_INGOT)
                    .define('f', Items.FURNACE)
                    .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                    .unlockedBy(getHasName(Items.FURNACE), has(Items.FURNACE))
                    .save(output);

            generateWaxingRecipe(HeaterBlocks.HEATER);

            shaped(RecipeCategory.MISC, HeaterBlocks.HEAT_PIPE.weathering().unaffected())
                    .pattern("ccc")
                    .define('c', Items.COPPER_INGOT)
                    .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                    .save(output);

            generateWaxingRecipe(HeaterBlocks.HEAT_PIPE);

            shaped(RecipeCategory.MISC, HeaterBlocks.THERMOSTAT.weathering().unaffected())
                    .pattern("ccc")
                    .pattern("#c#")
                    .pattern("#r#")
                    .define('c', Items.COPPER_INGOT)
                    .define('r', Items.REDSTONE)
                    .define('#', Items.COBBLESTONE)
                    .unlockedBy(getHasName(Items.COBBLESTONE), has(Items.COBBLESTONE))
                    .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);

            generateWaxingRecipe(HeaterBlocks.THERMOSTAT);
        }

        private void generateWaxingRecipe(WeatheringCopperCollection<Block> blocks) {
            for (var state : WeatherState.values()) {
                var block = blocks.weathering().pick(state);
                var waxed = blocks.waxed().pick(state);
                shapeless(RecipeCategory.MISC, waxed)
                        .requires(block).requires(Items.HONEYCOMB)
                        .unlockedBy(getHasName(block), has(block))
                        .unlockedBy(getHasName(Items.HONEYCOMB), has(Items.HONEYCOMB))
                        .save(output);
            }
        }
    }

    private static class HeaterTagProvider extends FabricTagsProvider<Block> {

        public HeaterTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, Registries.BLOCK, registriesFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup) {
            builder(BlockTags.MINEABLE_WITH_PICKAXE)
                    .addAll(getResourceKeys(HeaterBlocks.HEATER))
                    .addAll(getResourceKeys(HeaterBlocks.HEAT_PIPE))
                    .addAll(getResourceKeys(HeaterBlocks.THERMOSTAT))
                    .setReplace(false);
        }

        @SuppressWarnings("null")
        private Stream<ResourceKey<Block>> getResourceKeys(WeatheringCopperCollection<Block> blocks) {
            var list = new ArrayList<Block>(8);
            blocks.forEach(list::add);
            return list.stream()
                    .map(BuiltInRegistries.BLOCK::getResourceKey)
                    .flatMap(Optional::stream);
        }
    }
}
