package com.hana.gear_forge.network;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.config.ModConfigs;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 服务端侧：在玩家登录时把服务端配置快照发给该玩家。
 *
 * <p>放在游戏总线（{@link PlayerEvent.PlayerLoggedInEvent} 不是 {@code IModBusEvent}），
 * 用 {@link EventPriority#LOWEST} 让其它 mod 先处理登录事件，避免我们的包早于连接就绪。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class ConfigSyncEvents {

    private ConfigSyncEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PacketDistributor.sendToPlayer(player, snapshot());
    }

    /** 供 {@code /reload} 或配置热重载后广播使用。 */
    public static void broadcast() {
        PacketDistributor.sendToAllPlayers(snapshot());
    }

    private static ConfigSyncPayload snapshot() {
        String[] parts = ModConfigs.toSnapshot();
        return new ConfigSyncPayload(parts[0], parts[1], parts[2], parts[3]);
    }
}
