package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.config.ModConfigs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * 客户端：断线后丢弃服务端下发的配置快照，恢复成自己磁盘上的 {@code config/gear_forge/*.json}。
 *
 * <p>否则从多人服务器退回单人游戏/主菜单时，客户端会继续用上一个服务器的数值渲染工具提示，
 * 出现“进了新世界但提示还是旧数值”的情况。
 *
 * <p>本类只会在客户端被加载（{@code value = Dist.CLIENT}），FML 的 Dist 过滤发生在类加载之前。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
public final class ClientConfigSync {

    private ClientConfigSync() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ModConfigs.reload();
    }
}
