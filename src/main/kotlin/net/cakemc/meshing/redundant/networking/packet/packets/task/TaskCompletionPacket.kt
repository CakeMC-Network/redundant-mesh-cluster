package net.cakemc.meshing.redundant.networking.packet.packets.task

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

data class TaskCompletionPacket(
    val taskId: String,
    val completedBy: String
) : Packet(packetType = PacketType.REQUEST.ordinal)