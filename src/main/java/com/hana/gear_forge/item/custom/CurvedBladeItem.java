package com.hana.gear_forge.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.item.component.ItemAttributeModifiers;


/**
 * 弧刀。按材质参数化，钻石/下界合金/耀金共用同一套行为。
 *
 * <p>26.3 起专门物品子类（SwordItem 等）被移除，所有物品都是 {@link Item}，
 * 武器属性通过 {@code Item.Properties#sword(...)} 组件设定。弧刀沿用剑组件，
 * 再覆盖攻速与攻击范围。
 *
 * <p>由耀金铁锤在强化台上把剑"重新锻打"而来，产物材质跟随底材。
 * 与剑相比：攻击范围 +1、最终攻速 2.0、伤害比同级剑低 1 点。每第 3 次命中触发特效斩击。
 */
public class CurvedBladeItem extends Item {

    public static final int SLASH_EVERY = 3;
    public static final float DAMAGE_PENALTY = 1.0F;
    public static final double REACH_BONUS = 1.0D;
    public static final float ATTACK_SPEED = 2.0F;
    private static final float ATTACK_SPEED_MODIFIER = ATTACK_SPEED - 4.0F;

    public CurvedBladeItem(Properties properties) {
        super(properties);
    }

    /** 构造弧刀的 Properties：标准剑（伤害 -1、攻速 2.0）+ 攻击范围 +1。 */
    public static Properties createBladeProperties(ToolMaterial material) {
        Properties props = new Properties()
                .sword(material, 3.0F - DAMAGE_PENALTY, ATTACK_SPEED_MODIFIER);
        return props.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("gear_forge", "curved_blade_reach"),
                                REACH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build());
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            com.hana.gear_forge.event.ArcSlashHandler.onBladeHit(player, stack, target);
        }
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.gear_forge.curved_blade").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.gear_forge.curved_blade.charge",
                        com.hana.gear_forge.component.ModDataComponents.arcSlashCharges(stack),
                        SLASH_EVERY)
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
