package com.hana.gear_forge.item.custom;

import com.hana.gear_forge.content.YaokinMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * 耀金弧刀。
 *
 * <p>由耀金铁锤在强化台上把剑"重新锻打"而来。与剑相比的三处差异：
 * <ul>
 *   <li><b>攻击范围更长</b>：通过 {@code ENTITY_INTERACTION_RANGE} 属性 +1.0（原版玩家默认 3.0）。</li>
 *   <li><b>攻击冷却更低</b>：攻速 2.0（下界合金剑是 1.6），蓄力更快。</li>
 *   <li><b>伤害略低</b>：基础伤害比同级剑低 1 点，作为范围与速度的代价。</li>
 * </ul>
 *
 * <p>另外每第 3 次命中会触发一次"特效斩击"——见 {@code ArcSlashHandler}。
 * 计数器存在物品的 {@code arc_slash_charges} 组件上，随物品同步、随玩家切换武器保留。
 */
public class YaokinCurvedBladeItem extends SwordItem {

    /** 弧刀的蓄力计数组件名（见 ModDataComponents.ARC_SLASH_CHARGES）。 */
    public static final int SLASH_EVERY = 3;

    /** 相对剑的伤害惩罚。 */
    public static final float DAMAGE_PENALTY = 1.0F;

    /** 攻击范围加成（格）。 */
    public static final double REACH_BONUS = 1.0D;

    /** 攻击速度：剑是 1.6，弧刀更快。 */
    public static final float ATTACK_SPEED = 2.0F;

    public YaokinCurvedBladeItem(Properties properties) {
        super(YaokinMaterial.TIER, properties);
    }

    /** 弧刀的属性：伤害 -1、攻速 2.0、攻击范围 +1。 */
    public static ItemAttributeModifiers createBladeAttributes() {
        // 先取标准剑的属性（伤害 = 基础 + 材质加成），再覆盖为本物品的数值。
        ItemAttributeModifiers base = SwordItem.createAttributes(
                YaokinMaterial.TIER, 3.0F - DAMAGE_PENALTY, ATTACK_SPEED);
        return base.withModifierAdded(
                Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "gear_forge", "curved_blade_reach"),
                        REACH_BONUS,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            com.hana.gear_forge.event.ArcSlashHandler.onBladeHit(player, stack, target);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.curved_blade").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gear_forge.curved_blade.charge",
                        com.hana.gear_forge.component.ModDataComponents.arcSlashCharges(stack),
                        SLASH_EVERY)
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
