package net.cakemc.meshing.redundant.watch

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.io.path.extension
import kotlin.io.path.name

class FolderMonitor(
    private val root: Path,
    private val onCreate: (FileInfo) -> Unit,
    private val onDelete: (FileInfo) -> Unit,
    private val onModify: (FileInfo) -> Unit,
) {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val executor = Executors.newSingleThreadExecutor()
    private val keyMap = ConcurrentHashMap<WatchKey, Path>()
    private val index = ConcurrentHashMap.newKeySet<Path>()

    fun start() {
        index.clear()
        registerAndIndexAll(root)

        executor.submit {
            while (!Thread.currentThread().isInterrupted) {
                val key = try {
                    watchService.take()
                } catch (e: InterruptedException) {
                    return@submit
                }

                val dir = keyMap[key] ?: continue

                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    val relativePath = event.context() as Path
                    val fullPath = dir.resolve(relativePath)

                    val fileInfo = buildFileInfo(fullPath)

                    when (kind) {
                        ENTRY_CREATE -> {
                            fileInfo?.let {
                                onCreate(it)
                                index.add(fullPath)
                                if (it.isDirectory) {
                                    registerAndIndexAll(fullPath)
                                }
                            }
                        }

                        ENTRY_DELETE -> {
                            onDelete(
                                fileInfo ?: FileInfo(
                                    path = fullPath,
                                    name = fullPath.name,
                                    isDirectory = false,
                                    size = null,
                                    lastModified = null,
                                    createdTime = null,
                                    extension = fullPath.extension,
                                    mimeType = null,
                                )
                            )
                            index.remove(fullPath)
                        }

                        ENTRY_MODIFY -> {
                            fileInfo?.let {
                                onModify(it)
                                index.add(fullPath)
                            }
                        }
                    }
                }

                if (!key.reset()) {
                    keyMap.remove(key)
                }
            }
        }
    }

    private fun buildFileInfo(path: Path): FileInfo? {
        return try {
            val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
            FileInfo(
                path = path,
                name = path.fileName.toString(),
                isDirectory = attrs.isDirectory,
                size = if (attrs.isRegularFile) attrs.size() else null,
                lastModified = attrs.lastModifiedTime().toMillis(),
                createdTime = attrs.creationTime().toMillis(),
                extension = path.extension.takeIf { it.isNotEmpty() },
                mimeType = Files.probeContentType(path),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun registerAndIndexAll(start: Path) {
        if (!Files.isDirectory(start)) return

        Files.walk(start).use { stream ->
            stream.forEach { path ->
                try {
                    index.add(path)
                    if (Files.isDirectory(path)) {
                        val key = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                        keyMap[key] = path
                    }
                } catch (e: Exception) {
                    println("Failed to register: $path - ${e.message}")
                }
            }
        }
    }

    fun stop() {
        executor.shutdownNow()
        watchService.close()
    }

    fun getIndex(): Set<Path> = index.toSet()
}
