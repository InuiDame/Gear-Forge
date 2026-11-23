package com.hana.gear_forge.compat.jei;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.block.ModBlocks;
import com.hana.gear_forge.content.ModEnchantments;
import com.hana.gear_forge.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 插件。
 *
 * <p>做两件事：
 * <ol>
 *   <li><b>让锻造成果在 JEI 里可查</b>。原版工作台/锻造台的合成表 JEI 自己会读，
 *       但本模组的 {@code gear_smithing} 配方产物是运行时计算的（{@code getResultItem()} 返回空），
 *       JEI 无法从配方管理器反推出任何东西。所以这里手工构造一份"每种操作一条"的示例配方，
 *       用 {@link ForgeSmithingCategory} 展示。</li>
 *   <li><b>把耀金升级挂到原版锻造台分类里</b>。耀金升级本质是"模板 + 底材 + 材料"，
 *       与下界合金升级同构，直接以原版 {@code smithing} 配方形式喂给 JEI 即可。</li>
 * </ol>
 *
 * <p>本类带 {@code @JeiPlugin}，只在 JEI 存在时被它扫描；没有安装 JEI 时这个类根本不会被加载，
 * 因此本模组对 JEI 是<b>可选</b>依赖。
 */
@JeiPlugin
public class GearForgeJeiPlugin implements IModPlugin {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "jei_plugin");

    /** 自定义的"锻造台操作"配方类型。 */
    public static final RecipeType<SmithingDisplay> FORGE_SMITHING =
            RecipeType.create(GearForgeMod.MODID, "forge_smithing", SmithingDisplay.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ForgeSmithingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FORGE_SMITHING, buildForgeDisplays());

        // 耀金升级：喂给原版锻造台分类。用 SmithingTransformRecipe 的真实实例，
        // 这样 JEI 会按原版锻造界面渲染，玩家一眼能对上。
        registration.addRecipes(RecipeTypes.SMITHING, buildYaokinUpgradeRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(FORGE_SMITHING,
                ModBlocks.FORGE_TABLE.get(), Items.SMITHING_TABLE);
    }

    /** 每种锻造操作一条示例，让玩家在 JEI 里能查到"放什么会发生什么"。 */
    private static List<SmithingDisplay> buildForgeDisplays() {
        List<SmithingDisplay> list = new ArrayList<>();

        // 强化
        list.add(new SmithingDisplay(
                new ItemStack(Items.DIAMOND, 3),
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(ModItems.FORGE_HAMMER.get()),
                new ItemStack(Items.DIAMOND_SWORD),
                "jei.gear_forge.upgrade"));
        // 重铸
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(ModItems.REROLL_STONE.get()),
                new ItemStack(Items.DIAMOND_SWORD),
                "jei.gear_forge.reroll"));
        // 宝石镶嵌
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(ModItems.RUBY.get()),
                new ItemStack(Items.DIAMOND_SWORD),
                "jei.gear_forge.socket"));
        // 宝石升阶
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(ModItems.RUBY.get()),
                new ItemStack(ModItems.UPGRADE_CRYSTAL.get()),
                new ItemStack(ModItems.RUBY.get()),
                "jei.gear_forge.gem_tier"));
        // 副手模式
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(ModItems.TRANSFER_ORB.get()),
                new ItemStack(Items.DIAMOND_SWORD),
                "jei.gear_forge.offhand"));
        // 附魔书
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.ENCHANTED_BOOK),
                new ItemStack(Items.DIAMOND_SWORD),
                "jei.gear_forge.enchant"));
        // 摘除宝石
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(ModItems.GEM_REMOVER.get()),
                new ItemStack(Items.DIAMOND_SWORD),
                "jei.gear_forge.gem_remove"));
        // 转生
        list.add(new SmithingDisplay(
                new ItemStack(Items.NETHERITE_BLOCK, 8),
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(ModItems.REINCARNATION_STONE.get()),
                new ItemStack(Items.DIAMOND_SWORD),
                "jei.gear_forge.reincarnate"));
        // 耀金铁锤：剑 → 同材质弧刀
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(ModItems.YAOKIN_HAMMER.get()),
                new ItemStack(ModItems.DIAMOND_CURVED_BLADE.get()),
                "jei.gear_forge.curved_blade"));
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(Items.NETHERITE_SWORD),
                new ItemStack(ModItems.YAOKIN_HAMMER.get()),
                new ItemStack(ModItems.NETHERITE_CURVED_BLADE.get()),
                "jei.gear_forge.curved_blade"));
        list.add(new SmithingDisplay(
                ItemStack.EMPTY,
                new ItemStack(ModItems.YAOKIN_SWORD.get()),
                new ItemStack(ModItems.YAOKIN_HAMMER.get()),
                new ItemStack(ModItems.YAOKIN_CURVED_BLADE.get()),
                "jei.gear_forge.curved_blade"));

        return list;
    }

    /** 耀金升级：9 条（四件护甲 + 剑 + 四种工具），以原版锻造台配方形式展示。弧刀仍走耀金铁锤那条路径。 */
    private static List<RecipeHolder<net.minecraft.world.item.crafting.SmithingRecipe>>
    buildYaokinUpgradeRecipes() {
        List<RecipeHolder<net.minecraft.world.item.crafting.SmithingRecipe>> list = new ArrayList<>();
        net.minecraft.world.item.crafting.Ingredient template =
                net.minecraft.world.item.crafting.Ingredient.of(
                        Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        net.minecraft.world.item.crafting.Ingredient material =
                net.minecraft.world.item.crafting.Ingredient.of(ModItems.YAOKIN_INGOT.get());

        addUpgrade(list, template, material, Items.NETHERITE_HELMET, ModItems.YAOKIN_HELMET.get());
        addUpgrade(list, template, material, Items.NETHERITE_CHESTPLATE, ModItems.YAOKIN_CHESTPLATE.get());
        addUpgrade(list, template, material, Items.NETHERITE_LEGGINGS, ModItems.YAOKIN_LEGGINGS.get());
        addUpgrade(list, template, material, Items.NETHERITE_BOOTS, ModItems.YAOKIN_BOOTS.get());
        addUpgrade(list, template, material, Items.NETHERITE_SWORD, ModItems.YAOKIN_SWORD.get());
        addUpgrade(list, template, material, Items.NETHERITE_PICKAXE, ModItems.YAOKIN_PICKAXE.get());
        addUpgrade(list, template, material, Items.NETHERITE_AXE, ModItems.YAOKIN_AXE.get());
        addUpgrade(list, template, material, Items.NETHERITE_SHOVEL, ModItems.YAOKIN_SHOVEL.get());
        addUpgrade(list, template, material, Items.NETHERITE_HOE, ModItems.YAOKIN_HOE.get());
        return list;
    }

    private static void addUpgrade(
            List<RecipeHolder<net.minecraft.world.item.crafting.SmithingRecipe>> out,
            net.minecraft.world.item.crafting.Ingredient template,
            net.minecraft.world.item.crafting.Ingredient material,
            net.minecraft.world.item.Item base,
            net.minecraft.world.item.Item result) {
        var recipe = new net.minecraft.world.item.crafting.SmithingTransformRecipe(
                template,
                net.minecraft.world.item.crafting.Ingredient.of(base),
                material,
                new ItemStack(result));
        out.add(new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID,
                        "jei_yaokin_" + net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(result).getPath()),
                recipe));
    }

    /** 让编译器知道 ModEnchantments 的 import 有意义（附魔书在 JEI 里也走锻造分类）。 */
    @SuppressWarnings("unused")
    private static final Object ENCHANTMENT_KEYS = ModEnchantments.LIGHTNING_ARROW;

    /**
     * 一条锻造示例。
     *
     * @param template 模板槽
     * @param base     底材槽
     * @param addition 材料槽
     * @param result   结果
     * @param descKey  说明文本的语言键
     */
    public record SmithingDisplay(ItemStack template, ItemStack base, ItemStack addition,
                                  ItemStack result, String descKey) {}
}
