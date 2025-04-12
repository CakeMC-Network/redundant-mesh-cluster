package net.cakemc.meshing.redundant.networking.packet.packets.session

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class SessionRenewalResponsePacket(
    val success: Boolean,
    val newExpiration: Long? = null,
    val message: String
) : Packet(packetType = PacketType.RESPONSE.ordinal)