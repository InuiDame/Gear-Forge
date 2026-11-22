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
        // 单次最多消耗 maxConsumeMaterialPerLevel 个（默认 5）。模板槽上限 64，
        // 没有这个上限时高等级会要求几十个材料，玩家放不下也看不明白。
        int raw = consumeAmountBase + consumeAmountIncrement * Math.max(0, level - 1);
        int cap = maxConsumeMaterialPerLevel > 0 ? maxConsumeMaterialPerLevel : Integer.MAX_VALUE;
        return Math.max(1, Math.min(raw, cap));
    }

    public double getSuccessRate(int level) {
        return Math.max(0.0, Math.min(1.0, baseSuccessRate - levelSuccessDecay * (level - 1)));
    }

    /**
     * 失败时的降级概率。
     *
     * <p>语义：{@code levelFailureDecay} 既表示“失败会让后续强化更难的衰减量”，
     * 也用作“这次失败是否真的降级”的概率。这样 {@code allowDowngrade=true} 不等于必定掉级，
     * 配置可以做出“失败了但没掉级”的层次感。
     */
    public double getDowngradeChance() {
        return Math.max(0.0, Math.min(1.0, levelFailureDecay));
    }

    /** 失败时的损毁概率。{@code allowDestroy=false} 时恒为 0。 */
    public double getDestroyChanceSafe() {
        if (!allowDestroy) return 0.0;
        return Math.max(0.0, Math.min(1.0, destroyChance));
    }

    /** 是否消耗材料。材料 id 为空或数量恒为 0 时视为“强化不消耗材料”。 */
    public boolean consumesMaterial() {
        return consumeMaterial != null && !consumeMaterial.isBlank();
    }

    public int getDowngradeLevelsSafe() {
        return Math.max(0, downgradeLevels);
    }

    public int getMaxLevelSafe() {
        return Math.max(1, maxLevel);
    }

    public double getSpecialBonusStat(String statKey, int level) {
        if (specialBonusStats == null || specialBonusLevels == null
                || !specialBonusLevels.contains(level)) return 0;
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
