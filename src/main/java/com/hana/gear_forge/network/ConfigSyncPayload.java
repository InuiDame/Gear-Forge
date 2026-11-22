package com.hana.gear_forge.network;

import com.hana.gear_forge.GearForgeMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端的配置快照。
 *
 * <p>四段配置各用一个 UTF-8 字符串传输。选择“原样传输 JSON 文本”而不是逐字段的二进制结构，理由是：
 * 配置模型随时会因为平衡调整而增删字段，逐字段编解码会让“客户端模组版本略旧”直接断连；
 * 而 JSON 文本由接收端的 Gson 解析，缺失/多余的字段都能自然降级。
 *
 * <p>体积上限：默认四份配置合计约 20 KB，{@link ByteBufCodecs#STRING_UTF8} 的上限是 32767 个字符，
 * 单段足够；如将来配置变大需要拆分或改用压缩，这里应当改为分段 payload。
 */
public record ConfigSyncPayload(String affixes, String upgrades, String gems, String mobGear)
        implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ConfigSyncPayload::affixes,
                    ByteBufCodecs.STRING_UTF8, ConfigSyncPayload::upgrades,
                    ByteBufCodecs.STRING_UTF8, ConfigSyncPayload::gems,
                    ByteBufCodecs.STRING_UTF8, ConfigSyncPayload::mobGear,
                    ConfigSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
