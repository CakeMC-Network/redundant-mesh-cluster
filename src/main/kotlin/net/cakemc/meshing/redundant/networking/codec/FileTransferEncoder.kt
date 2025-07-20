package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.cakemc.meshing.redundant.compression.ByteCompression
import net.cakemc.meshing.redundant.compression.NoCompression
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.writeUtf8String
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.writeVarInt

class FileTransferEncoder(): MessageToByteEncoder<FileTransfer>() {

    val compression: ByteCompression = NoCompression()

    override fun encode(ctx: ChannelHandlerContext, fileTransfer: FileTransfer, buffer: ByteBuf) {
        buffer.run {
            writeUtf8String(CodecType.FILE.name)

            // protocol
            writeLong(System.currentTimeMillis())
            // data
            writeVarInt(fileTransfer.length)
            writeBytes(compression.compress(fileTransfer.content))
            // location
            writeUtf8String(fileTransfer.fileName)
            writeUtf8String(fileTransfer.fileLocation)
            writeUtf8String(fileTransfer.fileHash)

        }
    }

}