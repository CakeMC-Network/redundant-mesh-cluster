package net.cakemc.meshing.redundant.networking.packet.packets.task

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

data class TaskRequestPacket(
    val taskId: String,
    val taskDescription: String
) : Packet(packetType = PacketType.REQUEST.ordinal)