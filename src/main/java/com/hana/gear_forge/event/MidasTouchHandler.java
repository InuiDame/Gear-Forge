package com.hana.gear_forge.event;

import com.hana.gear_forge.content.ModMobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * 点金等级的来源管理。
 *
 * <p>点金有三个独立来源，等级需要相加：
 * <ul>
 *   <li>耀金四件套：常驻 I，穿齐就生效，脱下就消失；</li>
 *   <li>耀金苹果：II，限时；</li>
 *   <li>耀金硬币击杀：I，30 秒。</li>
 * </ul>
 * 直接给玩家反复 {@code addEffect} 无法处理"苹果的 II 到期后金币的 I 还在"这种情况，
 * 所以这里按来源记录到期时间，每次重算总和，再把可见的点金效果刷新成对应等级。
 */
public final class MidasTouchHandler {

    public enum Source {
        ARMOR,
        COIN,
        APPLE
    }

    private record Entry(int level, long expiresAt) {}

    private static final Map<UUID, EnumMap<Source, Entry>> SOURCES = new java.util.HashMap<>();

    private MidasTouchHandler() {}

    /** 添加/覆盖一个限时来源。durationTicks <= 0 时不生效。 */
    public static void add(ServerPlayer player, Source source, int level, int durationTicks) {
        if (level <= 0 || durationTicks <= 0) return;
        long expiresAt = player.level().getGameTime() + durationTicks;
        SOURCES.computeIfAbsent(player.getUUID(), k -> new EnumMap<>(Source.class))
                .merge(source, new Entry(level, expiresAt),
                        (oldEntry, newEntry) ->
                                new Entry(Math.max(oldEntry.level(), newEntry.level()),
                                        Math.max(oldEntry.expiresAt(), newEntry.expiresAt())));
        refresh(player);
    }

    /** 四件套的常驻来源：active=false 时移除。 */
    public static void setArmorSource(ServerPlayer player, boolean active) {
        EnumMap<Source, Entry> map = SOURCES.computeIfAbsent(player.getUUID(),
                k -> new EnumMap<>(Source.class));
        if (active) {
            map.put(Source.ARMOR, new Entry(1, Long.MAX_VALUE));
        } else {
            map.remove(Source.ARMOR);
        }
        refresh(player);
    }

    /** 当前有效点金等级（已过期来源不计入）。 */
    public static int currentLevel(ServerPlayer player) {
        EnumMap<Source, Entry> map = SOURCES.get(player.getUUID());
        if (map == null) return 0;
        long now = player.level().getGameTime();
        map.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
        return map.values().stream().mapToInt(Entry::level).sum();
    }

    /** 每 10 tick 调一次：清理过期来源并把可见效果刷成当前总等级。 */
    public static void tick(ServerPlayer player) {
        refresh(player);
    }

    private static void refresh(ServerPlayer player) {
        int total = currentLevel(player);
        if (total <= 0) {
            if (player.hasEffect(ModMobEffects.MIDAS_TOUCH)) {
                player.removeEffect(ModMobEffects.MIDAS_TOUCH);
            }
            SOURCES.remove(player.getUUID());
            return;
        }

        int amplifier = Math.min(total - 1, 9);
        MobEffectInstance current = player.getEffect(ModMobEffects.MIDAS_TOUCH);
        // 等级变化时必须先移除再添加：addEffect 对同效果的低等级不会降级。
        if (current == null || current.getAmplifier() != amplifier) {
            player.removeEffect(ModMobEffects.MIDAS_TOUCH);
            player.addEffect(new MobEffectInstance(
                    ModMobEffects.MIDAS_TOUCH, 60, amplifier, true, true, true));
        } else if (current.getDuration() < 30) {
            player.addEffect(new MobEffectInstance(
                    ModMobEffects.MIDAS_TOUCH, 60, amplifier, true, true, true));
        }
    }

    public static void clear(ServerPlayer player) {
        SOURCES.remove(player.getUUID());
    }
}
