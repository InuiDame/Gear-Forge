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
        List<AffixDefinition> pool = config.getAffixPool();

        int totalWeight = pool.stream().mapToInt(AffixDefinition::getWeight).sum();

        int minCount = config.getMinAffixCount();
        int maxCount = Math.min(config.getMaxAffixCount(), pool.size());
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
        return affixes.stream()
                .filter(a -> isAffixOfType(a.id(), type))
                .mapToDouble(ModDataComponents.AffixEntry::value)
                .sum();
    }

    private static boolean isAffixOfType(String affixId, String statType) {
        AffixConfig config = ModConfigs.getAffixConfig();
        return config.getAffixPool().stream()
                .filter(a -> a.getId().equals(affixId))
                .anyMatch(a -> a.getType().equals(statType) || a.getId().equals(statType));
    }

    public static boolean hasEnoughItems(Player player, Item item, int count) {
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }

    public static void consumeItems(Player player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
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
        Double perLevel = config.getStatBoostPerLevel().get(statKey);
        double total = 0;
        if (perLevel != null) total = perLevel * upgradeLevel;
        for (int specialLevel : config.getSpecialBonusLevels()) {
            if (upgradeLevel >= specialLevel) {
                total += config.getSpecialBonusStat(statKey, specialLevel);
            }
        }
        return total;
    }

    public static double getGemStatBonus(String gemId, String statType, int tier) {
        if (tier <= 0) tier = 1;
        GemConfig config = ModConfigs.getGemConfig();
        for (GemDefinition gem : config.getGems()) {
            if (gem.getId().equals(gemId) && gem.getEffects() != null) {
                for (GemEffect effect : gem.getEffects()) {
                    if (effect.getType().equals(statType)) {
                        return effect.getValue() * tier;
                    }
                }
            }
        }
        return 0;
    }

    public static boolean hasUniqueGemEffect(String effectId, List<ModDataComponents.GemsEntry> gems) {
        if (gems == null) return false;
        GemConfig config = ModConfigs.getGemConfig();
        for (ModDataComponents.GemsEntry entry : gems) {
            for (GemDefinition gem : config.getGems()) {
                if (gem.getId().equals(entry.id()) && gem.getEffects() != null) {
                    for (GemEffect effect : gem.getEffects()) {
                        if (effect.getType().equals("unique")
                                && (effect.getDescription().contains(effectId) || gem.getId().contains(effectId))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasGemEffectType(List<ModDataComponents.GemsEntry> gems, String effectType) {
        if (gems == null) return false;
        GemConfig config = ModConfigs.getGemConfig();
        for (ModDataComponents.GemsEntry entry : gems) {
            for (GemDefinition gem : config.getGems()) {
                if (gem.getId().equals(entry.id()) && gem.getEffects() != null) {
                    for (GemEffect effect : gem.getEffects()) {
                        if (effect.getType().equals(effectType)) {
                            return true;
                        }
                    }
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
        return config.getMaxSocketsPerGear();
    }

    public static String getSocketTypeForItem(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.SwordItem
                || stack.getItem() instanceof net.minecraft.world.item.DiggerItem
                || stack.is(net.minecraft.world.item.Items.BOW)
                || stack.is(net.minecraft.world.item.Items.CROSSBOW)
                || stack.is(net.minecraft.world.item.Items.TRIDENT)
                || stack.is(net.minecraft.world.item.Items.SHIELD)) {
            return "weapon";
        }
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
            return "armor";
        }
        return "tool";
    }

    public static boolean isGearItem(ItemStack stack) {
        return stack.isDamageableItem() || stack.is(Items.STICK)
                || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT) || stack.is(Items.SHIELD)
                || stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                || stack.getItem() instanceof net.minecraft.world.item.SwordItem
                || stack.getItem() instanceof net.minecraft.world.item.DiggerItem;
    }

    public static double getTotalGemEffectValue(List<ModDataComponents.GemsEntry> gems, String effectType) {
        if (gems == null) return 0;
        double total = 0;
        GemConfig config = ModConfigs.getGemConfig();
        for (ModDataComponents.GemsEntry entry : gems) {
            for (GemDefinition gem : config.getGems()) {
                if (gem.getId().equals(entry.id()) && gem.getEffects() != null) {
                    for (GemEffect effect : gem.getEffects()) {
                        if (effect.getType().equals(effectType)) {
                            total += effect.getValue() * entry.tier();
                        }
                    }
                }
            }
        }
        return total;
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
            for (var entry : config.getStatBoostPerLevel().entrySet()) {
                double value = entry.getValue() * upgradeLevel;
                if (value > 0) {
                    bonusMap.merge("upgrade_" + entry.getKey(), value, Double::sum);
                }
            }
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

        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            GemConfig gemConfig = ModConfigs.getGemConfig();
            for (ModDataComponents.GemsEntry gem : gems) {
                for (GemDefinition def : gemConfig.getGems()) {
                    if (def.getId().equals(gem.id()) && def.getEffects() != null) {
                        for (GemEffect effect : def.getEffects()) {
                            bonusMap.merge(effect.getType(), effect.getValue() * gem.tier(), Double::sum);
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
        return bonuses.stream()
                .filter(b -> b.id().equals(id))
                .mapToDouble(StoredBonus::value)
                .sum();
    }
}
