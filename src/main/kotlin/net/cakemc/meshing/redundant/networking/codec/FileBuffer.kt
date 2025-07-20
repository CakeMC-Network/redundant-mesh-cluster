package net.cakemc.meshing.redundant.networking.codec

data class FileBuffer(val totalChunks: Int, val chunks: MutableMap<Int, ByteArray>)
