package net.cakemc.meshing.redundant.networking.codec

import java.util.*

class Packet(
    var responseUUID: UUID = UUID.randomUUID(),
    var packetType: PacketType,

    val sender: String,
    val channel: String,
    val topic: String,
    val payload: String,
) {

    constructor(
        packetType: PacketType,

        sender: String,
        channel: String,
        topic: String,
        payload: String
    ) : this(UUID.randomUUID(), packetType, sender, channel, topic, payload)

    override fun toString(): String {
        return buildString {
            appendLine("Packet {")
            appendLine("  UUID     : $responseUUID")
            appendLine("  Type     : ${packetType.name} [${packetType.ordinal}]")
            appendLine("  Sender   : $sender")
            appendLine("  Channel  : $channel")
            appendLine("  Topic    : $topic")
            appendLine("  Payload  : $payload")
            append("}")
        }
    }

}
