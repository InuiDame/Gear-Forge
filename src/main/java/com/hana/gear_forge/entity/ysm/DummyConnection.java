package com.hana.gear_forge.entity.ysm;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;

/**
 * 吞掉所有发给假玩家 NPC 的数据包的空连接。
 *
 * <p>关键：ServerPlayerList.placeNewPlayer() 会调用 setupInboundProtocol()/setupOutboundProtocol()，
 * 这两个方法内部访问 {@code this.channel.writeAndFlush(...)}。本类没有真实 Netty channel，
 * 因此必须重写它们为空操作，否则 placeNewPlayer 会抛 NPE，导致右键召唤完全无反应。
 */
public final class DummyConnection extends Connection {

    public DummyConnection() {
        super(PacketFlow.SERVERBOUND);
    }

    @Override
    public void send(Packet<?> packet) {
        // no-op
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {
        // no-op
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener, boolean flush) {
        // no-op
    }

    /** 假玩家不接收任何包，跳过 inbound 管线配置（原版实现会 NPE）。 */
    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetInfo) {
        // no-op
    }

    /** 假玩家不发送任何包，跳过 outbound 管线配置（原版实现会 NPE）。 */
    @Override
    public void setupOutboundProtocol(ProtocolInfo<?> protocolInfo) {
        // no-op
    }

    @Override
    public String getLoggableAddress(boolean logIps) {
        return "<gear_forge_npc>";
    }
}
