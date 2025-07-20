package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class FramingEncoder : MessageToByteEncoder<ByteBuf>() {
    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        out.ensureWritable(4 + msg.readableBytes())
        out.writeInt(msg.readableBytes())
        out.writeBytes(msg, msg.readerIndex(), msg.readableBytes())
    }
}
