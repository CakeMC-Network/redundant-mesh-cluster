package net.cakemc.meshing.redundant.networking.codec

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class FileTransfer(
    var sendTime: Long,
    var length: Int,
    var content: ByteArray,
    var fileName: String,
    var fileLocation: String,
    var fileHash: String
) {

    constructor(
        content: ByteArray,
        fileName: String,
        fileLocation: String,
        fileHash: String
    ): this(
        System.currentTimeMillis(), content.size, content, fileName, fileLocation, fileHash
    )

    companion object {

        fun calculateFileHash(file: ByteArray, algorithm: String = "SHA-256"): String {
            val digest = MessageDigest.getInstance(algorithm)
            ByteArrayInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun calculateFileHash(file: File, algorithm: String = "SHA-256"): String {
            val digest = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun calculateFileHash(path: Path, algorithm: String = "SHA-256"): String {
            val digest = MessageDigest.getInstance(algorithm)
            Files.newInputStream(path).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

    }

    override fun toString(): String {
        return buildString {
            appendLine("FileTransfer {")
            appendLine("  SendTime     : $sendTime")
            appendLine("  Length       : $length bytes")
            appendLine("  FileName     : $fileName")
            appendLine("  FileLocation : $fileLocation")
            appendLine("  FileHash     : $fileHash")
            appendLine("  Content      : ${content.size} bytes")
            append("}")
        }
    }

}