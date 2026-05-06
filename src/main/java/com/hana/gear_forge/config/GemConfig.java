package com.hana.gear_forge.config;

import java.util.List;

public class GemConfig {
    private int maxSocketsPerGear;
    private List<String> socketTypes;
    private List<GemDefinition> gems;
    private int maxTier;

    public GemConfig() {
    }

    public GemConfig(int maxSocketsPerGear, List<String> socketTypes, List<GemDefinition> gems, int maxTier) {
        this.maxSocketsPerGear = maxSocketsPerGear;
        this.socketTypes = socketTypes;
        this.gems = gems;
        this.maxTier = maxTier;
    }

    public int getMaxSocketsPerGear() { return maxSocketsPerGear; }
    public List<String> getSocketTypes() { return socketTypes; }
    public List<GemDefinition> getGems() { return gems; }
    public int getMaxTier() { return maxTier; }

    public static class GemDefinition {
        private String id;
        private String name;
        private int tier;
        private String itemId;
        private List<GemEffect> effects;
        private List<String> compatibleSockets;
        private String rarity;

        public GemDefinition() {
        }

        public GemDefinition(String id, String name, int tier, String itemId, List<GemEffect> effects,
                             List<String> compatibleSockets, String rarity) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.itemId = itemId;
            this.effects = effects;
            this.compatibleSockets = compatibleSockets;
            this.rarity = rarity;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getTier() { return tier; }
        public String getItemId() { return itemId; }
        public List<GemEffect> getEffects() { return effects; }
        public List<String> getCompatibleSockets() { return compatibleSockets; }
        public String getRarity() { return rarity; }
    }

    public static class GemEffect {
        private String type;
        private double value;
        private String description;

        public GemEffect() {
        }

        public GemEffect(String type, double value, String description) {
            this.type = type;
            this.value = value;
            this.description = description;
        }

        public String getType() { return type; }
        public double getValue() { return value; }
        public String getDescription() { return description; }
    }

    public static GemConfig createDefault() {
        return new GemConfig(
                3, List.of("weapon", "armor", "tool"),
                List.of(
                        new GemDefinition("ruby", "红宝石", 1, "gear_forge:ruby",
                                List.of(
                                        new GemEffect("attack_damage", 3.0, "增加3点攻击力"),
                                        new GemEffect("fire_damage", 2.0, "额外造成2点火焰伤害")
                                ),
                                List.of("weapon", "armor", "tool"), "uncommon"),
                        new GemDefinition("sapphire", "蓝宝石", 1, "gear_forge:sapphire",
                                List.of(
                                        new GemEffect("defense", 2.0, "增加2点防御力"),
                                        new GemEffect("frost_damage", 2.0, "额外造成2点寒冰伤害")
                                ),
                                List.of("weapon", "armor", "tool"), "uncommon"),
                        new GemDefinition("emerald", "绿宝石", 1, "gear_forge:emerald_gem",
                                List.of(
                                        new GemEffect("movement_speed", 0.05, "增加5%移动速度"),
                                        new GemEffect("looting", 1.0, "增加1级掠夺"),
                                        new GemEffect("enchant_compat", 0.0, "允许冲突附魔共存")
                                ),
                                List.of("weapon", "armor", "tool"), "uncommon"),
                        new GemDefinition("diamond_gem", "钻石", 2, "minecraft:diamond",
                                List.of(
                                        new GemEffect("all_stats", 1.0, "全属性+1")
                                ),
                                List.of("weapon", "armor", "tool"), "rare"),
                        new GemDefinition("nether_star_gem", "下界之星", 3, "minecraft:nether_star",
                                List.of(
                                        new GemEffect("unique", 0.0, "提供抗性提升效果")
                                ),
                                List.of("weapon", "armor", "tool"), "epic"),
                        new GemDefinition("topaz", "黄玉", 1, "gear_forge:topaz",
                                List.of(
                                        new GemEffect("bow_charge_speed", 0.3, "弓与弩蓄力速度+30%"),
                                        new GemEffect("projectile_damage", 3.0, "投射物伤害+3")
                                ),
                                List.of("weapon", "armor", "tool"), "uncommon"),
                        new GemDefinition("jade", "翡翠", 1, "gear_forge:jade",
                                List.of(
                                        new GemEffect("durability_boost", 0.5, "耐久度+50%")
                                ),
                                List.of("weapon", "armor", "tool"), "uncommon"),
                        new GemDefinition("onyx", "玛瑙", 1, "gear_forge:onyx",
                                List.of(
                                        new GemEffect("shield_block_recovery", 3.0, "减少格挡后受到的伤害+3")
                                ),
                                List.of("weapon"), "uncommon")
                ),
                5
        );
    }
}
