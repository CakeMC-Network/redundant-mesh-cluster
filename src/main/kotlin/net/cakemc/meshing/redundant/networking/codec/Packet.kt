package net.cakemc.meshing.redundant.networking.codec

import java.util.*

class Packet(
    var responseUUID: UUID = UUID.randomUUID(),
    var packetType: PacketType,

    val sender: String,
    val channel: String,
    val topic: String,
    val payload: String,
)