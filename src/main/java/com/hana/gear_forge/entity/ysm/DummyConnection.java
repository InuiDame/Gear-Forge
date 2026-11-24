package com.hana.gear_forge.entity.ysm;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;

/**
 * 吞掉所有发给假玩家 NPC 的数据包的空连接。
 *
 * <p>关键：Connection 必须拥有一个真实的 Netty channel，否则两处 NPE：
 * <ol>
 *   <li>{@code PlayerList.placeNewPlayer()} 调用 {@code setupInboundProtocol()} 时
 *       {@code this.channel.writeAndFlush(...)}；</li>
 *   <li>NeoForge 的 {@code OnDatapackSyncEvent} → {@code hasChannel()} →
 *       {@code connection.channel().attr(...)}，会直接中断 placeNewPlayer。</li>
 * </ol>
 * 用 {@link EmbeddedChannel} 注册本连接作为 handler，channelActive 回调会给 Connection.channel
 * 赋值为内存 channel，之后所有 attr()/writeAndFlush() 都作用在内存 channel 上，不发真实网络数据。
 */
public final class DummyConnection extends Connection {

    private final EmbeddedChannel channel;

    public DummyConnection() {
        super(PacketFlow.SERVERBOUND);
        // 把自身作为 handler 加入内存 channel；channelActive 会设置 Connection.channel
        this.channel = new EmbeddedChannel(this);
    }

    @Override
    public void send(Packet<?> packet) {
        // no-op：吞掉发给假玩家的所有包
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {
        // no-op
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener, boolean flush) {
        // no-op
    }

    @Override
    public String getLoggableAddress(boolean logIps) {
        return "<gear_forge_npc>";
    }
}
