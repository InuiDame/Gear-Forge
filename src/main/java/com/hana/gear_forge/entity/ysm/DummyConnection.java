package com.hana.gear_forge.entity.ysm;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;

/** 吞掉所有发给假玩家 NPC 的数据包的空连接。 */
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

    @Override
    public String getLoggableAddress(boolean logIps) {
        return "<gear_forge_npc>";
    }
}
