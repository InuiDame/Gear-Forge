package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 柚鸟夏相关物品的获取规则：
 * <ul>
 *   <li>吃原版甜点食物有 0.5% 概率获得「浪漫的邀请函」；</li>
 *   <li>「甜点部的契约」仅限一次掉落：已领取过的玩家不再重复获得。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class NatsuItemEvents {

    private static final ResourceLocation CLAIMED_CONTRACT =
            ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "natsu_contract_claimed");

    /** 甜点类原版食物：吃完有概率掉落邀请函。 */
    private static boolean isDessert(Item item) {
        return item == Items.CAKE
                || item == Items.COOKIE
                || item == Items.PUMPKIN_PIE
                || item == Items.HONEY_BOTTLE
                || item == Items.SWEET_BERRIES
                || item == Items.GLOW_BERRIES
                || item == Items.APPLE
                || item == Items.GOLDEN_APPLE
                || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.MELON_SLICE
                || item == Items.SUGAR;
    }

    private NatsuItemEvents() {}

    /** 吃完甜点：0.5% 概率掉落邀请函。 */
    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;
        ItemStack used = event.getItem();
        if (used.isEmpty() || !isDessert(used.getItem())) return;

        // 0.5% = 1/200
        if (player.getRandom().nextFloat() >= 0.005F) return;

        ItemStack reward = new ItemStack(ModItems.NATSU_SUMMON.get());
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.sendSystemMessage(Component.literal(
                "「吃下这份甜点，浪漫便有了形状。」——获得了浪漫的邀请函！")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    /** 查询/设置玩家是否已领取过契约（供 NatsuBossEntity 掉落判断）。 */
    public static boolean hasClaimedContract(ServerPlayer player) {
        return player.getPersistentData().getBoolean(CLAIMED_CONTRACT.toString());
    }

    public static void markContractClaimed(ServerPlayer player) {
        player.getPersistentData().putBoolean(CLAIMED_CONTRACT.toString(), true);
    }
}
