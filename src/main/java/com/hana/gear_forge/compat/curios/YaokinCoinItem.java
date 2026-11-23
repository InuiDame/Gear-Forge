package com.hana.gear_forge.compat.curios;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;


/**
 * 耀金硬币：小饰品。
 *
 * <p>自身给幸运 +1。和幸运钻石同时装备时，由 {@code YaokinCoinEvents} 额外补一条 +20，
 * 让幸运钻石的 +20 变成 +40。携带者击杀怪物时还会获得 30 秒点金 I。
 */
public class YaokinCoinItem extends Item implements ICurioItem {

    public static final Identifier LUCK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("gear_forge", "yaokin_coin_luck");

    /** 与幸运钻石的 +20 对齐，同时装备时额外补同样的数值。 */
    public static final double LUCKY_DIAMOND_BONUS = 20.0D;

    public YaokinCoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, Identifier id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> multimap = ArrayListMultimap.create();
        multimap.put(Attributes.LUCK, new AttributeModifier(
                LUCK_MODIFIER_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE));
        return multimap;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.gear_forge.yaokin_coin").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
