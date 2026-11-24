package com.hana.gear_forge.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        // 和原版药水一致：喝完留下空玻璃瓶。stack 已被 super 消耗 1 个。
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 3));
            ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
            // 原版行为：若这瓶是最后一瓶，直接把空瓶作为返回值（回到手上）；
            // 否则尝试塞进背包，塞不下就丢在脚边。
            if (result.isEmpty()) {
                return emptyBottle;
            } else if (!player.getInventory().add(emptyBottle)) {
                player.drop(emptyBottle, false);
            }
        }
        return result;
    }
}
