package com.hana.gear_forge.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 图中夏之糖霜盾。1.21.1 沿用 ShieldItem 子类参与原版正面阻挡判定。 */
public class NatsuShieldItem extends ShieldItem {

    public NatsuShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.natsu_shield")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.gear_forge.natsu_shield.tacz")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
