package net.cakemc.meshing.redundant.networking.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class CipherWrapper(private val encrypt: Boolean = false) {

    private val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    private val secretKey = SecretKeySpec(KEY_BYTES, "AES")

    init {
        cipher.init(
            if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE,
            secretKey
        )
    }

    fun encrypt(input: ByteBuf, output: ByteBuf) {
        val inputBytes = ByteArray(input.readableBytes())
        input.readBytes(inputBytes)

        val encrypted = cipher.doFinal(inputBytes)
        output.writeBytes(encrypted)
    }

    fun decrypt(ctx: io.netty.channel.ChannelHandlerContext, input: ByteBuf): ByteBuf {
        val inputBytes = ByteArray(input.readableBytes())
        input.readBytes(inputBytes)

        val decrypted = cipher.doFinal(inputBytes)
        return Unpooled.wrappedBuffer(decrypted)
    }

    fun free() {}

    companion object {
        // Example static AES key (16 bytes = 128 bits). Replace with secure key.
        private var KEY_BYTES = byteArrayOf(
            0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0x0C,
            0x0D, 0x0E, 0x0F, 0x10
        )

        fun setKey(key: ByteArray) {
            if (key.size > 16 || key.size < 16)
                throw IllegalArgumentException("invalid key size please provide a 128 bit sized key!")
            KEY_BYTES = key
        }
    }
}
