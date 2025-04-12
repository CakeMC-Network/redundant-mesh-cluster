package net.cakemc.meshing.redundant.networking.packet.packets.auth

import net.cakemc.skrilla.networking.packet.Packet
import net.cakemc.skrilla.networking.packet.PacketType

class AuthRequestPacket(
    var username: String,
    var password: String
): Packet(
    packetType = PacketType.REQUEST.ordinal
)