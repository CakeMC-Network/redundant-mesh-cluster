package net.cakemc.meshing.redundant.watch

import java.nio.file.Path

data class FileInfo(
    val path: Path,
    val name: String,
    val isDirectory: Boolean,
    val size: Long?,
    val lastModified: Long?,
    val createdTime: Long?,
    val extension: String?,
    val mimeType: String?,
)
