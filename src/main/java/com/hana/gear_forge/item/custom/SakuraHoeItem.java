package com.hana.gear_forge.item.custom;

import com.hana.gear_forge.content.SakuraMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 樱花锄：数值同下界合金锄，右键对可施肥方块（作物、树苗、草等）自动施肥，
 * 每次消耗 1 点耐久。耕地行为沿用原版 {@link HoeItem}。
 */
public class SakuraHoeItem extends HoeItem {

    public SakuraHoeItem() {
        super(SakuraMaterial.TIER, new Item.Properties()
                .fireResistant()
                .attributes(HoeItem.createAttributes(SakuraMaterial.TIER, -4.0F, 0.0F)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof BonemealableBlock bonemealable
                && bonemealable.isValidBonemealTarget(level, pos, state)) {
            Player player = context.getPlayer();
            ItemStack stack = context.getItemInHand();
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                if (bonemealable.isBonemealSuccess(level, level.getRandom(), pos, state)) {
                    bonemealable.performBonemeal(serverLevel, level.getRandom(), pos, state);
                    if (player != null) {
                        EquipmentSlot slot = context.getHand() == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        stack.hurtAndBreak(1, player, slot);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }
}
