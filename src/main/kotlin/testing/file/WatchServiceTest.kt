package testing.file

import net.cakemc.meshing.redundant.watch.FolderMonitor
import kotlin.io.path.Path

fun main() {
    val monitor = FolderMonitor(
        path = Path("./test"),
        onCreate = { println("Created: ${it.fileName}") },
        onDelete = { println("Deleted: ${it.fileName}") },
        onModify = { println("Modified: ${it.fileName}") },
        onMove = { from, to -> println("Moved: $from -> $to") }
    )

    monitor.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        monitor.stop()
    })
}
