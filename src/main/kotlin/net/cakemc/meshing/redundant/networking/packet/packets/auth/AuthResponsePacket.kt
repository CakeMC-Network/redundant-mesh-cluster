package net.cakemc.meshing.redundant.networking.packet.packets.auth

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class AuthResponsePacket(
  val status: Int,
  val message: String = "",
  val sessionToken: String? = null
): Packet(
    packetType = PacketType.RESPONSE.ordinal
)