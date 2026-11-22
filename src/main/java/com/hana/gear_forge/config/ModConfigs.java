package com.hana.gear_forge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.hana.gear_forge.GearForgeMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 四份 JSON 平衡配置的加载器。
 *
 * <p>三个必须守住的约定：
 * <ol>
 *   <li><b>用户值优先</b>。默认值只用来“补全缺失字段”，任何用户已经写过的值——包括数组——都不覆盖。
 *       旧实现里 {@code mergeJsonArrays} 在数组元素没有 {@code id} 时直接返回默认值，
 *       等于每次启动都把 {@code valueRange} / {@code effects} 重置掉（GF-003）。</li>
 *   <li><b>只在有变化时写回</b>。旧实现在每次启动都无条件重写主文件和 {@code .old} 备份，
 *       只读目录下会静默回退默认值。</li>
 *   <li><b>可被服务端覆盖</b>。专用服务器与客户端各读自己的文件会导致“提示里的数值”和
 *       “实际结算的数值”不一致（GF-007），因此提供了 {@link #applyServerSnapshot} 入口，
 *       由网络层在登录时下发服务端权威快照。客户端只覆盖内存，不写回自己的文件。</li>
 * </ol>
 */
public final class ModConfigs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String AFFIX_FILE = "affixes.json";
    private static final String UPGRADE_FILE = "upgrades.json";
    private static final String GEM_FILE = "gems.json";
    private static final String MOB_GEAR_FILE = "mob_gear.json";

    /**
     * 读多写少：配置在启动、登录同步、/reload 时被整体替换，而每 tick 的伤害结算与 tooltip 都在读。
     * 用读写锁而不是 volatile 是因为索引表也是成组替换的，必须一起可见。
     */
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    private static AffixConfig affixConfig;
    private static UpgradeConfig upgradeConfig;
    private static GemConfig gemConfig;
    private static MobGearConfig mobGearConfig;

    private static Map<String, AffixConfig.AffixDefinition> affixIndex = Map.of();
    private static Map<String, GemConfig.GemDefinition> gemIndex = Map.of();

    private ModConfigs() {}

    // ===================== 生命周期 =====================

    /** 从本地 {@code config/gear_forge/*.json} 载入（客户端与服务端启动时各自调用一次）。 */
    public static void init() {
        LOCK.writeLock().lock();
        try {
            affixConfig = load(AFFIX_FILE, new TypeToken<AffixConfig>() {}.getType(), AffixConfig.createDefault());
            upgradeConfig = load(UPGRADE_FILE, new TypeToken<UpgradeConfig>() {}.getType(), UpgradeConfig.createDefault());
            gemConfig = load(GEM_FILE, new TypeToken<GemConfig>() {}.getType(), GemConfig.createDefault());
            mobGearConfig = load(MOB_GEAR_FILE, new TypeToken<MobGearConfig>() {}.getType(), MobGearConfig.createDefault());
            rebuildIndices();
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /** 重新读取本地文件并重建索引（{@code /reload} 或断线回到单人游戏时使用）。 */
    public static void reload() {
        init();
    }

    /**
     * 用服务端下发的权威快照覆盖内存中的配置，<b>不写回本地文件</b>。
     *
     * <p>任何一段解析失败都保留原有值，绝不把配置清空——配置错误只应影响数值，不应让整个模组失效。
     *
     * @param affixesJson  affixes.json 的 JSON 文本，null 或空串表示该段不更新
     * @param upgradesJson upgrades.json 的 JSON 文本
     * @param gemsJson     gems.json 的 JSON 文本
     * @param mobGearJson  mob_gear.json 的 JSON 文本
     */
    public static void applyServerSnapshot(String affixesJson, String upgradesJson,
                                           String gemsJson, String mobGearJson) {
        LOCK.writeLock().lock();
        try {
            AffixConfig newAffix = parseOrNull(affixesJson, new TypeToken<AffixConfig>() {}.getType());
            UpgradeConfig newUpgrade = parseOrNull(upgradesJson, new TypeToken<UpgradeConfig>() {}.getType());
            GemConfig newGem = parseOrNull(gemsJson, new TypeToken<GemConfig>() {}.getType());
            MobGearConfig newMobGear = parseOrNull(mobGearJson, new TypeToken<MobGearConfig>() {}.getType());

            if (newAffix != null) affixConfig = newAffix;
            if (newUpgrade != null) upgradeConfig = newUpgrade;
            if (newGem != null) gemConfig = newGem;
            if (newMobGear != null) mobGearConfig = newMobGear;
            rebuildIndices();
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /** 服务端把当前内存配置序列化成可下发的 JSON 快照。 */
    public static String[] toSnapshot() {
        LOCK.readLock().lock();
        try {
            return new String[] {
                    GSON.toJson(affixConfig),
                    GSON.toJson(upgradeConfig),
                    GSON.toJson(gemConfig),
                    GSON.toJson(mobGearConfig)
            };
        } finally {
            LOCK.readLock().unlock();
        }
    }

    // ===================== 读取器 =====================

    public static AffixConfig getAffixConfig() {
        LOCK.readLock().lock();
        try {
            return affixConfig != null ? affixConfig : AffixConfig.createDefault();
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static UpgradeConfig getUpgradeConfig() {
        LOCK.readLock().lock();
        try {
            return upgradeConfig != null ? upgradeConfig : UpgradeConfig.createDefault();
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static GemConfig getGemConfig() {
        LOCK.readLock().lock();
        try {
            return gemConfig != null ? gemConfig : GemConfig.createDefault();
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static MobGearConfig getMobGearConfig() {
        LOCK.readLock().lock();
        try {
            return mobGearConfig != null ? mobGearConfig : MobGearConfig.createDefault();
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static AffixConfig.AffixDefinition getAffixDefinition(String id) {
        if (id == null) return null;
        LOCK.readLock().lock();
        try {
            return affixIndex.get(id);
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static GemConfig.GemDefinition getGemDefinition(String id) {
        if (id == null) return null;
        LOCK.readLock().lock();
        try {
            return gemIndex.get(id);
        } finally {
            LOCK.readLock().unlock();
        }
    }

    // ===================== 加载 / 合并 / 索引 =====================

    private static <T> T load(String fileName, Type type, T defaults) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(GearForgeMod.MODID);
        Path configPath = configDir.resolve(fileName);

        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configDir);
                Files.writeString(configPath, GSON.toJson(defaults));
                return defaults;
            }

            JsonObject userObj = readObject(configPath);
            if (userObj == null) return defaults;

            JsonObject merged = merge(userObj, readObject(GSON.toJson(defaults)));
            writeBackIfChanged(configPath, userObj, merged);
            return GSON.fromJson(merged, type);
        } catch (Exception e) {
            GearForgeMod.LOGGER.error("Failed to load config {}, using defaults", fileName, e);
            return defaults;
        }
    }

    private static JsonObject readObject(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            GearForgeMod.LOGGER.error("Failed to parse {}", path, e);
            return null;
        }
    }

    private static JsonObject readObject(String json) {
        JsonElement element = JsonParser.parseString(json);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static <T> T parseOrNull(String json, Type type) {
        if (json == null || json.isBlank()) return null;
        try {
            return GSON.fromJson(json, type);
        } catch (RuntimeException e) {
            GearForgeMod.LOGGER.error("Rejected malformed config snapshot from server", e);
            return null;
        }
    }

    /**
     * 合并策略：以默认值骨架补全用户文件缺失的键，但<b>用户已有的值一律保留</b>。
     *
     * <p>与旧实现的关键差别在数组：旧实现在元素没有 {@code id} 时直接丢弃用户内容（GF-003）。
     * 新实现只在“用户数组比默认数组短且元素带 id”的典型列表配置里按 id 对齐，
     * 其余情况一律保留用户数组。
     */
    private static JsonObject merge(JsonObject user, JsonObject defaults) {
        JsonObject result = new JsonObject();

        for (String key : user.keySet()) {
            JsonElement userValue = user.get(key);
            if (defaults != null && defaults.has(key)) {
                result.add(key, mergeValue(userValue, defaults.get(key)));
            } else {
                result.add(key, userValue);
            }
        }

        if (defaults != null) {
            for (String key : defaults.keySet()) {
                if (!result.has(key)) {
                    result.add(key, defaults.get(key));
                }
            }
        }

        return result;
    }

    private static JsonElement mergeValue(JsonElement user, JsonElement defaults) {
        if (user.isJsonObject() && defaults.isJsonObject()) {
            return merge(user.getAsJsonObject(), defaults.getAsJsonObject());
        }
        if (user.isJsonArray() && defaults.isJsonArray()) {
            return mergeArray(user.getAsJsonArray(), defaults.getAsJsonArray());
        }
        // 标量：用户值优先。
        return user;
    }

    /**
     * 数组合并。
     *
     * <p>只有“默认数组的每个元素都是带 {@code id} 的对象”——即词条池、宝石列表这类具名集合——
     * 才做按 id 对齐的补全（用户已定义的同 id 元素整体保留，含它自己的子数组）；
     * 其它数组（{@code valueRange}、{@code effects}、{@code socketTypes}、{@code specialBonusLevels} …）
     * 一律视为用户数据，直接保留，绝不用默认值替换。
     */
    private static JsonArray mergeArray(JsonArray user, JsonArray defaults) {
        if (!isIdKeyedArray(defaults)) return user;
        if (!isIdKeyedArray(user)) return user;

        JsonArray result = new JsonArray();
        for (JsonElement elem : user) {
            result.add(elem);
        }
        for (JsonElement defElem : defaults) {
            String id = defElem.getAsJsonObject().get("id").getAsString();
            if (findById(result, id) == null) {
                result.add(defElem);
            }
        }
        return result;
    }

    private static boolean isIdKeyedArray(JsonArray array) {
        if (array.isEmpty()) return false;
        for (JsonElement elem : array) {
            if (!elem.isJsonObject() || !elem.getAsJsonObject().has("id")) return false;
        }
        return true;
    }

    private static JsonElement findById(JsonArray array, String id) {
        for (JsonElement elem : array) {
            if (elem.isJsonObject() && elem.getAsJsonObject().has("id")
                    && id.equals(elem.getAsJsonObject().get("id").getAsString())) {
                return elem;
            }
        }
        return null;
    }

    /**
     * 只在补全真的改动了内容时才写回，并把改动前的原文备份成 {@code .old}。
     * 用户文件已经完整（最常见的稳态）时完全不碰磁盘。
     */
    private static void writeBackIfChanged(Path configPath, JsonObject original, JsonObject merged) {
        if (merged.equals(original)) return;
        try {
            Path backup = configPath.resolveSibling(configPath.getFileName() + ".old");
            Files.copy(configPath, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(merged, writer);
            }
        } catch (IOException e) {
            // 只读目录/权限不足时不应让模组失效：内存里已经是合并后的值，继续用即可。
            GearForgeMod.LOGGER.warn("Could not write back merged config {} (read-only?)", configPath, e);
        }
    }

    private static void rebuildIndices() {
        Map<String, AffixConfig.AffixDefinition> affixes = new HashMap<>();
        if (affixConfig != null && affixConfig.getAffixPool() != null) {
            for (var def : affixConfig.getAffixPool()) {
                if (def != null && def.getId() != null) {
                    affixes.put(def.getId(), def);
                }
            }
        }
        affixIndex = Map.copyOf(affixes);

        Map<String, GemConfig.GemDefinition> gems = new HashMap<>();
        if (gemConfig != null && gemConfig.getGems() != null) {
            for (var def : gemConfig.getGems()) {
                if (def == null || def.getId() == null) continue;
                gems.put(def.getId(), def);
                if (def.getItemId() != null) {
                    gems.put(def.getItemId(), def);
                }
            }
        }
        gemIndex = Map.copyOf(gems);
    }
}
