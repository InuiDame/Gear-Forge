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

    /** 魔性铁砧让附魔台各槽位的等级需求降低多少。 */
    private static final int ENCHANT_LEVEL_DISCOUNT = 5;

    /** 附魔台的交互距离，与原版 {@code canInteractWithBlock(pos, 4.0)} 一致。 */
    private static final double TABLE_INTERACT_RANGE = 4.0;

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

        Player player = findTableOperator(event);
        if (player == null || !hasCursedAnvil(player)) return;

        // 附魔台的 costs[k] 在原版里既是"解锁槽位所需的经验等级"，也是"抽取附魔用的强度"
        // （EnchantmentMenu.clickMenuButton 把它同时用于等级检查和 getEnchantmentList）。
        // 所以这里的 -5 会同步降低附魔质量，这是原版机制决定的，不是本模组的副作用。
        // 描述文案因此写成"等级需求 -5（附魔强度同步降低）"。
        int newLevel = Math.max(1, event.getEnchantLevel() - ENCHANT_LEVEL_DISCOUNT);
        event.setEnchantLevel(newLevel);
    }

    /**
     * 找附魔台的操作者。
     *
     * <p>原版 {@link EnchantmentLevelSetEvent} 只带 level 和 pos，不带玩家。旧实现按 10 格
     * 找最近的玩家，多人时旁边站着一个戴铁砧的玩家就会影响别人的附魔台。
     *
     * <p>这里把半径收紧到原版 {@code canInteractWithBlock(pos, 4.0)} 的 4 格：
     * 超过这个距离本来就打不开附魔台界面，不可能成为操作者。
     */
    private static Player findTableOperator(EnchantmentLevelSetEvent event) {
        return event.getLevel().getNearestPlayer(
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(),
                TABLE_INTERACT_RANGE, false);
    }
}
