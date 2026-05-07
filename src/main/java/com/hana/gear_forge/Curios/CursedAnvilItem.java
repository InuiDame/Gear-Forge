package com.hana.gear_forge.Curios;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class CursedAnvilItem extends Item implements ICurioItem {

    public CursedAnvilItem() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide || player.tickCount % 100 != 0) {
            return; // 每5秒尝试一次
        }

        // 仇恨逻辑（10格内中立生物敌视）
        applyHostileToNearby(player);

    }
    private void applyHostileToNearby(Player player) {
        var area = player.getBoundingBox().inflate(10);
        var mobs = player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area);
        for (var mob : mobs) {
            if (!(mob instanceof net.minecraft.world.entity.monster.Enemy) && mob.getTarget() != player) {
                mob.setTarget(player);
                mob.setAggressive(true);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.curse_anvil").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}