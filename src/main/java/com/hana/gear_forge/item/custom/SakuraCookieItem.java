package com.hana.gear_forge.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 樱花曲奇：4 樱花木粒 + 曲奇。比原版曲奇恢复更多饱食度，并给生命提升 I 持续 3 分钟。 */
public class SakuraCookieItem extends Item {

    private static final int HEALTH_BOOST_TICKS = 20 * 180;

    public SakuraCookieItem() {
        super(new Item.Properties()
                .stacksTo(16)
                .food(new FoodProperties.Builder()
                        .nutrition(6)
                        .saturationModifier(1.2F)
                        .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, HEALTH_BOOST_TICKS, 0));
        }
        return result;
    }
}
