package com.hana.gear_forge.util;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.component.ModDataComponents.AffixEntry;
import com.hana.gear_forge.component.ModDataComponents.StoredBonus;
import com.hana.gear_forge.config.AffixConfig;
import com.hana.gear_forge.config.AffixConfig.AffixDefinition;
import com.hana.gear_forge.config.GemConfig;
import com.hana.gear_forge.config.GemConfig.GemDefinition;
import com.hana.gear_forge.config.GemConfig.GemEffect;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GearHelper {

    public static List<ModDataComponents.AffixEntry> rollAffixes(ItemStack stack, RandomSource random) {
        AffixConfig config = ModConfigs.getAffixConfig();
        if (config == null || config.getAffixPool() == null) return List.of();

        List<AffixDefinition> pool = config.getAffixPool().stream()
                .filter(java.util.Objects::nonNull)
                .filter(AffixDefinition::isEnabled)
                .filter(def -> def.getId() != null && def.getWeight() > 0)
                .toList();

        if (pool.isEmpty()) return List.of();

        int totalWeight = pool.stream().mapToInt(AffixDefinition::getWeight).sum();

        // 配置是用户可改的：min > max 或 min > 池子大小时，原实现会调用 nextInt(负数/0) 抛
        // IllegalArgumentException（怪物加入世界、重铸、调试命令三处都会踩到）。这里全部钳制。
        int maxCount = Math.min(Math.max(0, config.getMaxAffixCount()), pool.size());
        int minCount = Math.min(Math.max(0, config.getMinAffixCount()), maxCount);
        if (maxCount <= 0) return List.of();
        int count = minCount + random.nextInt(maxCount - minCount + 1);

        List<ModDataComponents.AffixEntry> result = new ArrayList<>();
        List<AffixDefinition> available = new ArrayList<>(pool);

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            AffixDefinition chosen = weightedRandom(available, totalWeight, random);
            if (chosen == null) break;

            double value = rollValue(chosen, random);
            result.add(new ModDataComponents.AffixEntry(chosen.getId(), value));

            available.remove(chosen);
            totalWeight -= chosen.getWeight();
        }

        return result;
    }

    private static AffixDefinition weightedRandom(List<AffixDefinition> pool, int totalWeight, RandomSource random) {
        if (pool.isEmpty() || totalWeight <= 0) return null;
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (AffixDefinition def : pool) {
            cumulative += def.getWeight();
            if (roll < cumulative) return def;
        }
        return pool.getLast();
    }

    private static double rollValue(AffixDefinition def, RandomSource random) {
        List<Double> range = def.getValueRange();
        if (range == null || range.size() < 2) return 0;
        double min = range.get(0);
        double max = range.get(1);
        return min + random.nextDouble() * (max - min);
    }

    public static double getAffixTotalValue(List<ModDataComponents.AffixEntry> affixes, String type) {
        if (affixes == null) return 0;
        double total = 0;
        for (ModDataComponents.AffixEntry entry : affixes) {
            if (entry == null) continue;
            var def = ModConfigs.getAffixDefinition(entry.id());
            if (def == null) continue;
            // type 既可能是词条分类（percentage / stat_boost），也可能是具体词条 id。
            String defType = def.getType() == null ? "" : def.getType();
            if (defType.equals(type) || entry.id().equals(type)) {
                total += entry.value();
            }
        }
        return total;
    }

    public static boolean hasEnoughItems(Player player, Item item, int count) {
        int found = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }

    public static void consumeItems(Player player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getNonEquipmentItems().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getNonEquipmentItems().get(i);
            if (stack.is(item)) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }

    public static double getUpgradedStatBonus(String statKey, int upgradeLevel) {
        if (upgradeLevel <= 0) return 0;
        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        double total = 0;
        if (config.getStatBoostPerLevel() != null) {
            Double perLevel = config.getStatBoostPerLevel().get(statKey);
            if (perLevel != null) total = perLevel * upgradeLevel;
        }
        if (config.getSpecialBonusLevels() != null) {
            for (int specialLevel : config.getSpecialBonusLevels()) {
                if (upgradeLevel >= specialLevel) {
                    total += config.getSpecialBonusStat(statKey, specialLevel);
                }
            }
        }
        return total;
    }

    /**
     * 把装备从 {@code currentLevel} 强化到下一级需要几个模板材料。
     *
     * <p>配置关掉材料消耗（{@code consumeMaterial} 为空）时返回 0，表示模板槽可以空着。
     */
    public static int getUpgradeConsumeCount(ItemStack stack, UpgradeConfig config, int currentLevel) {
        if (config == null || !config.consumesMaterial()) return 0;
        return config.getConsumeAmount(currentLevel + 1);
    }

    public static double getGemStatBonus(String gemId, String statType, int tier) {
        if (tier <= 0) tier = 1;
        var gem = ModConfigs.getGemDefinition(gemId);
        if (gem == null || gem.getEffects() == null) return 0;
        for (GemConfig.GemEffect effect : gem.getEffects()) {
            if (effect != null && statType.equals(effect.getType())) {
                return effect.getValue() * tier;
            }
        }
        return 0;
    }

    public static boolean hasUniqueGemEffect(String effectId, List<ModDataComponents.GemsEntry> gems) {
        if (gems == null) return false;
        for (ModDataComponents.GemsEntry entry : gems) {
            var gem = ModConfigs.getGemDefinition(entry.id());
            if (gem == null || gem.getEffects() == null) continue;
            for (GemConfig.GemEffect effect : gem.getEffects()) {
                if (effect == null || !"unique".equals(effect.getType())) continue;
                if (effectId.equals(gem.getId())) return true;
                String desc = effect.getDescription();
                if (desc != null && desc.contains(effectId)) return true;
            }
        }
        return false;
    }

    public static boolean hasGemEffectType(List<ModDataComponents.GemsEntry> gems, String effectType) {
        if (gems == null) return false;
        for (ModDataComponents.GemsEntry entry : gems) {
            var gem = ModConfigs.getGemDefinition(entry.id());
            if (gem == null || gem.getEffects() == null) continue;
            for (GemEffect effect : gem.getEffects()) {
                if (effect != null && effectType.equals(effect.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static double getAffixTotalValueDirect(List<ModDataComponents.AffixEntry> affixes, String affixId) {
        if (affixes == null) return 0;
        return affixes.stream()
                .filter(a -> a.id().equals(affixId))
                .mapToDouble(ModDataComponents.AffixEntry::value)
                .sum();
    }

    public static int getMaxSockets(ItemStack stack) {
        GemConfig config = ModConfigs.getGemConfig();
        return Math.max(0, config.getMaxSocketsPerGear());
    }

    public static String getSocketTypeForItem(ItemStack stack) {
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS)
                || stack.is(net.minecraft.tags.ItemTags.PICKAXES)
                || stack.is(net.minecraft.tags.ItemTags.AXES)
                || stack.is(net.minecraft.tags.ItemTags.SHOVELS)
                || stack.is(net.minecraft.tags.ItemTags.HOES)
                || stack.is(net.minecraft.world.item.Items.BOW)
                || stack.is(net.minecraft.world.item.Items.CROSSBOW)
                || stack.is(net.minecraft.world.item.Items.TRIDENT)
                || stack.is(net.minecraft.world.item.Items.SHIELD)) {
            return "weapon";
        }
        if (stack.has(net.minecraft.core.component.DataComponents.EQUIPPABLE)) {
            return "armor";
        }
        return "tool";
    }

    /**
     * 是否是“可以被锻造的装备”。
     *
     * <p>原实现把 {@code Items.STICK} 也算作装备（GF-014），导致木棍能被强化/洗词条/镶宝石。
     * 现在只保留“有耐久”这一条硬标准，外加几个原版里没有耐久但语义上是装备的特例。
     */
    public static boolean isGearItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isDamageableItem()
                || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT) || stack.is(Items.SHIELD);
    }

    /**
     * 一次强化掷骰的结果。由 {@code SmithingMenuMixin} 在真正取出时决定，
     * 不放在 {@code assemble} 里——否则预览阶段反复放取就能刷成功。
     */
    public enum UpgradeOutcome {
        /** 强化成功，等级 +1。 */
        SUCCESS,
        /** 强化失败但装备完好，等级不变。 */
        FAIL_KEEP,
        /** 强化失败且降级。 */
        FAIL_DOWNGRADE,
        /** 强化失败且装备损毁。 */
        DESTROYED
    }

    /**
     * 掷一次强化结果。
     *
     * <p>顺序：先判损毁（配置开启时），再判成功；失败时按 {@code getDowngradeChance()} 决定是否降级。
     * 损毁排在成功之前是有意的——否则高等级时"先成功后损毁"会让配置失去意义。
     */
    public static UpgradeOutcome rollUpgradeOutcome(UpgradeConfig config, int currentLevel, RandomSource random) {
        if (config == null) return UpgradeOutcome.SUCCESS;

        double destroyChance = config.getDestroyChanceSafe();
        if (destroyChance > 0 && random.nextDouble() < destroyChance) {
            return UpgradeOutcome.DESTROYED;
        }

        double successRate = config.getSuccessRate(currentLevel);
        if (random.nextDouble() < successRate) {
            return UpgradeOutcome.SUCCESS;
        }

        if (config.isAllowDowngrade() && config.getDowngradeLevelsSafe() > 0
                && random.nextDouble() < config.getDowngradeChance()) {
            return UpgradeOutcome.FAIL_DOWNGRADE;
        }
        return UpgradeOutcome.FAIL_KEEP;
    }

    /**
     * 掷骰并把结果写回玩家实际拿到的这一份产物。
     *
     * <p>由强化台菜单和原版锻造台的 mixin 共用，避免两处各写一份判定逻辑而逐渐走样。
     */
    public static void applyUpgradeOutcome(Player player, ItemStack result, int baseLevel, UpgradeConfig config) {
        switch (rollUpgradeOutcome(config, baseLevel, player.getRandom())) {
            case SUCCESS -> player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.ANVIL_USE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
            case FAIL_KEEP -> {
                setUpgradeLevel(result, baseLevel);
                notifyActionBar(player, "message.gear_forge.upgrade_fail");
            }
            case FAIL_DOWNGRADE -> {
                setUpgradeLevel(result, baseLevel - config.getDowngradeLevelsSafe());
                notifyActionBar(player, "message.gear_forge.upgrade_downgrade");
            }
            case DESTROYED -> {
                result.setCount(0);
                notifyActionBar(player, "message.gear_forge.upgrade_destroyed");
                player.level().playSound(null, player.blockPosition(),
                        net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        }
    }

    /** 写回强化等级，0 时直接移除组件而不是写 0。 */
    public static void setUpgradeLevel(ItemStack stack, int level) {
        if (level <= 0) {
            stack.remove(ModDataComponents.UPGRADE_LEVEL.get());
        } else {
            stack.set(ModDataComponents.UPGRADE_LEVEL.get(), level);
        }
    }

    /**
     * 从模板槽补扣材料，给原版留 1 个。
     *
     * <p>原版取出时会把三个输入槽各扣 1 个，所以这里只扣 {@code required - 1}。
     * 对非法配置（数量 &lt; 1、&gt; 64）直接放弃补扣而不是抛异常：配置写错只应导致材料消耗不准确。
     */
    public static void consumeTemplateExtra(ItemStack template, String materialId, int required) {
        if (required <= 1) return;
        int extra = Math.min(required, 64) - 1;
        if (extra <= 0) return;
        if (!com.hana.gear_forge.recipe.GearSmithingRecipe.isItemMatchingId(template, materialId)) return;

        int removable = Math.min(extra, template.getCount() - 1);
        if (removable > 0) {
            template.shrink(removable);
        }
    }

    /** 在 actionbar 上发一条提示。 */
    public static void notifyActionBar(Player player, String langKey) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(langKey), true);
        }
    }

    public static double getTotalGemEffectValue(List<ModDataComponents.GemsEntry> gems, String effectType) {
        if (gems == null) return 0;
        double total = 0;
        for (ModDataComponents.GemsEntry entry : gems) {
            var gem = ModConfigs.getGemDefinition(entry.id());
            if (gem == null || gem.getEffects() == null) continue;
            for (GemConfig.GemEffect effect : gem.getEffects()) {
                if (effect != null && effectType.equals(effect.getType())) {
                    total += effect.getValue() * entry.tier();
                }
            }
        }
        return total;
    }

    /**
     * 一个物品（弓弩或盔甲）上的 {@code bow_charge_speed} 总量。
     * 词条、宝石、转生奖励三处来源共用这一份实现，Mixin 与其它调用方不应各自复制逻辑。
     */
    public static double getBowChargeSpeed(ItemStack stack) {
        double total = 0;

        List<ModDataComponents.AffixEntry> affixes = stack.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            total += getAffixTotalValueDirect(affixes, "bow_charge_speed");
        }

        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            total += getTotalGemEffectValue(gems, "bow_charge_speed");
        }

        List<StoredBonus> stored = stack.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            total += getStoredBonusValue(stored, "bow_charge_speed");
        }

        return total;
    }

    /**
     * 只作用于护甲的耐久损耗减免系数，钳制在 [0.05, 1]。
     *
     * <p>翡翠的 {@code durability_boost} 每阶 +0.5，T2 起原实现算出负系数（等于护甲永不掉耐久），
     * 这里是所有耐久减免路径唯一的下限保护点。
     */
    public static float durabilityDamageFactor(ItemStack armor) {
        double factor = 1.0;

        List<ModDataComponents.GemsEntry> gems = armor.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            factor *= 1.0 - getTotalGemEffectValue(gems, "durability_boost") * 0.5;
        }

        List<ModDataComponents.AffixEntry> affixes = armor.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            factor *= 1.0 - getAffixTotalValueDirect(affixes, "durability_reduction");
        }

        List<StoredBonus> stored = armor.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            factor *= 1.0 - getStoredBonusValue(stored, "durability_reduction");
            factor *= 1.0 - getStoredBonusValue(stored, "durability_boost") * 0.5;
        }

        return (float) Math.max(0.05, Math.min(1.0, factor));
    }

    public static boolean isHoldingRangedWeapon(LivingEntity entity) {
        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
        return main.is(Items.BOW) || main.is(Items.CROSSBOW)
                || off.is(Items.BOW) || off.is(Items.CROSSBOW);
    }

    public static List<StoredBonus> calculateReincarnationBonuses(ItemStack stack) {
        List<StoredBonus> bonuses = new ArrayList<>();
        Map<String, Double> bonusMap = new HashMap<>();

        List<AffixEntry> affixes = stack.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            for (AffixEntry entry : affixes) {
                bonusMap.merge(entry.id(), entry.value(), Double::sum);
            }
        }

        int upgradeLevel = stack.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
        if (upgradeLevel > 0) {
            UpgradeConfig config = ModConfigs.getUpgradeConfig();
            if (config.getStatBoostPerLevel() != null) {
                for (var entry : config.getStatBoostPerLevel().entrySet()) {
                    double value = entry.getValue() * upgradeLevel;
                    if (value > 0) {
                        bonusMap.merge("upgrade_" + entry.getKey(), value, Double::sum);
                    }
                }
            }
            if (config.getSpecialBonusLevels() != null && config.getSpecialBonusStats() != null) {
                for (int specialLevel : config.getSpecialBonusLevels()) {
                    if (upgradeLevel >= specialLevel) {
                        Map<String, Double> levelBonuses = config.getSpecialBonusStats().get(specialLevel);
                        if (levelBonuses != null) {
                            for (var entry : levelBonuses.entrySet()) {
                                bonusMap.merge("upgrade_" + entry.getKey(), entry.getValue(), Double::sum);
                            }
                        }
                    }
                }
            }
        }

        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            GemConfig gemConfig = ModConfigs.getGemConfig();
            if (gemConfig.getGems() != null) {
                for (ModDataComponents.GemsEntry gem : gems) {
                    for (GemDefinition def : gemConfig.getGems()) {
                        if (def != null && def.getId() != null && def.getId().equals(gem.id())
                                && def.getEffects() != null) {
                            for (GemEffect effect : def.getEffects()) {
                                if (effect == null || effect.getType() == null) continue;
                                bonusMap.merge("gem_" + effect.getType(), effect.getValue() * gem.tier(), Double::sum);
                            }
                        }
                    }
                }
            }
        }

        for (var entry : bonusMap.entrySet()) {
            bonuses.add(new StoredBonus(entry.getKey(), entry.getValue()));
        }
        return bonuses;
    }

    public static double getStoredBonusValue(List<StoredBonus> bonuses, String id) {
        if (bonuses == null) return 0;
        double value = bonuses.stream()
                .filter(b -> b.id().equals(id))
                .mapToDouble(StoredBonus::value)
                .sum();
        double gemValue = bonuses.stream()
                .filter(b -> b.id().equals("gem_" + id))
                .mapToDouble(StoredBonus::value)
                .sum();
        return value + gemValue;
    }
}
