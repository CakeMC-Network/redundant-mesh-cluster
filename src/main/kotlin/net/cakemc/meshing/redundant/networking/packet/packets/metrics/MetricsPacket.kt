package net.cakemc.meshing.redundant.networking.packet.packets.metrics

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class MetricsPacket(
    val nodeName: String,
    val cpuUsage: Double, // 0.0 - 1.0
    val ramUsage: Double,
    val networkUsage: Double, // Mbps or normalized
    val timestamp: Long = System.currentTimeMillis()
) : Packet(packetType = PacketType.REQUEST.ordinal)
