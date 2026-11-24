package com.hana.gear_forge.util;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.content.ModMobEffects;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 樱花木体系的公共判定：印记层数、樱花石宝石、四件套与护佑等级。
 *
 * <p>印记层数存在受击实体的 persistent data 上（换武器不丢、死亡自然清空），
 * 5 层触发一次樱花绽放。护佑等级取「套装提供的效果等级」与「护甲上樱花石提供的 1 级」
 * 的最大值，从而实现"不与套装叠加"。
 */
public final class SakuraHelper {

    /** 触发樱花绽放所需的印记层数。 */
    public static final int MARK_TRIGGER = 5;

    private static final String MARK_KEY = "gear_forge_sakura_mark";

    private SakuraHelper() {}

    public static int getMarks(LivingEntity entity) {
        return entity.getPersistentData().getInt(MARK_KEY);
    }

    /** 设置印记层数；<=0 时移除键，保持实体数据干净。 */
    public static void setMarks(LivingEntity entity, int value) {
        if (value <= 0) {
            entity.getPersistentData().remove(MARK_KEY);
        } else {
            entity.getPersistentData().putInt(MARK_KEY, value);
        }
    }

    public static boolean isSakuraSword(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.SAKURA_SWORD.get());
    }

    /** 物品上是否镶嵌了樱花石宝石。 */
    public static boolean hasSakuraStoneGem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        if (gems == null) return false;
        for (ModDataComponents.GemsEntry gem : gems) {
            if ("sakura_stone".equals(gem.id())) return true;
        }
        return false;
    }

    /** 该武器是否能打出樱花印记：樱花剑本身，或任意镶嵌樱花石的武器。 */
    public static boolean grantsMark(ItemStack weapon) {
        return isSakuraSword(weapon) || hasSakuraStoneGem(weapon);
    }

    public static boolean isSakuraArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ModItems.SAKURA_HELMET.get())
                || stack.is(ModItems.SAKURA_CHESTPLATE.get())
                || stack.is(ModItems.SAKURA_LEGGINGS.get())
                || stack.is(ModItems.SAKURA_BOOTS.get());
    }

    /** 是否穿齐四件樱花护甲。 */
    public static boolean hasFullSakuraArmor(Player player) {
        return isSakuraArmor(player.getItemBySlot(EquipmentSlot.HEAD))
                && isSakuraArmor(player.getItemBySlot(EquipmentSlot.CHEST))
                && isSakuraArmor(player.getItemBySlot(EquipmentSlot.LEGS))
                && isSakuraArmor(player.getItemBySlot(EquipmentSlot.FEET));
    }

    /**
     * 玩家当前的樱花之护佑等级。
     *
     * <p>套装提供 5 级（通过 {@code SAKURA_BLESSING} 状态效果），每件镶樱花石的护甲提供 1 级，
     * 取最大值而非相加——这就是"宝石不与樱花木套叠加"。
     */
    public static int blessingLevel(Player player) {
        int level = 0;
        MobEffectInstance effect = player.getEffect(ModMobEffects.SAKURA_BLESSING);
        if (effect != null) {
            level = effect.getAmplifier() + 1;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (hasSakuraStoneGem(player.getItemBySlot(slot))) {
                level = Math.max(level, 1);
            }
        }
        return level;
    }
}
