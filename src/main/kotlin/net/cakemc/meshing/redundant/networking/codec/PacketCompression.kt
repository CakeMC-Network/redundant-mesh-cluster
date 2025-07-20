package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

class PacketCompression {

    private val buffer = ByteArray(8192)
    private var compress: Boolean = false
    private var deflater: Deflater? = null
    private var inflater: Inflater? = null

    fun init(compress: Boolean, level: Int) {
        this.compress = compress
        free()

        if (compress) {
            deflater = Deflater(level)
        } else {
            inflater = Inflater()
        }
    }

    fun free() {
        deflater?.end()
        deflater = null
        inflater?.end()
        inflater = null
    }

    @Throws(DataFormatException::class)
    fun process(`in`: ByteBuf, out: ByteBuf) {
        val inData = ByteArray(`in`.readableBytes())
        `in`.readBytes(inData)

        if (compress) {
            deflater?.setInput(inData)
            deflater?.finish()

            while (deflater != null && !(deflater?.finished() ?: true)) {
                val count = deflater!!.deflate(buffer)
                out.writeBytes(buffer, 0, count)
            }

            deflater?.reset()
        } else {
            inflater?.setInput(inData)

            while (inflater != null && !(inflater?.finished() ?: true) && inflater!!.totalIn < inData.size) {
                val count = inflater!!.inflate(buffer)
                out.writeBytes(buffer, 0, count)
            }

            inflater?.reset()
        }
    }
}
