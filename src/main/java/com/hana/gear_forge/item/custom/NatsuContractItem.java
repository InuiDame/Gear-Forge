package com.hana.gear_forge.item.custom;

import com.hana.gear_forge.entity.ysm.NatsuCompanionNpc;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** 甜点部的契约：击败 Boss 后掉落，右键生成队友小夏。 */
public class NatsuContractItem extends Item {

    public NatsuContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            NatsuCompanionNpc companion = new NatsuCompanionNpc(
                    serverLevel.getServer(), serverLevel,
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    context.getPlayer());
            if (context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.translatable(
                        "message.gear_forge.natsu_companion").withStyle(ChatFormatting.LIGHT_PURPLE));
                if (!context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.natsu_contract")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.gear_forge.natsu_contract_2")
                .withStyle(ChatFormatting.GRAY));
    }
}
