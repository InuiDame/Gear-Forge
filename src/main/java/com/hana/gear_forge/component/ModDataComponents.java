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
                            .networkSynchronized(ByteBufCodecs.fromCodec(AffixEntry.CODEC.listOf())));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UPGRADE_LEVEL =
            DATA_COMPONENTS.registerComponentType("upgrade_level",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GemsEntry>>> GEMS =
            DATA_COMPONENTS.registerComponentType("gems",
                    builder -> builder
                            .persistent(GemsEntry.CODEC.listOf())
                            .networkSynchronized(ByteBufCodecs.fromCodec(GemsEntry.CODEC.listOf())));

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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<StoredBonus>>> STORED_BONUSES =
            DATA_COMPONENTS.registerComponentType("stored_bonuses",
                    builder -> builder
                            .persistent(StoredBonus.CODEC.listOf())
                            .networkSynchronized(ByteBufCodecs.fromCodec(StoredBonus.CODEC.listOf())));

}
