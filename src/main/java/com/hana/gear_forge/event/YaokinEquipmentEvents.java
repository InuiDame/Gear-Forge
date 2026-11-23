package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.compat.curios.YaokinCoinItem;
import com.hana.gear_forge.content.ModMobEffects;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 耀金装备/饰品的运行时效果：
 * <ul>
 *   <li>点金：挖石头掉金锭；</li>
 *   <li>四件套：点金 I + 金化；</li>
 *   <li>耀金硬币：幸运叠加、击杀给点金 I。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class YaokinEquipmentEvents {

    private static final Identifier COIN_DIAMOND_DOUBLE_ID =
            Identifier.fromNamespaceAndPath(GearForgeMod.MODID, "yaokin_coin_diamond_double");

    private static final int GILDED_REFRESH_TICKS = 60;
    private static final int COIN_MIDAS_TICKS = 20 * 30;

    private YaokinEquipmentEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.tickCount % 5 == 0) {
            MidasTouchHandler.tick(player);
        }
        if (player.tickCount % 20 == 0) {
            updateFullSet(player);
            updateCoinLuck(player);
        }
    }

    /** 四件耀金盔甲：常驻点金 I + 金化。 */
    private static void updateFullSet(ServerPlayer player) {
        boolean full = isYaokinArmor(player.getItemBySlot(EquipmentSlot.HEAD))
                && isYaokinArmor(player.getItemBySlot(EquipmentSlot.CHEST))
                && isYaokinArmor(player.getItemBySlot(EquipmentSlot.LEGS))
                && isYaokinArmor(player.getItemBySlot(EquipmentSlot.FEET));

        MidasTouchHandler.setArmorSource(player, full);
        if (full) {
            player.addEffect(new MobEffectInstance(
                    ModMobEffects.GILDED, GILDED_REFRESH_TICKS, 0, true, true, true));
        } else {
            player.removeEffect(ModMobEffects.GILDED);
        }
    }

    private static boolean isYaokinArmor(ItemStack stack) {
        return stack.is(ModItems.YAOKIN_HELMET.get())
                || stack.is(ModItems.YAOKIN_CHESTPLATE.get())
                || stack.is(ModItems.YAOKIN_LEGGINGS.get())
                || stack.is(ModItems.YAOKIN_BOOTS.get());
    }

    /**
     * 幸运硬币：自身 +1 由 Curios 的物品修饰符提供；这里只在同时装备幸运钻石时
     * 额外补 +20，使钻石的 +20 翻倍为 +40。
     */
    private static void updateCoinLuck(ServerPlayer player) {
        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        if (luck == null) return;

        boolean both = hasCurio(player, ModItems.YAOKIN_COIN.get())
                && hasCurio(player, ModItems.LUCKY_DIAMOND.get());
        // 先无条件移除再按需添加：这个回调每 tick 都跑，直接 add 会在第二次触发
        // "Modifier is already applied on this attribute!"（见 crash-2026-11-23_20.04.07）。
        luck.removeModifier(COIN_DIAMOND_DOUBLE_ID);
        if (both) {
            luck.addTransientModifier(new AttributeModifier(
                    COIN_DIAMOND_DOUBLE_ID, YaokinCoinItem.LUCKY_DIAMOND_BONUS,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** 点金：等级 I 掉 1 个金锭，等级 II 及以上掉 2 个。 */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        if (!isStone(event.getState())) return;

        int level = MidasTouchHandler.currentLevel(player);
        if (level <= 0) return;

        event.getDrops().clear();
        int count = level >= 2 ? 2 : 1;
        var pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                new ItemStack(Items.GOLD_INGOT, count)));
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    /** 携带耀金硬币击杀怪物 → 30 秒点金 I。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 玩家死亡时清掉点金来源，避免复活后残留苹果/硬币的剩余时间。
        if (event.getEntity() instanceof ServerPlayer dead) {
            MidasTouchHandler.clear(dead);
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getEntity() instanceof Mob)) return;
        if (!hasCurio(player, ModItems.YAOKIN_COIN.get())) return;

        MidasTouchHandler.add(player, MidasTouchHandler.Source.COIN, 1, COIN_MIDAS_TICKS);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MidasTouchHandler.clear(player);
        }
    }

    private static boolean hasCurio(ServerPlayer player, net.minecraft.world.item.Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.isEquipped(item))
                .orElse(false);
    }
}
