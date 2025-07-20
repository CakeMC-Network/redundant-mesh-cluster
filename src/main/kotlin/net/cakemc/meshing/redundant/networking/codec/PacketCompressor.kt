package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.writeVarInt

class PacketCompressor : MessageToByteEncoder<ByteBuf>() {

    private val compression = PacketCompression()
    private var threshold: Int = 256

    @Throws(Exception::class)
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        compression.init(true, -1)
    }

    @Throws(Exception::class)
    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        compression.free()
    }

    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        val originalLength = msg.readableBytes()
        if (originalLength < threshold) {
            out.writeVarInt(0)
            out.writeBytes(msg)
        } else {
            out.writeVarInt(originalLength)
            compression.process(msg, out)
        }
    }

    fun setThreshold(threshold: Int) {
        this.threshold = threshold
    }
}
