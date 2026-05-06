package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.MobGearConfig;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = GearForgeMod.MODID)
public class MobGearEventHandler {

    @SubscribeEvent
    public static void onMonsterSpawn(FinalizeSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;
        if (entity.level().isClientSide()) return;

        // 防止重复强化
        if (entity.getPersistentData().getBoolean("gear_forge_geared")) return;
        entity.getPersistentData().putBoolean("gear_forge_geared", true);

        MobGearConfig config = ModConfigs.getMobGearConfig();
        Difficulty difficulty = entity.level().getDifficulty();
        double chance = switch (difficulty) {
            case EASY -> config.getEasyChance();
            case NORMAL -> config.getNormalChance();
            case HARD -> config.getHardChance();
            default -> 0;
        };

        RandomSource random = entity.getRandom();
        if (random.nextDouble() >= chance) return;

        int upgradeLevel = random.nextInt(1, config.getMaxUpgradeLevel() + 1);
        int affixCount = random.nextInt(1, config.getMaxAffixes() + 1);

        // 遍历所有装备槽（主手、副手、护甲）
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR
                    && slot != EquipmentSlot.MAINHAND
                    && slot != EquipmentSlot.OFFHAND) continue;

            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            // 设置强化等级
            stack.set(ModDataComponents.UPGRADE_LEVEL.get(), upgradeLevel);

            // 生成词条，截断到最大数量
            List<ModDataComponents.AffixEntry> affixes = new ArrayList<>(
                    GearHelper.rollAffixes(stack, random)
            );
            if (affixes.size() > affixCount) {
                affixes = affixes.subList(0, affixCount);
            }
            stack.set(ModDataComponents.AFFIXES.get(), affixes);
        }
    }
}