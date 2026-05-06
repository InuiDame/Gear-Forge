package com.hana.gear_forge.config;

import java.util.List;
import java.util.Map;

public class AffixConfig {
    private int maxAffixCount;
    private int minAffixCount;
    private List<AffixDefinition> affixPool;
    private Map<String, List<Integer>> rarityAffixCount;
    private int affixRerollCostDiamonds;

    public AffixConfig() {
    }

    public AffixConfig(int maxAffixCount, int minAffixCount, List<AffixDefinition> affixPool,
                       Map<String, List<Integer>> rarityAffixCount, int affixRerollCostDiamonds) {
        this.maxAffixCount = maxAffixCount;
        this.minAffixCount = minAffixCount;
        this.affixPool = affixPool;
        this.rarityAffixCount = rarityAffixCount;
        this.affixRerollCostDiamonds = affixRerollCostDiamonds;
    }

    public int getMaxAffixCount() { return maxAffixCount; }
    public int getMinAffixCount() { return minAffixCount; }
    public List<AffixDefinition> getAffixPool() { return affixPool; }
    public Map<String, List<Integer>> getRarityAffixCount() { return rarityAffixCount; }
    public int getAffixRerollCostDiamonds() { return affixRerollCostDiamonds; }

    public static class AffixDefinition {
        private String id;
        private String name;
        private String type;
        private List<Double> valueRange;
        private int weight;
        private String description;

        public AffixDefinition() {
        }

        public AffixDefinition(String id, String name, String type, List<Double> valueRange, int weight, String description) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.valueRange = valueRange;
            this.weight = weight;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public List<Double> getValueRange() { return valueRange; }
        public int getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    public static AffixConfig createDefault() {
        return new AffixConfig(
                5, 1,
                List.of(
                        new AffixDefinition("attack_power", "攻击力", "stat_boost", List.of(1.0, 15.0), 20, "增加基础攻击伤害"),
                        new AffixDefinition("defense_power", "防御力", "stat_boost", List.of(1.0, 10.0), 20, "增加基础防御值"),
                        new AffixDefinition("crit_chance", "暴击率", "percentage", List.of(0.01, 0.15), 10, "增加暴击概率"),
                        new AffixDefinition("crit_damage", "暴击伤害", "percentage", List.of(0.05, 0.5), 10, "增加暴击伤害倍率"),
                        new AffixDefinition("attack_speed", "攻击速度", "stat_boost", List.of(0.05, 0.2), 8, "增加攻击速度"),
                        new AffixDefinition("life_steal", "生命偷取", "percentage", List.of(0.01, 0.08), 5, "攻击时恢复生命"),
                        new AffixDefinition("movement_speed", "移动速度", "percentage", List.of(0.02, 0.1), 7, "增加移动速度"),
                        new AffixDefinition("fire_damage", "火焰伤害", "stat_boost", List.of(1.0, 5.0), 8, "额外造成火焰伤害"),
                        new AffixDefinition("frost_damage", "寒冰伤害", "stat_boost", List.of(1.0, 5.0), 8, "额外造成寒冰伤害并减速敌人"),
                        new AffixDefinition("looting_bonus", "掠夺率", "percentage", List.of(0.05, 0.25), 5, "增加掉落物数量"),
                        new AffixDefinition("xp_bonus", "经验加成", "percentage", List.of(0.1, 0.5), 5, "增加经验掉落"),
                        new AffixDefinition("bow_charge_speed", "弓蓄力加速", "percentage", List.of(0.05, 0.3), 5, "增加弓箭蓄力速度"),
                        new AffixDefinition("projectile_damage", "弹射物伤害", "stat_boost", List.of(1.0, 8.0), 5, "增加弹射物伤害"),
                        new AffixDefinition("auto_repair", "自动修复", "stat_boost", List.of(1.0, 5.0), 3, "每秒自动修复耐久度"),
                        new AffixDefinition("durability_reduction", "耐久保护", "percentage", List.of(0.05, 0.3), 5, "减少耐久度消耗"),
                        new AffixDefinition("shield_block_reduction", "盾牌格挡", "stat_boost", List.of(1.0, 5.0), 5, "增加盾牌格挡减伤"),
                        new AffixDefinition("enchantment_boost", "附魔增幅", "stat_boost", List.of(1.0, 3.0), 4, "增加装备附魔等级")
                ),
                Map.of(
                        "common", List.of(1, 1),
                        "uncommon", List.of(1, 2),
                        "rare", List.of(2, 3),
                        "epic", List.of(3, 4),
                        "legendary", List.of(4, 5)
                ),
                3
        );
    }
}
