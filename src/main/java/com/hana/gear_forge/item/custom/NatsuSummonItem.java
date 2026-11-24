package com.hana.gear_forge.item.custom;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ysm.NatsuBossNpc;
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

/** 浪漫的邀请函：右键地面召唤柚鸟夏 Boss（YSM 假玩家模型，三阶段）。 */
public class NatsuSummonItem extends Item {

    public NatsuSummonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            try {
                NatsuBossNpc boss = new NatsuBossNpc(serverLevel.getServer(), serverLevel,
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                if (context.getPlayer() != null) {
                    context.getPlayer().sendSystemMessage(Component.translatable(
                            "message.gear_forge.natsu_summon").withStyle(ChatFormatting.LIGHT_PURPLE));
                    if (!context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
                }
            } catch (Throwable t) {
                GearForgeMod.LOGGER.error("Failed to summon Natsu Boss", t);
                if (context.getPlayer() != null) {
                    context.getPlayer().sendSystemMessage(Component.literal(
                            "召唤失败：请确认已安装 YSM 并查看日志").withStyle(ChatFormatting.RED));
                }
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.natsu_summon")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
