package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readUtf8String
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readVarInt

class FileChunkDecoder : ByteToMessageDecoder() {


    public override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        buffer.markReaderIndex()

        try {
            // Check if there's enough data to read the initial type string length (VarInt)
            if (buffer.readableBytes() < 1) {
                buffer.resetReaderIndex(); return
            }

            val type = buffer.readUtf8String()

            if (type != CodecType.FILE_CHUNK.name) {
                return // Unknown message type, discard or ignore
            }

            // Check for enough data for fileName and fileHash (assume VarInt length + some data)
            if (buffer.readableBytes() < 4) {
                buffer.resetReaderIndex(); return
            }

            val fileName = buffer.readUtf8String()
            val fileLocation = buffer.readUtf8String()
            val fileHash = buffer.readUtf8String()

            // Check if we have enough data for 3 VarInts (totalChunks, chunkIndex, dataLen)
            if (buffer.readableBytes() < 3) {
                buffer.resetReaderIndex(); return
            }

            val totalChunks = buffer.readVarInt()
            val chunkIndex = buffer.readVarInt()
            val dataLen = buffer.readVarInt()

            if (buffer.readableBytes() < dataLen) {
                buffer.resetReaderIndex(); return
            }

            val data = ByteArray(dataLen)
            buffer.readBytes(data)

            out.add(
                FileChunk(
                    fileName = fileName,
                    fileLocation = fileLocation,
                    fileHash = fileHash,
                    totalChunks = totalChunks,
                    chunkIndex = chunkIndex,
                    data = data
                )
            )
        } catch (e: Exception) {
            buffer.resetReaderIndex()
        }
    }

}
