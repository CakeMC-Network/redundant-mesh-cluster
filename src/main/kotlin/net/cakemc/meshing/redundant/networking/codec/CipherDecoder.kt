package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.GeneralSecurityException

class CipherDecoder : MessageToMessageDecoder<ByteBuf>() {

    private val cipherWrapper: CipherWrapper = CipherWrapper()

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        cipherWrapper.free()
    }

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        out.add(cipherWrapper.decrypt(ctx, msg))
    }
}
