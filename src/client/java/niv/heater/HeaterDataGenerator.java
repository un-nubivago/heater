package niv.heater;

import static net.minecraft.data.models.blockstates.Condition.condition;
import static net.minecraft.data.models.blockstates.Variant.variant;
import static net.minecraft.data.models.blockstates.VariantProperties.MODEL;
import static net.minecraft.data.models.blockstates.VariantProperties.X_ROT;
import static net.minecraft.data.models.blockstates.VariantProperties.Y_ROT;
import static net.minecraft.data.models.blockstates.VariantProperties.Rotation.R180;
import static net.minecraft.data.models.blockstates.VariantProperties.Rotation.R270;
import static net.minecraft.data.models.blockstates.VariantProperties.Rotation.R90;
import static net.minecraft.data.models.model.TexturedModel.ORIENTABLE_ONLY_TOP;
import static net.minecraft.data.recipes.ShapedRecipeBuilder.shaped;
import static net.minecraft.data.recipes.ShapelessRecipeBuilder.shapeless;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.DOWN;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.UP;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;
import static niv.heater.Heater.MOD_ID;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.BlockTagProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiPartGenerator;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import niv.heater.block.entity.HeaterBlockEntity;
import niv.heater.registry.HeaterBlocks;
import niv.heater.registry.HeaterTabs;
import niv.heater.util.WeatheringCopperBlocks;

public class HeaterDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
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

        private static ModelTemplate create(String template, String suffix, TextureSlot... textureSlots) {
            return new ModelTemplate(
                    Optional.of(ResourceLocation.tryBuild(MOD_ID, "block/" + template)),
                    Optional.ofNullable(suffix),
                    textureSlots);
        }
    }

    private static class HeaterModelProvider extends FabricModelProvider {

        private static final PropertyDispatch ROTATION_FACING = PropertyDispatch
                .property(BlockStateProperties.FACING)
                .select(Direction.DOWN, variant().with(X_ROT, R90))
                .select(Direction.UP, variant().with(X_ROT, R270))
                .select(Direction.NORTH, variant())
                .select(Direction.EAST, variant().with(Y_ROT, R90))
                .select(Direction.SOUTH, variant().with(Y_ROT, R180))
                .select(Direction.WEST, variant().with(Y_ROT, R270));

        private static final PropertyDispatch ROTATION_HORIZONTAL_FACING = PropertyDispatch
                .property(BlockStateProperties.HORIZONTAL_FACING)
                .select(Direction.NORTH, variant())
                .select(Direction.EAST, variant().with(Y_ROT, R90))
                .select(Direction.SOUTH, variant().with(Y_ROT, R180))
                .select(Direction.WEST, variant().with(Y_ROT, R270));

        private static final TexturedModel.Provider THERMOSTAT = TexturedModel
                .createDefault(HeaterModelProvider::orientableFullTilt, HeaterModelTemplates.THERMOSTAT);
        private static final TexturedModel.Provider PIPE_CORE = TexturedModel
                .createDefault(HeaterModelProvider::pipeCore, HeaterModelTemplates.PIPE_CORE);
        private static final TexturedModel.Provider PIPE_ARM = TexturedModel
                .createDefault(HeaterModelProvider::pipeArm, HeaterModelTemplates.PIPE_ARM);

        public HeaterModelProvider(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generateBlockStateModels(BlockModelGenerators generator) {
            HeaterBlocks.HEATER.waxedMapping()
                    .forEach((block, waxed) -> createWaxingFurnace(generator, block, waxed));
            HeaterBlocks.THERMOSTAT.waxedMapping()
                    .forEach((block, waxed) -> createWaxingOrientable(generator, block, waxed));
            HeaterBlocks.HEAT_PIPE.waxedMapping()
                    .forEach((block, waxed) -> createWaxingPipe(generator, block, waxed));
        }

        @Override
        public void generateItemModels(ItemModelGenerators generator) {
            // no-op
        }

        private static final void createWaxingFurnace(BlockModelGenerators generator, Block block, Block waxed) {
            var unlit = ORIENTABLE_ONLY_TOP.create(block, generator.modelOutput);
            var lit = ORIENTABLE_ONLY_TOP.get(block)
                    .updateTextures(mapping -> mapping.put(TextureSlot.FRONT,
                            TextureMapping.getBlockTexture(block, "_front_on")))
                    .createWithSuffix(block, "_on", generator.modelOutput);

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .multiVariant(block)
                    .with(BlockModelGenerators.createBooleanModelDispatch(LIT, lit, unlit))
                    .with(ROTATION_HORIZONTAL_FACING));

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .multiVariant(waxed)
                    .with(BlockModelGenerators.createBooleanModelDispatch(LIT, lit, unlit))
                    .with(ROTATION_HORIZONTAL_FACING));

            generator.delegateItemModel(block, unlit);
            generator.delegateItemModel(waxed, unlit);
        }

        private static final void createWaxingOrientable(BlockModelGenerators generator, Block block, Block waxed) {
            var model = THERMOSTAT.create(block, generator.modelOutput);
            var variant = variant().with(MODEL, model);

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .multiVariant(block, variant)
                    .with(ROTATION_FACING));

            generator.blockStateOutput.accept(MultiVariantGenerator
                    .multiVariant(waxed, variant)
                    .with(ROTATION_FACING));

            generator.delegateItemModel(block, model);
            generator.delegateItemModel(waxed, model);
        }

        private static final void createWaxingPipe(BlockModelGenerators generator, Block block, Block waxed) {
            var core = PIPE_CORE.create(block, generator.modelOutput);
            var arm = PIPE_ARM.create(block, generator.modelOutput);

            generator.blockStateOutput.accept(MultiPartGenerator.multiPart(block)
                    .with(variant().with(MODEL, core))
                    .with(condition().term(NORTH, true), variant().with(MODEL, arm))
                    .with(condition().term(EAST, true), variant().with(MODEL, arm).with(Y_ROT, R90))
                    .with(condition().term(SOUTH, true), variant().with(MODEL, arm).with(Y_ROT, R180))
                    .with(condition().term(WEST, true), variant().with(MODEL, arm).with(Y_ROT, R270))
                    .with(condition().term(DOWN, true), variant().with(MODEL, arm).with(X_ROT, R90))
                    .with(condition().term(UP, true), variant().with(MODEL, arm).with(X_ROT, R270)));

            generator.blockStateOutput.accept(MultiPartGenerator.multiPart(waxed)
                    .with(variant().with(MODEL, core))
                    .with(condition().term(NORTH, true), variant().with(MODEL, arm))
                    .with(condition().term(EAST, true), variant().with(MODEL, arm).with(Y_ROT, R90))
                    .with(condition().term(SOUTH, true), variant().with(MODEL, arm).with(Y_ROT, R180))
                    .with(condition().term(WEST, true), variant().with(MODEL, arm).with(Y_ROT, R270))
                    .with(condition().term(DOWN, true), variant().with(MODEL, arm).with(X_ROT, R90))
                    .with(condition().term(UP, true), variant().with(MODEL, arm).with(X_ROT, R270)));

            generator.delegateItemModel(block, core);
            generator.delegateItemModel(waxed, core);
        }

        private static TextureMapping orientableFullTilt(Block block) {
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

        private HeaterEnglishLanguageProvider(FabricDataOutput dataOutput) {
            super(dataOutput);
        }

        @Override
        public void generateTranslations(TranslationBuilder builder) {
            addAll(builder, "Heater", HeaterBlocks.HEATER);
            addAll(builder, "Heat Pipe", HeaterBlocks.HEAT_PIPE);
            addAll(builder, "Thermostat", HeaterBlocks.THERMOSTAT);

            builder.add(HeaterBlockEntity.CONTAINER_NAME, Heater.MOD_NAME);
            builder.add(HeaterTabs.TAB_NAME, Heater.MOD_NAME);
        }

        private void addAll(TranslationBuilder builder, String name, WeatheringCopperBlocks blocks) {
            builder.add(blocks.unaffected(), name);
            builder.add(blocks.exposed(), "Exposed " + name);
            builder.add(blocks.weathered(), "Weathered " + name);
            builder.add(blocks.oxidized(), "Oxidized " + name);
            builder.add(blocks.waxed(), "Waxed " + name);
            builder.add(blocks.waxedExposed(), "Waxed Exposed " + name);
            builder.add(blocks.waxedWeathered(), "Waxed Weathered " + name);
            builder.add(blocks.waxedOxidized(), "Waxed Oxidized " + name);
        }
    }

    private static class HeaterLootTableProvider extends FabricBlockLootTableProvider {

        private HeaterLootTableProvider(FabricDataOutput dataOutput) {
            super(dataOutput);
        }

        @Override
        public void generate() {
            HeaterBlocks.HEATER.forEach(block -> this.add(block, this::createNameableBlockEntityTable));
            HeaterBlocks.THERMOSTAT.forEach(this::dropSelf);
            HeaterBlocks.HEAT_PIPE.forEach(this::dropSelf);
        }
    }

    private static final class HeaterRecipeProvider extends FabricRecipeProvider {

        public HeaterRecipeProvider(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void buildRecipes(Consumer<FinishedRecipe> output) {
            shaped(RecipeCategory.MISC, HeaterBlocks.HEATER.unaffected())
                    .pattern("ccc")
                    .pattern("cfc")
                    .pattern("ccc")
                    .define('c', Items.COPPER_INGOT)
                    .define('f', Items.FURNACE)
                    .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                    .unlockedBy(getHasName(Items.FURNACE), has(Items.FURNACE))
                    .save(output);

            generateWaxingRecipe(output, HeaterBlocks.HEATER);

            shaped(RecipeCategory.MISC, HeaterBlocks.HEAT_PIPE.unaffected())
                    .pattern("ccc")
                    .define('c', Items.COPPER_INGOT)
                    .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                    .save(output);

            generateWaxingRecipe(output, HeaterBlocks.HEAT_PIPE);

            shaped(RecipeCategory.MISC, HeaterBlocks.THERMOSTAT.unaffected())
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

            generateWaxingRecipe(output, HeaterBlocks.THERMOSTAT);
        }

        private void generateWaxingRecipe(Consumer<FinishedRecipe> output, WeatheringCopperBlocks blocks) {
            blocks.waxedMapping().forEach((block, waxed) -> shapeless(RecipeCategory.MISC, waxed)
                    .requires(block).requires(Items.HONEYCOMB)
                    .unlockedBy(getHasName(block), has(block))
                    .unlockedBy(getHasName(Items.HONEYCOMB), has(Items.HONEYCOMB))
                    .save(output));
        }
    }

    private static class HeaterTagProvider extends BlockTagProvider {

        public HeaterTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup) {
            getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(HeaterBlocks.HEATER.asList().toArray(Block[]::new))
                    .add(HeaterBlocks.HEAT_PIPE.asList().toArray(Block[]::new))
                    .add(HeaterBlocks.THERMOSTAT.asList().toArray(Block[]::new))
                    .setReplace(false);
        }
    }
}
