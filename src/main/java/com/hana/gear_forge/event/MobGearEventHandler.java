package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.MobGearConfig;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.List;

@EventBusSubscriber(modid = GearForgeMod.MODID)
public class MobGearEventHandler {

    public static final String GEARED_TAG = "gear_forge_geared";

    /*
     * 不能用 FinalizeSpawnEvent：它在 Mob#finalizeSpawn 之前触发，此时僵尸/骷髅还没拿到默认装备，
     * 装备槽全是空的。实体加入世界时装备已经生成好了。
     */
    @SubscribeEvent
    public static void onMonsterJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof Monster entity)) return;

        // 防止重复强化（例如跨维度传送时会再次加入世界）
        if (entity.getPersistentData().getBooleanOr(GEARED_TAG, false)) return;

        MobGearConfig config = ModConfigs.getMobGearConfig();
        Difficulty difficulty = event.getLevel().getDifficulty();
        double chance = switch (difficulty) {
            case EASY -> config.getEasyChance();
            case NORMAL -> config.getNormalChance();
            case HARD -> config.getHardChance();
            default -> 0;
        };

        RandomSource random = entity.getRandom();
        if (random.nextDouble() >= chance) return;

        // 只在“真的要给装备”时才写持久 NBT。
        // 旧实现在概率判定之前就打标，导致每只怪物（包括没被强化的）都会往存档里写一条
        // ForgeData，长期游玩会让实体 NBT 无谓膨胀。
        entity.getPersistentData().putBoolean(GEARED_TAG, true);

        int upgradeLevel = 1 + random.nextInt(Math.max(1, config.getMaxUpgradeLevel()));
        int affixCount = 1 + random.nextInt(Math.max(1, config.getMaxAffixes()));

        // 遍历主手、副手和四件护甲
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR
                    && slot != EquipmentSlot.MAINHAND
                    && slot != EquipmentSlot.OFFHAND) continue;

            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !GearHelper.isGearItem(stack)) continue;

            stack.set(ModDataComponents.UPGRADE_LEVEL.get(), upgradeLevel);

            // 生成词条，截断到最大数量（组件值必须是不可变列表）
            List<ModDataComponents.AffixEntry> affixes = GearHelper.rollAffixes(stack, random);
            if (affixes.size() > affixCount) {
                affixes = affixes.subList(0, affixCount);
            }
            stack.set(ModDataComponents.AFFIXES.get(), List.copyOf(affixes));
        }
    }
}
