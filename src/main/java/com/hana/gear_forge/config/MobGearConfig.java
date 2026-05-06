package com.hana.gear_forge.config;

public class MobGearConfig {
    private double easyChance;
    private double normalChance;
    private double hardChance;
    private int maxUpgradeLevel;
    private int maxAffixes;

    public MobGearConfig() {
    }

    public MobGearConfig(double easyChance, double normalChance, double hardChance,
                         int maxUpgradeLevel, int maxAffixes) {
        this.easyChance = easyChance;
        this.normalChance = normalChance;
        this.hardChance = hardChance;
        this.maxUpgradeLevel = maxUpgradeLevel;
        this.maxAffixes = maxAffixes;
    }

    public double getEasyChance() { return easyChance; }
    public double getNormalChance() { return normalChance; }
    public double getHardChance() { return hardChance; }
    public int getMaxUpgradeLevel() { return maxUpgradeLevel; }
    public int getMaxAffixes() { return maxAffixes; }

    public static MobGearConfig createDefault() {
        return new MobGearConfig(
                0.001,   // 简单
                0.005,   // 普通
                0.01,    // 困难
                10,      // 最高强化+10
                2        // 最多2条词条
        );
    }
}