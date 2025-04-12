package net.cakemc.meshing.redundant.networking.packet.packets.session

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class SessionValidationRequestPacket(
    val token: String
) : Packet(packetType = PacketType.REQUEST.ordinal)