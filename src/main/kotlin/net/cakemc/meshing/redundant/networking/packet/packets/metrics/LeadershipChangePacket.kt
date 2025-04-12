package net.cakemc.meshing.redundant.networking.packet.packets.metrics

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class LeadershipChangePacket(
    val newLeader: String,
    val reason: String
) : Packet(packetType = PacketType.REQUEST.ordinal)