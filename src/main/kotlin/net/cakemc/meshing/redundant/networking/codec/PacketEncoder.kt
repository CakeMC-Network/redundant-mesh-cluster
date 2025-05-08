package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.writeUtf8String
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.writeVarInt

class PacketEncoder(): MessageToByteEncoder<Packet>() {

    override fun encode(channel: ChannelHandlerContext, packet: Packet, output: ByteBuf) {
        output.run {
            writeUtf8String(packet.responseUUID.toString())
            writeVarInt(packet.packetType.ordinal)
            writeUtf8String(packet.sender)
            writeUtf8String(packet.channel)
            writeUtf8String(packet.topic)
            writeUtf8String(packet.payload)
        }
    }

}