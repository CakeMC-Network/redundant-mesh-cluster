package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.writeUtf8String
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.writeVarInt

class FileChunkEncoder : MessageToByteEncoder<FileChunk>() {
    override fun encode(ctx: ChannelHandlerContext, msg: FileChunk, out: ByteBuf) {
        out.run {
            writeUtf8String(CodecType.FILE_CHUNK.name)

            writeUtf8String(msg.fileName)
            writeUtf8String(msg.fileLocation)
            writeUtf8String(msg.fileHash)

            writeVarInt(msg.totalChunks)
            writeVarInt(msg.chunkIndex)

            writeVarInt(msg.data.size)
            writeBytes(msg.data)
        }
    }
}
