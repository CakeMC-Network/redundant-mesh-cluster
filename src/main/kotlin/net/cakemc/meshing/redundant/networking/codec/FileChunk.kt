package net.cakemc.meshing.redundant.networking.codec

data class FileChunk(
    val fileName: String,
    val fileLocation: String,
    val fileHash: String,
    val totalChunks: Int,
    val chunkIndex: Int,
    val data: ByteArray
)
