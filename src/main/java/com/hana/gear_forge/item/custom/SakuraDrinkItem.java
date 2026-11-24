package com.hana.gear_forge.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** 樱花饮：饮用后生命恢复 IV 持续 30 秒。食物属性由 ModItems 注入。 */
public class SakuraDrinkItem extends Item {

    private static final int REGEN_TICKS = 20 * 30;

    public SakuraDrinkItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 和原版药水一致：喝完留下空玻璃瓶。stack 已被 super 消耗 1 个。
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 3));
            ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
            if (result.isEmpty()) {
                return emptyBottle;
            } else if (!player.getInventory().add(emptyBottle)) {
                player.drop(emptyBottle, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        return result;
    }
}
