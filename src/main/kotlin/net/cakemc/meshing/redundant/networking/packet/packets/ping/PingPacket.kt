package net.cakemc.meshing.redundant.networking.packet.packets.ping

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class PingPacket(
    val from: String
) : Packet(packetType = PacketType.REQUEST.ordinal)