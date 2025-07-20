package testing.file

import net.cakemc.meshing.redundant.watch.FolderMonitor
import java.nio.file.Paths
import kotlin.io.path.Path

fun main() {
    val monitor = FolderMonitor(
        Paths.get("./test"),
        onCreate = { println("Created: $it") },
        onDelete = { println("Deleted: $it") },
        onModify = { println("Modified: $it") }
    )

    monitor.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        monitor.stop()
        println("Final index:")
        monitor.getIndex().forEach { println(it) }
    })
}