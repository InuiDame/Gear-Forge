package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Random;

@EventBusSubscriber(modid = GearForgeMod.MODID)
public class LootModEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // 检查死亡的实体是不是僵尸
        if (event.getEntity() instanceof Zombie) {
            // 使用世界自带的随机数生成器，20% 几率掉落
            if (event.getEntity().level().random.nextFloat() < 0.02f) {
                // 创建一个 ItemEntity，位置就在僵尸死亡的位置
                ItemEntity drop = new ItemEntity(
                        event.getEntity().level(),
                        event.getEntity().getX(),
                        event.getEntity().getY(),
                        event.getEntity().getZ(),
                        new ItemStack(ModItems.LUCKY_DIAMOND.get(), 1)
                );
                // 添加到掉落物集合
                event.getDrops().add(drop);
            }
        }
    }
}