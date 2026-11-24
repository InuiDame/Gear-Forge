package com.hana.gear_forge.mixin;

import com.hana.gear_forge.entity.ysm.DummyConnection;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让发给 YSM 假玩家 NPC 的自定义 payload 跳过 channel 协商检查。
 *
 * <p>假玩家通过 {@code PlayerList.placeNewPlayer} 加入时会触发 {@code OnDatapackSyncEvent}，
 * 第三方模组（如 TaCZ）会给所有 relevant players 发自定义 payload（如 server_sync_gun_pack）。
 * NeoForge 的 {@code checkPacket} 会发现假玩家的连接未协商该 payload channel 而抛
 * UnsupportedOperationException，该异常从事件总线传播出去中断 placeNewPlayer，
 * 导致 NPC 生成失败、YSM 模型套不上。
 *
 * <p>假玩家没有真实客户端，所有包最终都会被 DummyConnection.send() 吞掉，
 * 因此对其跳过检查完全安全；真实玩家不受影响。
 */
@Mixin(NetworkRegistry.class)
public abstract class NetworkRegistryMixin {

    @Inject(method = "checkPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/protocol/common/ServerCommonPacketListener;)V",
            at = @At("HEAD"), cancellable = true)
    private static void gearForge$skipCheckForDummyConnection(
            net.minecraft.network.protocol.Packet<?> packet,
            ServerCommonPacketListener listener,
            CallbackInfo ci) {
        if (listener != null && listener.getConnection() instanceof DummyConnection) {
            ci.cancel();
        }
    }
}
