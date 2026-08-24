package com.hana.gear_forge.config;

public class MobGearConfig {
    private int configVersion = 1;
    private double easyChance;
    private double normalChance;
    private double hardChance;
    private int maxUpgradeLevel;
    private int maxAffixes;

    public MobGearConfig() {
    }

    public MobGearConfig(int configVersion, double easyChance, double normalChance, double hardChance,
                         int maxUpgradeLevel, int maxAffixes) {
        this.configVersion = configVersion;
        this.easyChance = easyChance;
        this.normalChance = normalChance;
        this.hardChance = hardChance;
        this.maxUpgradeLevel = maxUpgradeLevel;
        this.maxAffixes = maxAffixes;
    }

    public int getConfigVersion() { return configVersion; }
    public double getEasyChance() { return easyChance; }
    public double getNormalChance() { return normalChance; }
    public double getHardChance() { return hardChance; }
    public int getMaxUpgradeLevel() { return maxUpgradeLevel; }
    public int getMaxAffixes() { return maxAffixes; }

    public static MobGearConfig createDefault() {
        return new MobGearConfig(
                1,
                0.001, 0.005, 0.01,
                10, 2
        );
    }
}