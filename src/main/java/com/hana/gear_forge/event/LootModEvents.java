package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 僵尸额外掉落幸运钻石。
 *
 * <p>用 {@link EventPriority#LOWEST}：让原版和战利品表先把“正常掉落”全部放进列表，本类最后再追加。
 * 否则额外掉落的幸运钻石会被 {@link GearEventHandlers#onLivingDrops} 的掠夺复制逻辑看到，
 * 结果取决于两个处理器的注册顺序（同一份代码在不同 JVM 上表现可能不同）。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public class LootModEvents {

    /** 幸运钻石的掉落概率（僵尸）。饰品效果很强，保持稀有。 */
    private static final float LUCKY_DIAMOND_CHANCE = 0.005F;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;
        // 只由玩家击杀时掉落，避免岩浆/摔死这类“无主击杀”也能刷钻石。
        if (!(event.getSource().getEntity() instanceof Player)) return;

        // 用实体自己的随机源，避免污染世界共享的随机序列。
        if (zombie.getRandom().nextFloat() >= LUCKY_DIAMOND_CHANCE) return;

        event.getDrops().add(new ItemEntity(
                zombie.level(),
                zombie.getX(), zombie.getY(), zombie.getZ(),
                new ItemStack(ModItems.LUCKY_DIAMOND.get(), 1)));
    }
}
