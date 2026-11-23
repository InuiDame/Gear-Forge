package com.hana.gear_forge.compat.curios;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;


public class CursedAnvilItem extends Item implements ICurioItem {

    /** 激怒半径（格）。 */
    private static final double PROVOKE_RADIUS = 10.0;

    public CursedAnvilItem(Properties properties) {
        super(properties);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide() || player.tickCount % 100 != 0) {
            return; // 每5秒尝试一次
        }

        // 仇恨逻辑（10格内中立生物敌视）
        applyHostileToNearby(player);

    }

    /**
     * 让附近的中立生物敌视佩戴者。
     *
     * <p>只挑原版定义了中立行为的 {@link NeutralMob}（狼、末影人、猪灵、蜜蜂、北极熊、铁傀儡），
     * 并且排除已被驯服的个体——旧实现对"所有非 Enemy 的 Mob"生效，会让玩家自己的狗和猫、
     * 村民、牛羊全都转头打玩家。
     *
     * <p>铁傀儡单独放行：在 1.21.1 里它实现的是 {@code NeutralMob}，但它是村庄守护者，
     * "被魔性铁砧激怒"符合道具设定；如果不想让它参与，把 {@code provokeIronGolem} 关掉即可。
     */
    private void applyHostileToNearby(Player player) {
        var area = player.getBoundingBox().inflate(PROVOKE_RADIUS);
        var mobs = player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area);
        for (var mob : mobs) {
            if (mob instanceof net.minecraft.world.entity.monster.Enemy) continue;
            if (!(mob instanceof net.minecraft.world.entity.NeutralMob)) continue;
            if (mob.getTarget() == player) continue;
            // 有主人的个体（狼/猫/鹦鹉）不参与，避免宠物反咬主人。
            if (mob instanceof net.minecraft.world.entity.OwnableEntity ownable
                    && ownable.getOwnerReference() != null) continue;

            mob.setTarget(player);
            mob.setAggressive(true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.gear_forge.curse_anvil").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}