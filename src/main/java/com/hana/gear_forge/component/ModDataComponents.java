package com.hana.gear_forge.component;

import com.hana.gear_forge.GearForgeMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GearForgeMod.MODID);

    public record AffixEntry(String id, double value) {
        public static final Codec<AffixEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("id").forGetter(AffixEntry::id),
                        Codec.DOUBLE.fieldOf("value").forGetter(AffixEntry::value)
                ).apply(instance, AffixEntry::new));

        public static final StreamCodec<ByteBuf, AffixEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AffixEntry::id,
                ByteBufCodecs.DOUBLE, AffixEntry::value,
                AffixEntry::new);
    }

    public record GemsEntry(String id, int tier) {
        public static final Codec<GemsEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("id").forGetter(GemsEntry::id),
                        Codec.INT.fieldOf("tier").forGetter(GemsEntry::tier)
                ).apply(instance, GemsEntry::new));

        public static final StreamCodec<ByteBuf, GemsEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, GemsEntry::id,
                ByteBufCodecs.INT, GemsEntry::tier,
                GemsEntry::new);
    }

    public record StoredBonus(String id, double value) {
        public static final Codec<StoredBonus> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("id").forGetter(StoredBonus::id),
                        Codec.DOUBLE.fieldOf("value").forGetter(StoredBonus::value)
                ).apply(instance, StoredBonus::new));

        public static final StreamCodec<ByteBuf, StoredBonus> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, StoredBonus::id,
                ByteBufCodecs.DOUBLE, StoredBonus::value,
                StoredBonus::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<AffixEntry>>> AFFIXES =
            DATA_COMPONENTS.registerComponentType("affixes",
                    builder -> builder
                            .persistent(AffixEntry.CODEC.listOf())
                            .networkSynchronized(AffixEntry.STREAM_CODEC.apply(ByteBufCodecs.list())));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UPGRADE_LEVEL =
            DATA_COMPONENTS.registerComponentType("upgrade_level",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GemsEntry>>> GEMS =
            DATA_COMPONENTS.registerComponentType("gems",
                    builder -> builder
                            .persistent(GemsEntry.CODEC.listOf())
                            .networkSynchronized(GemsEntry.STREAM_CODEC.apply(ByteBufCodecs.list())));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GEM_TIER =
            DATA_COMPONENTS.registerComponentType("gem_tier",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> OFFHAND_MODE =
            DATA_COMPONENTS.registerComponentType("offhand_mode",
                    builder -> builder
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> REINCARNATION_COUNT =
            DATA_COMPONENTS.registerComponentType("reincarnation_count",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    /**
     * 重铸“盐值”。重铸结果必须由它确定性地推导出来，否则每次 createResult()（锻造台预览）都会掷一次新骰，
     * 玩家可以反复放取免费刷词条。盐值只随“真正取出”而 +1，因此同一次预览永远得到同一套词条。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROLL_SALT =
            DATA_COMPONENTS.registerComponentType("roll_salt",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    /**
     * 弧刀的斩击充能计数。
     *
     * <p>存在物品上而不是玩家上：换武器时计数跟着那把刀走，符合"这把刀蓄了几刀"的直觉，
     * 也不会因为玩家有多个角色/NBT 迁移而丢失。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ARC_SLASH_CHARGES =
            DATA_COMPONENTS.registerComponentType("arc_slash_charges",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    /** 读取斩击充能，缺失时视为 0。 */
    public static int arcSlashCharges(ItemStack stack) {
        return stack.getOrDefault(ARC_SLASH_CHARGES.get(), 0);
    }

    /** 写入斩击充能，0 时移除组件保持物品干净。 */
    public static void setArcSlashCharges(ItemStack stack, int value) {
        if (value <= 0) {
            stack.remove(ARC_SLASH_CHARGES.get());
        } else {
            stack.set(ARC_SLASH_CHARGES.get(), value);
        }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<StoredBonus>>> STORED_BONUSES =
            DATA_COMPONENTS.registerComponentType("stored_bonuses",
                    builder -> builder
                            .persistent(StoredBonus.CODEC.listOf())
                            .networkSynchronized(StoredBonus.STREAM_CODEC.apply(ByteBufCodecs.list())));


}
