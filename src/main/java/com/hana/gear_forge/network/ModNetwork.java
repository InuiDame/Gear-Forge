package com.hana.gear_forge.network;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.config.ModConfigs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 把服务端的权威配置下发给客户端。
 *
 * <p>为什么需要：伤害/属性/掉落全部由服务端按<b>服务端</b>的 {@code config/gear_forge/*.json} 结算，
 * 而工具提示、物品属性表和弓蓄力预测在客户端按<b>客户端</b>的同一批文件计算。两边文件不一致时，
 * 玩家看到的数值和实际结算的数值不同（GF-007）。这里在登录时下发一次服务端快照，
 * 客户端只覆盖内存、不写回自己的文件；断线后重新读取本地文件。
 *
 * <p>注意 {@code RegisterPayloadHandlersEvent} 实现了 {@code IModBusEvent}，
 * FML 会依据事件类型把本类自动注册到 mod 总线，不需要（也不应该）再写 {@code bus} 参数。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class ModNetwork {

    private ModNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ConfigSyncPayload.TYPE,
                ConfigSyncPayload.STREAM_CODEC,
                // 默认就是 HandlerThread.MAIN（MainThreadPayloadHandler），这里显式写出意图。
                (payload, context) -> context.enqueueWork(() -> applySnapshot(payload)));
    }

    /**
     * 客户端收到快照后的处理。
     *
     * <p>不直接引用任何客户端专属类：本方法只碰配置对象，因此即使被加载到专用服务器也不会 NoClassDefFoundError。
     * 单人游戏里集成服务端与客户端共享同一 JVM 和同一批静态字段，重复应用同一份快照是幂等的。
     */
    private static void applySnapshot(ConfigSyncPayload payload) {
        ModConfigs.applyServerSnapshot(
                payload.affixes(), payload.upgrades(), payload.gems(), payload.mobGear());
        GearForgeMod.LOGGER.debug("Applied server-side gear_forge config snapshot");
    }
}
