package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class FramingDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, out: MutableList<Any>) {
        while (byteBuf.readableBytes() >= 4) {
            byteBuf.markReaderIndex()

            val toRead = byteBuf.readInt()
            if (byteBuf.readableBytes() < toRead) {
                byteBuf.resetReaderIndex()
                return
            }

            out.add(byteBuf.readBytes(toRead))
        }
    }
}
