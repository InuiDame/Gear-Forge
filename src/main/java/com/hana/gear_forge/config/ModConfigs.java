package com.hana.gear_forge.config;

import com.hana.gear_forge.GearForgeMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfigs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static AffixConfig affixConfig;
    private static UpgradeConfig upgradeConfig;
    private static GemConfig gemConfig;
    private static MobGearConfig mobGearConfig;


    public static void init() {
        loadAffixConfig();
        loadUpgradeConfig();
        loadGemConfig();
        loadMobGearConfig();
    }

    public static void reload() {
        init();
    }

    private static <T> T loadJsonConfig(String fileName, Type type, T defaultConfig) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(GearForgeMod.MODID);
        Path configPath = configDir.resolve(fileName);

        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configDir);
                String json = GSON.toJson(defaultConfig);
                Files.writeString(configPath, json);
            }
            try (Reader reader = Files.newBufferedReader(configPath)) {
                return GSON.fromJson(reader, type);
            }
        } catch (IOException e) {
            return defaultConfig;
        }
    }

    private static void loadAffixConfig() {
        Type type = new TypeToken<AffixConfig>() {}.getType();
        affixConfig = loadJsonConfig("affixes.json", type, AffixConfig.createDefault());
    }

    private static void loadUpgradeConfig() {
        Type type = new TypeToken<UpgradeConfig>() {}.getType();
        upgradeConfig = loadJsonConfig("upgrades.json", type, UpgradeConfig.createDefault());
    }

    private static void loadGemConfig() {
        Type type = new TypeToken<GemConfig>() {}.getType();
        gemConfig = loadJsonConfig("gems.json", type, GemConfig.createDefault());
    }

    private static void loadMobGearConfig() {
        Type type = new TypeToken<MobGearConfig>() {}.getType();
        mobGearConfig = loadJsonConfig("mob_gear.json", type, MobGearConfig.createDefault());
    }

    public static AffixConfig getAffixConfig() {
        return affixConfig;
    }

    public static UpgradeConfig getUpgradeConfig() {
        return upgradeConfig;
    }

    public static GemConfig getGemConfig() {
        return gemConfig;
    }

    public static MobGearConfig getMobGearConfig() { return mobGearConfig; }


}
