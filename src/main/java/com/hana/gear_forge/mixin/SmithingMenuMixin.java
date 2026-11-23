package com.hana.gear_forge.mixin;

import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 原版锻造台不再注入本模组的任何操作。
 *
 * <p>所有模组强化/升级/重铸/宝石操作都挂在强化台专用配方类型
 * {@link com.hana.gear_forge.recipe.GearSmithingRecipe#FORGE_TABLE_TYPE} 上，
 * 原版锻造台只按 {@code RecipeType.SMITHING} 查配方，因此自然只保留锻造模板的
 * 原生升级与纹饰。本 Mixin 刻意留空，以保持 mixins.json 配置不变。
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
}
