package com.hana.gear_forge.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeConfig {
    private int configVersion = 1;
    private int maxLevel;
    private double baseSuccessRate;
    private double levelSuccessDecay;
    private double levelFailureDecay;
    private int maxConsumeMaterialPerLevel;
    private String consumeMaterial;
    private int consumeAmountBase;
    private int consumeAmountIncrement;
    private boolean allowDowngrade;
    private int downgradeLevels;
    private boolean allowDestroy;
    private double destroyChance;
    private Map<String, Double> statBoostPerLevel;
    private List<Integer> specialBonusLevels;
    private Map<Integer, Map<String, Double>> specialBonusStats;
    private boolean reincarnationEnabled;
    private String reincarnationCostMaterial;
    private int reincarnationCostMaterialCount;
    private int maxReincarnationCount;

    public UpgradeConfig() {
    }

    public UpgradeConfig(int configVersion, int maxLevel, double baseSuccessRate, double levelSuccessDecay,
                         double levelFailureDecay, int maxConsumeMaterialPerLevel,
                         String consumeMaterial, int consumeAmountBase, int consumeAmountIncrement,
                         boolean allowDowngrade, int downgradeLevels, boolean allowDestroy,
                         double destroyChance, Map<String, Double> statBoostPerLevel,
                         List<Integer> specialBonusLevels,
                         Map<Integer, Map<String, Double>> specialBonusStats,
                         boolean reincarnationEnabled, String reincarnationCostMaterial,
                         int reincarnationCostMaterialCount, int maxReincarnationCount) {
        this.configVersion = configVersion;
        this.maxLevel = maxLevel;
        this.baseSuccessRate = baseSuccessRate;
        this.levelSuccessDecay = levelSuccessDecay;
        this.levelFailureDecay = levelFailureDecay;
        this.maxConsumeMaterialPerLevel = maxConsumeMaterialPerLevel;
        this.consumeMaterial = consumeMaterial;
        this.consumeAmountBase = consumeAmountBase;
        this.consumeAmountIncrement = consumeAmountIncrement;
        this.allowDowngrade = allowDowngrade;
        this.downgradeLevels = downgradeLevels;
        this.allowDestroy = allowDestroy;
        this.destroyChance = destroyChance;
        this.statBoostPerLevel = statBoostPerLevel;
        this.specialBonusLevels = specialBonusLevels;
        this.specialBonusStats = specialBonusStats;
        this.reincarnationEnabled = reincarnationEnabled;
        this.reincarnationCostMaterial = reincarnationCostMaterial;
        this.reincarnationCostMaterialCount = reincarnationCostMaterialCount;
        this.maxReincarnationCount = maxReincarnationCount;
    }

    public int getConfigVersion() { return configVersion; }
    public int getMaxLevel() { return maxLevel; }
    public double getBaseSuccessRate() { return baseSuccessRate; }
    public double getLevelSuccessDecay() { return levelSuccessDecay; }
    public double getLevelFailureDecay() { return levelFailureDecay; }
    public int getMaxConsumeMaterialPerLevel() { return maxConsumeMaterialPerLevel; }
    public String getConsumeMaterial() { return consumeMaterial; }
    public int getConsumeAmountBase() { return consumeAmountBase; }
    public int getConsumeAmountIncrement() { return consumeAmountIncrement; }
    public boolean isAllowDowngrade() { return allowDowngrade; }
    public int getDowngradeLevels() { return downgradeLevels; }
    public boolean isAllowDestroy() { return allowDestroy; }
    public double getDestroyChance() { return destroyChance; }
    public Map<String, Double> getStatBoostPerLevel() { return statBoostPerLevel; }
    public List<Integer> getSpecialBonusLevels() { return specialBonusLevels; }
    public Map<Integer, Map<String, Double>> getSpecialBonusStats() { return specialBonusStats; }
    public boolean isReincarnationEnabled() { return reincarnationEnabled; }
    public String getReincarnationCostMaterial() { return reincarnationCostMaterial; }
    public int getReincarnationCostMaterialCount() { return reincarnationCostMaterialCount; }
    public int getMaxReincarnationCount() { return maxReincarnationCount; }

    public int getConsumeAmount(int level) {
        return consumeAmountBase + consumeAmountIncrement * Math.max(0, level - 1);
    }

    public double getSuccessRate(int level) {
        return Math.max(0, baseSuccessRate - levelSuccessDecay * (level - 1));
    }

    public double getSpecialBonusStat(String statKey, int level) {
        if (specialBonusStats == null || !specialBonusLevels.contains(level)) return 0;
        Map<String, Double> levelBonuses = specialBonusStats.get(level);
        if (levelBonuses == null) return 0;
        return levelBonuses.getOrDefault(statKey, 0.0);
    }

    public static UpgradeConfig createDefault() {
        Map<Integer, Map<String, Double>> defaultSpecialStats = new HashMap<>();
        defaultSpecialStats.put(5, Map.of("attack_damage", 2.0, "defense", 1.0, "toughness", 0.5));
        defaultSpecialStats.put(10, Map.of("attack_damage", 5.0, "defense", 3.0, "toughness", 1.5));
        defaultSpecialStats.put(15, Map.of("attack_damage", 10.0, "defense", 5.0, "toughness", 3.0));

        return new UpgradeConfig(
                1,
                15, 1.0, 0.05, 0.1,
                5, "minecraft:diamond", 1, 1,
                true, 1, false, 0.0,
                Map.of("attack_damage", 0.5, "defense", 0.3, "toughness", 0.1),
                List.of(5, 10, 15),
                defaultSpecialStats,
                true, "minecraft:netherite_block", 8,
                -1
        );
    }
}
