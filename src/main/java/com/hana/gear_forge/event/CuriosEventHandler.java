package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = GearForgeMod.MODID)
public class CuriosEventHandler {

    private static final double CURSED_ANVIL_XP_BONUS = 0.15;

    // 经验 +15%
    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        int amount = event.getAmount();
        if (amount <= 0 || !hasCursedAnvil(event.getEntity())) return;

        // 单个经验球通常只有 1~7 点，直接 (int)(amount * 1.15) 会把加成截断成 0，
        // 这里把小数部分按概率补上，长期期望值正好是 +15%。
        double bonus = amount * CURSED_ANVIL_XP_BONUS;
        int extra = (int) bonus;
        if (event.getEntity().getRandom().nextDouble() < bonus - extra) {
            extra++;
        }
        event.setAmount(amount + extra);
    }

    private static boolean hasCursedAnvil(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.isEquipped(ModItems.CURSED_ANVIL.get()))
                .orElse(false);
    }

    @SubscribeEvent
    public static void onEnchantLevelSet(EnchantmentLevelSetEvent event) {
        // 等级为 0 表示该槽位本来就不可用，不要把它变成 1 级
        if (event.getEnchantLevel() <= 0) return;

        // 附魔台事件只有 BlockPos 和 Level，用 10 格内最近的玩家近似操作者
        Player player = event.getLevel().getNearestPlayer(
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(),
                10.0, false);

        if (player != null && hasCursedAnvil(player)) {
            // 佩戴饰品时，附魔等级需求降低 5 级
            int newLevel = Math.max(1, event.getEnchantLevel() - 5);
            event.setEnchantLevel(newLevel);
        }
    }
}
