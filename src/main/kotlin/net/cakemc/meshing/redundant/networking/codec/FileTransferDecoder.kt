package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import jdk.internal.org.jline.utils.Colors.s
import net.cakemc.meshing.redundant.compression.ByteCompression
import net.cakemc.meshing.redundant.compression.DefaultCompression
import net.cakemc.meshing.redundant.compression.NoCompression
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readUtf8String
import net.cakemc.meshing.redundant.networking.codec.BufferManipulation.readVarInt

class FileTransferDecoder(): ByteToMessageDecoder() {

    val compression: ByteCompression = NoCompression()

    public override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        val type = buffer.readUtf8String()
        if (type != CodecType.FILE.name)
            return

        val sendTime = buffer.readLong()
        val size = buffer.readVarInt()

        var content = ByteArray(size)
        buffer.readBytes(content)

        content = compression.decompress(content)

        val transfer = FileTransfer(
            sendTime,
            size,
            content,
            buffer.readUtf8String(),
            buffer.readUtf8String(),
            buffer.readUtf8String()
        )

        out.add(transfer)
    }

}