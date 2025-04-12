package net.cakemc.meshing.redundant.networking.packet.packets.session

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class SessionValidationResponsePacket(
    val isValid: Boolean,
    val username: String? = null,
    val message: String
) : Packet(packetType = PacketType.RESPONSE.ordinal)