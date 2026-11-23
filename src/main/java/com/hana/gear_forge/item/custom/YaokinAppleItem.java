package com.hana.gear_forge.item.custom;

import com.hana.gear_forge.event.MidasTouchHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 耀金苹果：普通金苹果的强化版。
 *
 * <p>食物数值比金苹果高（营养 6、饱和度系数 1.6），并把金苹果的吸收/再生时间延长：
 * 吸收 I 4 分钟、再生 II 10 秒。额外给点金 II 2 分钟。
 */
public class YaokinAppleItem extends Item {

    private static final int ABSORPTION_TICKS = 20 * 240;
    private static final int REGENERATION_TICKS = 20 * 10;
    private static final int MIDAS_TICKS = 20 * 120;

    public YaokinAppleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 1));
            MidasTouchHandler.add(player, MidasTouchHandler.Source.APPLE, 2, MIDAS_TICKS);
        }
        return result;
    }
}
