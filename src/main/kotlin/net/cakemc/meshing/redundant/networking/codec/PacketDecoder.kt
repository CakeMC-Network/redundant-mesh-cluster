package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readUtf8String
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readVarInt
import java.util.*

class PacketDecoder(): ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        val responseUUID = UUID.fromString(buffer.readUtf8String())
        val typeOrdinal = buffer.readVarInt()
        val packetType = PacketType.values()[typeOrdinal]

        val sender = buffer.readUtf8String()
        val channel = buffer.readUtf8String()
        val topic = buffer.readUtf8String()
        val payload = buffer.readUtf8String()

        val packet = Packet(
            responseUUID = responseUUID,
            packetType = packetType,
            sender = sender,
            channel = channel,
            topic = topic,
            payload = payload
        )

        out.add(packet)
    }
}