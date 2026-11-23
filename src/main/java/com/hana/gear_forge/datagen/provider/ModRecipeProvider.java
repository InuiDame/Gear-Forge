package com.hana.gear_forge.datagen.provider;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * 全部合成配方。
 *
 * <p>耀金相关的配方：
 * <ol>
 *   <li>耀金锭：3 金锭 + 3 钻石（固定配方）。</li>
 *   <li>耀金苹果：4 耀金锭 + 1 苹果。</li>
 *   <li>耀金硬币：4 金锭围 1 耀金锭。</li>
 *   <li>耀金装备：<b>不在这里</b>——它是锻造台配方（下界合金装备 + 耀金锭 + 原版下界合金升级模板），
 *       由 {@code GearSmithingRecipe} 在运行时判定，见 {@link com.hana.gear_forge.recipe.YaokinUpgradeRecipe}。
 *       没有工作台直接合成配方。</li>
 * </ol>
 */
public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // ===================== 原有材料 =====================

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

        // ===================== 强化台 =====================

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.FORGE_TABLE.get())
                .pattern("@X@")
                .pattern("###")
                .pattern("###")
                .define('@', Items.IRON_INGOT)
                .define('X', Items.SMITHING_TABLE)
                .define('#', net.minecraft.tags.ItemTags.PLANKS)
                .unlockedBy("has_smithing_table", has(Items.SMITHING_TABLE))
                .save(output);

        // ===================== 耀金 =====================

        // 耀金锭：3 金锭 + 3 钻石，固定配方（作者指定）。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.YAOKIN_INGOT.get())
                .requires(Items.GOLD_INGOT, 3)
                .requires(Items.DIAMOND, 3)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .save(output);

        // 耀金铁锤：耀金锭 + 木棍，形状沿用锻造锤但更重。
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.YAOKIN_HAMMER.get())
                .pattern(" ID")
                .pattern(" SI")
                .pattern("S  ")
                .define('I', ModItems.YAOKIN_INGOT.get())
                .define('D', ModItems.YAOKIN_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_yaokin_ingot", has(ModItems.YAOKIN_INGOT.get()))
                .save(output);

        // 耀金苹果：4 个耀金锭 + 1 个苹果。金苹果的强化版，附点金 II。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.YAOKIN_APPLE.get())
                .requires(ModItems.YAOKIN_INGOT.get(), 4)
                .requires(Items.APPLE)
                .unlockedBy("has_yaokin_ingot", has(ModItems.YAOKIN_INGOT.get()))
                .save(output);

        // 耀金硬币：4 个金锭围 1 个耀金锭。小饰品，幸运 +1。
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.YAOKIN_COIN.get())
                .pattern(" G ")
                .pattern("GYG")
                .pattern(" G ")
                .define('G', Items.GOLD_INGOT)
                .define('Y', ModItems.YAOKIN_INGOT.get())
                .unlockedBy("has_yaokin_ingot", has(ModItems.YAOKIN_INGOT.get()))
                .save(output);

        // 耀金剑/工具/盔甲不再有工作台配方：只能在锻造台用原版下界合金升级模板升级。
    }
}
