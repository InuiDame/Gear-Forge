package com.hana.gear_forge.compat.jei;

import com.hana.gear_forge.GearForgeMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 里的"锻造台操作"分类。
 *
 * <p>布局照抄原版锻造台界面：模板槽在左上、底材在中间、材料在右上、结果在右侧。
 * 坐标与原版 {@code smithing.png} 的三格位置对齐（8/26/44/98, y=48），
 * 但去掉背景图，只用槽位本身，视觉上更贴近"这是一条操作说明"而不是"这是一张配方"。
 */
public class ForgeSmithingCategory implements IRecipeCategory<GearForgeJeiPlugin.SmithingDisplay> {

    /** 分类宽度：容纳 4 个槽位 + 箭头。 */
    private static final int WIDTH = 116;
    private static final int HEIGHT = 54;

    private final IDrawable icon;

    public ForgeSmithingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(
                com.hana.gear_forge.item.ModItems.FORGE_HAMMER.get()));
    }

    @Override
    public RecipeType<GearForgeJeiPlugin.SmithingDisplay> getRecipeType() {
        return GearForgeJeiPlugin.FORGE_SMITHING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.gear_forge.forge_smithing");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          GearForgeJeiPlugin.SmithingDisplay recipe, IFocusGroup focuses) {
        // 三格输入 + 结果，坐标与原版锻造台界面一致
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 20)
                .addItemStack(recipe.template())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 20)
                .addItemStack(recipe.base())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 20)
                .addItemStack(recipe.addition())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 20)
                .addItemStack(recipe.result())
                .setOutputSlotBackground();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder,
                                   GearForgeJeiPlugin.SmithingDisplay recipe,
                                   IFocusGroup focuses) {
        // 在底部写一行操作说明（"强化 +1" 之类）
        builder.addText(Component.translatable(recipe.descKey()), WIDTH, 10)
                .setPosition(0, HEIGHT - 12);
    }
}
