package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readVarInt

class PacketDecompressor : MessageToMessageDecoder<ByteBuf>() {

    private val compression = PacketCompression()

    @Throws(Exception::class)
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        compression.init(false, 0)
    }

    @Throws(Exception::class)
    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        compression.free()
    }

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        val length = buf.readVarInt()
        if (length == 0) {
            out.add(buf.slice().retain())
            buf.skipBytes(buf.readableBytes())
        } else {
            var decompressed = ctx.alloc().directBuffer()
            try {
                compression.process(buf, decompressed)
                if (decompressed.readableBytes() != length) {
                    out.add(decompressed)
                    decompressed = null
                    return
                }
                System.err.println("Decompressed packet size mismatch")
            } finally {
                decompressed?.release()
            }
        }
    }
}
