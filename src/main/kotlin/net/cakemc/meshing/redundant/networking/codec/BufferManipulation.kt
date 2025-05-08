package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil

object BufferManipulation {

    fun ByteBuf.readUtf8String(): String {
        val length = readVarInt()
        val bytes = ByteArray(length)
        readBytes(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    fun ByteBuf.readRemainingUtf8(): String {
        return this.readCharSequence(this.readableBytes(), CharsetUtil.UTF_8).toString()
    }

    fun ByteBuf.writeUtf8String(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeVarInt(bytes.size)
        writeBytes(bytes)
    }

    fun ByteBuf.readVarInt(): Int {
        var numRead = 0
        var result = 0
        var read: Int

        do {
            if (!this.isReadable) throw IndexOutOfBoundsException("ByteBuf underflow while reading VarInt")
            read = this.readByte().toInt()
            val value = read and 0x7F
            result = result or (value shl (7 * numRead))

            numRead++
            if (numRead > 5) {
                throw RuntimeException("VarInt is too big")
            }
        } while ((read and 0x80) != 0)

        return result
    }

    fun ByteBuf.writeVarInt(value: Int) {
        var i = value
        while ((i and 0x7F.inv()) != 0) {
            this.writeByte((i and 0x7F) or 0x80)
            i = i ushr 7
        }
        this.writeByte(i)
    }



}