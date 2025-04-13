package net.cakemc.meshing.redundant.networking.packet.packets.task

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

data class TaskQueueRequestPacket(
    val nodeName: String
) : Packet(packetType = PacketType.REQUEST.ordinal)