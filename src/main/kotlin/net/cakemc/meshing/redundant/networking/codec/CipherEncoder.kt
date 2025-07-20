package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class CipherEncoder : MessageToByteEncoder<ByteBuf>() {

    private val cipherWrapper: CipherWrapper = CipherWrapper(encrypt = true)

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        cipherWrapper.free()
    }

    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        cipherWrapper.encrypt(msg, out)
    }
}
