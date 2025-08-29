package net.cakemc.meshing.redundant.archive

import java.io.*
import java.nio.file.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.Deflater
import java.util.zip.Inflater

class ClusterArchiveFormat(private val archiveFile: Path) {
    private val fileIndex = mutableMapOf<String, MutableList<Pair<String, Long>>>()
    private val history = mutableListOf<String>()
    private var dataOffset = 0L

    init {
        if (Files.exists(archiveFile)) loadArchive()
    }

    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private fun compress(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()
        return output.toByteArray()
    }

    private fun decompress(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            output.write(buffer, 0, count)
        }
        inflater.end()
        return output.toByteArray()
    }

    private fun loadArchive() {
        val raf = RandomAccessFile(archiveFile.toFile(), "r")
        raf.seek(0)
        val magic = raf.readLine()
        if (magic != "[CZSTART]") throw IOException("Invalid archive")

        val version = raf.readLine() // Skip version
        if (raf.readLine() != "[INDEX]") throw IOException("Invalid index")

        while (true) {
            val line = raf.readLine() ?: break
            if (line == "[HISTORY]") break
            if (line.endsWith(":")) {
                val name = line.removeSuffix(":")
                fileIndex[name] = mutableListOf()
            } else if (line.startsWith("  ")) {
                val (ts, offset) = line.trim().split(":", limit = 2)
                fileIndex.entries.last().value.add(ts to offset.toLong())
            }
        }

        while (true) {
            val line = raf.readLine() ?: break
            history.add(line)
        }

        dataOffset = raf.filePointer
        raf.close()
    }

    private fun writeArchive() {
        val tempFile = archiveFile.resolveSibling("${archiveFile.fileName}.tmp")
        RandomAccessFile(tempFile.toFile(), "rw").use { raf ->
            raf.writeBytes("[CZSTART]\n")
            raf.writeBytes("v1\n")
            raf.writeBytes("[INDEX]\n")
            for ((name, versions) in fileIndex) {
                raf.writeBytes("$name:\n")
                for ((ts, offset) in versions) {
                    raf.writeBytes("  $ts:$offset\n")
                }
            }
            raf.writeBytes("[HISTORY]\n")
            history.forEach { raf.writeBytes("$it\n") }

            // move to data section
            dataOffset = raf.filePointer

            for ((_, versions) in fileIndex) {
                for ((_, offset) in versions) {
                    raf.seek(offset)
                    // data already written, skip
                }
            }
        }

        Files.move(tempFile, archiveFile, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun writeData(data: ByteArray): Long {
        val raf = RandomAccessFile(archiveFile.toFile(), "rw")
        val offset = raf.length()
        raf.seek(offset)
        val size = ByteBuffer.allocate(4).putInt(data.size).array()
        raf.write(size)
        raf.write(data)
        raf.close()
        return offset
    }

    private fun readData(offset: Long): ByteArray {
        val raf = RandomAccessFile(archiveFile.toFile(), "r")
        raf.seek(offset)
        val sizeBytes = ByteArray(4)
        raf.read(sizeBytes)
        val size = ByteBuffer.wrap(sizeBytes).int
        val data = ByteArray(size)
        raf.readFully(data)
        raf.close()
        return data
    }

    fun addFile(name: String, content: ByteArray, message: String) {
        val timestamp = now()
        val compressed = compress(content)
        val offset = writeData(compressed)
        fileIndex.computeIfAbsent(name) { mutableListOf() }.add(timestamp to offset)
        history.add("$timestamp - ADD '$name' - $message")
        writeArchive()
    }

    fun updateFile(name: String, content: ByteArray, message: String) {
        if (!fileIndex.containsKey(name)) throw IllegalArgumentException("File not found")
        val timestamp = now()
        val compressed = compress(content)
        val offset = writeData(compressed)
        fileIndex[name]?.add(timestamp to offset)
        history.add("$timestamp - UPDATE '$name' - $message")
        writeArchive()
    }

    fun rollbackFile(name: String, toVersion: Int, message: String) {
        val versions = fileIndex[name] ?: throw IllegalArgumentException("No such file")
        val version = versions.getOrNull(toVersion) ?: throw IllegalArgumentException("Invalid version")
        fileIndex[name]?.add(now() to version.second)
        history.add("${now()} - ROLLBACK '$name' to version $toVersion - $message")
        writeArchive()
    }

    fun removeFile(name: String, message: String) {
        if (fileIndex.remove(name) != null) {
            history.add("${now()} - REMOVE '$name' - $message")
            writeArchive()
        }
    }

    fun getFile(name: String): ByteArray? {
        val latest = fileIndex[name]?.lastOrNull() ?: return null
        val raw = readData(latest.second)
        return decompress(raw)
    }

    fun getFileAt(name: String, version: Int): ByteArray? {
        val entry = fileIndex[name]?.getOrNull(version) ?: return null
        return decompress(readData(entry.second))
    }

    fun listFiles(): List<String> = fileIndex.keys.toList()

    fun listFileVersions(name: String): List<String> =
        fileIndex[name]?.mapIndexed { i, (ts, _) -> "$i: $ts" } ?: emptyList()

    fun getHistory(): List<String> = history.toList()

    fun exportToFolder(destination: Path) {
        Files.createDirectories(destination)
        for ((fileName, versions) in fileIndex) {
            for ((index, version) in versions.withIndex()) {
                val content = getFileAt(fileName, index)
                val baseName = fileName.substringBeforeLast('.')
                val ext = fileName.substringAfterLast('.', "")
                val versionedName = "${baseName}_v$index" + if (ext.isNotEmpty()) ".$ext" else ""
                Files.write(destination.resolve(versionedName), content!!)
            }
        }

        // Export history
        val historyFile = destination.resolve("history.txt")
        Files.write(historyFile, history, StandardCharsets.UTF_8)
    }

    fun importFromFolder(source: Path) {
        if (!Files.isDirectory(source)) throw IllegalArgumentException("Not a directory")

        val files = Files.list(source).toList().filter { it.fileName.toString() != "history.txt" }
        val grouped = files.groupBy { path ->
            val name = path.fileName.toString()
            name.replace(Regex("_v\\d+(\\..+)?$"), "") // extract base name
        }

        for ((baseName, paths) in grouped) {
            val sortedPaths = paths.sortedBy {
                val name = it.fileName.toString()
                Regex("_v(\\d+)").find(name)?.groupValues?.get(1)?.toInt() ?: 0
            }

            sortedPaths.forEachIndexed { i, path ->
                val content = Files.readAllBytes(path)
                val msg = if (i == 0) "Imported version 0 of $baseName" else "Imported version $i of $baseName"
                if (!fileIndex.containsKey(baseName)) {
                    addFile(baseName, content, msg)
                } else {
                    updateFile(baseName, content, msg)
                }
            }
        }

        val historyPath = source.resolve("history.txt")
        if (Files.exists(historyPath)) {
            val newEntries = Files.readAllLines(historyPath, StandardCharsets.UTF_8)
            history.addAll(newEntries)
            writeArchive()
        }
    }

}
