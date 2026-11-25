package com.hana.gear_forge.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Beyond The Lumination —— 柚鸟夏持有的冲锋枪（角色专武）。
 *
 * <p>模型原型为手枪型乌兹 SMG。作为 Boss 的主手武器，模型手持层能看到枪。
 * 本身不参与玩家合成；Boss 的三个阶段都由它开火。
 */
public class NatsuGunItem extends Item {

    public NatsuGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.natsu_gun")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
