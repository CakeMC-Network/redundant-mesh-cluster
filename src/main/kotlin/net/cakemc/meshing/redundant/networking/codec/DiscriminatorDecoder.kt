package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.CorruptedFrameException
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readUtf8String

class DiscriminatorDecoder : ByteToMessageDecoder() {

    private val packetDecoder = PacketDecoder()
    private val fileDecoder = FileTransferDecoder()
    private val fileChunkDecoder = FileChunkDecoder()

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (input.readableBytes() < 2) return

        input.markReaderIndex()
        try {
            val type = input.readUtf8String()

            input.resetReaderIndex()

            when (type) {
                CodecType.PACKET.name -> packetDecoder.decode(ctx, input, out)
                CodecType.FILE.name -> fileDecoder.decode(ctx, input, out)
                CodecType.FILE_CHUNK.name -> fileChunkDecoder.decode(ctx, input, out)

                else -> throw CorruptedFrameException("Unknown data type: $type")
            }

        } catch (e: IndexOutOfBoundsException) {
            input.resetReaderIndex() // Not enough data, wait for more
        } catch (e: Exception) {
            input.resetReaderIndex()
            throw CorruptedFrameException("Failed decoding: ${e.message}")
        }
    }

}
