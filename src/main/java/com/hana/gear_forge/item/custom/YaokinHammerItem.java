package com.hana.gear_forge.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 耀金铁锤。
 *
 * <p>它不是强化锤：强化锤（{@code forge_hammer}）用来 +1 等级，耀金铁锤用来把剑
 * <b>重新锻打成弧刀</b>——放在强化台的材料格，底材放一把剑即可。见 {@code GearSmithingRecipe}。
 *
 * <p>之所以单独做一把锤子而不是复用强化锤：这是两种完全不同的操作，放在同一个物品上
 * 会让"我放强化锤到底会发生什么"变得不可预测。
 */
public class YaokinHammerItem extends Item {

    public YaokinHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.yaokin_hammer").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
