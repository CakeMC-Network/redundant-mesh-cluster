package net.cakemc.meshing.redundant.event.impl.file

import io.netty.channel.Channel

class FileReceivedEvent(
    val channel: Channel,
    val name: String,
    val location: String,
    val content: ByteArray
)