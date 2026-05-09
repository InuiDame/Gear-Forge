package com.hana.gear_forge.Provider;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.UPGRADE_CRYSTAL.get())
                .requires(Items.DIAMOND, 3)
                .requires(Items.NETHERITE_SCRAP)
                .requires(Items.BLAZE_POWDER, 2)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FORGE_HAMMER.get())
                .pattern(" ID")
                .pattern(" SI")
                .pattern("S  ")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DIAMOND)
                .define('S', Items.STICK)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.REROLL_STONE.get())
                .requires(Items.DIAMOND, 2)
                .requires(Items.AMETHYST_SHARD, 4)
                .requires(Items.LAPIS_LAZULI, 2)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RUBY.get())
                .requires(Items.REDSTONE_BLOCK, 2)
                .requires(Items.AMETHYST_SHARD, 2)
                .requires(Items.POPPED_CHORUS_FRUIT)
                .unlockedBy("has_redstone", has(Items.REDSTONE_BLOCK))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SAPPHIRE.get())
                .requires(Items.LAPIS_BLOCK, 2)
                .requires(Items.AMETHYST_SHARD, 2)
                .requires(Items.POPPED_CHORUS_FRUIT)
                .unlockedBy("has_lapis", has(Items.LAPIS_BLOCK))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.EMERALD_GEM.get())
                .requires(Items.EMERALD, 4)
                .requires(Items.AMETHYST_SHARD, 2)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.TOPAZ.get())
                .requires(Items.GOLD_BLOCK, 2)
                .requires(Items.AMETHYST_SHARD, 2)
                .requires(Items.POPPED_CHORUS_FRUIT)
                .unlockedBy("has_gold", has(Items.GOLD_BLOCK))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.JADE.get())
                .requires(Items.EMERALD_BLOCK, 2)
                .requires(Items.AMETHYST_SHARD, 2)
                .requires(Items.POPPED_CHORUS_FRUIT)
                .unlockedBy("has_emerald_block", has(Items.EMERALD_BLOCK))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ONYX.get())
                .requires(Items.OBSIDIAN, 2)
                .requires(Items.AMETHYST_SHARD, 2)
                .requires(Items.POPPED_CHORUS_FRUIT)
                .unlockedBy("has_obsidian", has(Items.OBSIDIAN))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.TRANSFER_ORB.get())
                .pattern(" ED")
                .pattern("EN ")
                .pattern(" D ")
                .define('E', Items.ENDER_PEARL)
                .define('D', Items.DIAMOND)
                .define('N', Items.NETHERITE_SCRAP)
                .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GEM_REMOVER.get())
                .pattern("D D")
                .pattern(" R ")
                .pattern(" D ")
                .define('D', Items.DIAMOND)
                .define('R', Items.REDSTONE_BLOCK)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.REINCARNATION_STONE.get())
                .requires(Items.NETHERITE_INGOT, 2)
                .requires(Items.DIAMOND_BLOCK, 2)
                .requires(Items.END_CRYSTAL)
                .requires(Items.GHAST_TEAR, 2)
                .unlockedBy("has_netherite", has(Items.NETHERITE_INGOT))
                .save(output);

    }
}
