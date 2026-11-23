package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.content.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 给 7 个附魔加一行说明文字。
 *
 * <p>必须限定 {@code value = Dist.CLIENT}：{@code ItemTooltipEvent} 是纯客户端渲染路径上的事件，
 * 放在通用类里会让专用服务器也加载 tooltip 相关代码（当前不会崩，但迟早会有人往这里加客户端 API）。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
public final class EnchantmentTooltips {

    private EnchantmentTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || event.getEntity() == null) return;

        var lookup = event.getEntity().level().registryAccess().lookup(Registries.ENCHANTMENT);
        if (lookup.isEmpty()) return;
        Registry<Enchantment> registry = lookup.get();

        addDesc(stack, registry, ModEnchantments.LIGHTNING_ARROW, event.getToolTip());
        addDesc(stack, registry, ModEnchantments.FROST_ARROW, event.getToolTip());
        addDesc(stack, registry, ModEnchantments.EXPLOSIVE_ARROW, event.getToolTip());
        addDesc(stack, registry, ModEnchantments.ARMOR_PIERCING_ARROW, event.getToolTip());
        addDesc(stack, registry, ModEnchantments.MINING_CRIT, event.getToolTip());
        addDesc(stack, registry, ModEnchantments.ORE_HUNTER, event.getToolTip());
        addDesc(stack, registry, ModEnchantments.HARVEST_BOUNTY, event.getToolTip());
    }

    private static void addDesc(ItemStack stack, Registry<Enchantment> registry,
                               ResourceKey<Enchantment> key, List<Component> tooltips) {
        registry.get(key).ifPresent(holder -> {
            if (stack.getEnchantmentLevel(holder) > 0) {
                tooltips.add(Component.translatable("enchantment." + key.identifier().getNamespace()
                        + "." + key.identifier().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
