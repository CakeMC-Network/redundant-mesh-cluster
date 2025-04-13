package net.cakemc.meshing.redundant.networking.packet.packets.status

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

data class ClusterReadyBroadcastPacket(
    val nodes: List<String>
) : Packet(packetType = PacketType.RESPONSE.ordinal)