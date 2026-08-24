package com.hana.gear_forge.config;

import com.hana.gear_forge.GearForgeMod;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ModConfigs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static AffixConfig affixConfig;
    private static UpgradeConfig upgradeConfig;
    private static GemConfig gemConfig;
    private static MobGearConfig mobGearConfig;

    private static Map<String, AffixConfig.AffixDefinition> affixIndex;
    private static Map<String, GemConfig.GemDefinition> gemIndex;

    public static void init() {
        loadAffixConfig();
        loadUpgradeConfig();
        loadGemConfig();
        loadMobGearConfig();
        buildIndices();
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
                return defaultConfig;
            }

            String loadedStr = Files.readString(configPath);
            JsonObject loadedObj = JsonParser.parseString(loadedStr).getAsJsonObject();

            String defaultStr = GSON.toJson(defaultConfig);
            JsonObject defaultObj = JsonParser.parseString(defaultStr).getAsJsonObject();

            JsonObject merged = mergeJsonObjects(loadedObj, defaultObj);

            Path backupPath = configDir.resolve(fileName + ".old");
            try (Writer writer = Files.newBufferedWriter(backupPath)) {
                GSON.toJson(loadedObj, writer);
            }

            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(merged, writer);
            }

            return GSON.fromJson(merged, type);
        } catch (Exception e) {
            GearForgeMod.LOGGER.error("Failed to load config {}, using defaults", fileName, e);
            return defaultConfig;
        }
    }

    private static JsonObject mergeJsonObjects(JsonObject loaded, JsonObject defaults) {
        JsonObject result = new JsonObject();

        for (String key : loaded.keySet()) {
            JsonElement loadedElem = loaded.get(key);
            if (defaults.has(key)) {
                JsonElement defaultElem = defaults.get(key);
                result.add(key, mergeJsonElement(loadedElem, defaultElem));
            } else {
                result.add(key, loadedElem);
            }
        }

        for (String key : defaults.keySet()) {
            if (!result.has(key)) {
                result.add(key, defaults.get(key));
            }
        }

        return result;
    }

    private static JsonElement mergeJsonElement(JsonElement loaded, JsonElement defaultElem) {
        if (loaded.isJsonObject() && defaultElem.isJsonObject()) {
            return mergeJsonObjects(loaded.getAsJsonObject(), defaultElem.getAsJsonObject());
        }
        if (loaded.isJsonArray() && defaultElem.isJsonArray()) {
            return mergeJsonArrays(loaded.getAsJsonArray(), defaultElem.getAsJsonArray());
        }
        return loaded;
    }

    private static JsonArray mergeJsonArrays(JsonArray loaded, JsonArray defaults) {
        if (defaults.isEmpty()) return loaded;

        JsonElement firstDefault = defaults.get(0);
        if (!firstDefault.isJsonObject() || !firstDefault.getAsJsonObject().has("id")) {
            return defaults;
        }

        JsonArray result = new JsonArray();

        for (JsonElement defaultElem : defaults) {
            String id = defaultElem.getAsJsonObject().get("id").getAsString();
            JsonElement matched = findById(loaded, id);
            if (matched != null) {
                result.add(mergeJsonObjects(matched.getAsJsonObject(), defaultElem.getAsJsonObject()));
            } else {
                result.add(defaultElem);
            }
        }

        for (JsonElement loadedElem : loaded) {
            if (loadedElem.isJsonObject() && loadedElem.getAsJsonObject().has("id")) {
                String id = loadedElem.getAsJsonObject().get("id").getAsString();
                if (findById(result, id) == null) {
                    result.add(loadedElem);
                }
            }
        }

        return result;
    }

    private static JsonElement findById(JsonArray array, String id) {
        for (JsonElement elem : array) {
            if (elem.isJsonObject() && elem.getAsJsonObject().has("id")) {
                if (id.equals(elem.getAsJsonObject().get("id").getAsString())) {
                    return elem;
                }
            }
        }
        return null;
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

    private static void buildIndices() {
        affixIndex = new java.util.HashMap<>();
        if (affixConfig != null && affixConfig.getAffixPool() != null) {
            for (var def : affixConfig.getAffixPool()) {
                affixIndex.put(def.getId(), def);
            }
        }
        gemIndex = new java.util.HashMap<>();
        if (gemConfig != null && gemConfig.getGems() != null) {
            for (var def : gemConfig.getGems()) {
                gemIndex.put(def.getId(), def);
                if (def.getItemId() != null) {
                    gemIndex.put(def.getItemId(), def);
                }
            }
        }
    }

    public static AffixConfig.AffixDefinition getAffixDefinition(String id) {
        return affixIndex != null ? affixIndex.get(id) : null;
    }

    public static GemConfig.GemDefinition getGemDefinition(String id) {
        return gemIndex != null ? gemIndex.get(id) : null;
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
