package net.cakemc.meshing.redundant.networking.codec

import java.text.SimpleDateFormat
import java.util.*

class Packet(
    var responseUUID: UUID = UUID.randomUUID(),
    var packetType: PacketType,

    val creationTime: Long = System.currentTimeMillis(),

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
    ) : this(UUID.randomUUID(), packetType, creationTime = System.currentTimeMillis(), sender, channel, topic, payload)

    override fun toString(): String {
        return buildString {
            appendLine("Packet {")
            appendLine("  UUID     : $responseUUID")
            appendLine("  Type     : ${packetType.name} [${packetType.ordinal}]")
            appendLine("  Time     : ${timeFormat(creationTime)}")
            appendLine("  Sender   : $sender")
            appendLine("  Channel  : $channel")
            appendLine("  Topic    : $topic")
            appendLine("  Payload  : $payload")
            append("}")
        }
    }

    private fun timeFormat(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

}
