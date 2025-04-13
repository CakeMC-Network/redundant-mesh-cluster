package net.cakemc.meshing.redundant.networking.packet.packets.task

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

data class TaskQueueAcknowledgePacket(
    val taskId: String,
    val nodeName: String,
    val status: TaskStatus
) : Packet(packetType = PacketType.REQUEST.ordinal)