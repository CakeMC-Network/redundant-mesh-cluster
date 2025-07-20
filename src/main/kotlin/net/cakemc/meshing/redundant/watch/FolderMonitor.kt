package net.cakemc.meshing.redundant.watch

import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.Executors
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class FolderMonitor(
    private val path: Path,
    private val onCreate: (Path) -> Unit,
    private val onDelete: (Path) -> Unit,
    private val onModify: (Path) -> Unit,
    private val onMove: (Path, Path) -> Unit
) {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val executor = Executors.newSingleThreadExecutor()
    private val recentDeletes = mutableMapOf<Path, Long>() // for simulating move detection

    fun start() {
        path.register(
            watchService,
            ENTRY_CREATE,
            ENTRY_DELETE,
            ENTRY_MODIFY
        )

        executor.submit {
            while (true) {
                val key = watchService.take() // blocks
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    val relativePath = event.context() as Path
                    val fullPath = path.resolve(relativePath)

                    when (kind) {
                        ENTRY_CREATE -> {
                            // Check if recently deleted: simulate move
                            val deletedTime = recentDeletes.remove(fullPath)
                            if (deletedTime != null && System.currentTimeMillis() - deletedTime < 1000) {
                                onMove(fullPath, fullPath)
                            } else {
                                onCreate(fullPath)
                            }
                        }

                        ENTRY_DELETE -> {
                            onDelete(fullPath)
                            // Remember for possible move detection
                            recentDeletes[fullPath] = System.currentTimeMillis()
                        }

                        ENTRY_MODIFY -> {
                            onModify(fullPath)
                        }
                    }
                }
                key.reset()
            }
        }
    }

    fun stop() {
        executor.shutdownNow()
        watchService.close()
        println("🛑 Stopped watching: ${path.absolutePathString()}")
    }
}
