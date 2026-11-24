package com.hana.gear_forge.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 樱花饮：4 樱花木粒 + 水瓶。饮用后生命恢复 IV 持续 30 秒。 */
public class SakuraDrinkItem extends Item {

    private static final int REGEN_TICKS = 20 * 30;

    public SakuraDrinkItem() {
        super(new Item.Properties()
                .stacksTo(16)
                .food(new FoodProperties.Builder()
                        .nutrition(4)
                        .saturationModifier(0.6F)
                        .alwaysEdible()
                        .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 3));
        }
        return result;
    }
}
