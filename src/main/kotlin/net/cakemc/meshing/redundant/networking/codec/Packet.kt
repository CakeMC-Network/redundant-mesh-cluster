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
