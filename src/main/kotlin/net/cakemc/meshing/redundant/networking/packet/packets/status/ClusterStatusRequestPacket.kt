package net.cakemc.meshing.redundant.networking.packet.packets.status

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

data class ClusterStatusRequestPacket(
    val requestingNode: String
) : Packet(packetType = PacketType.REQUEST.ordinal)