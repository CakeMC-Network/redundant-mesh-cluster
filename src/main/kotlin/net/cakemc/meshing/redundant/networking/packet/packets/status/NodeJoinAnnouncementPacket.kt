package net.cakemc.meshing.redundant.networking.packet.packets.status

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

data class NodeJoinAnnouncementPacket(
    val nodeName: String
) : Packet(packetType = PacketType.REQUEST.ordinal)