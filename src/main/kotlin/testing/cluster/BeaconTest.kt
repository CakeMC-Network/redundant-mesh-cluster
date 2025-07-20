package testing.cluster

import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.event.impl.file.FileReceivedEvent
import net.cakemc.meshing.redundant.networking.EndPoint
import net.cakemc.meshing.redundant.networking.codec.Packet
import net.cakemc.meshing.redundant.networking.codec.PacketType
import java.io.File
import java.nio.file.Path
import kotlin.concurrent.thread

fun main() {
    val endpoint1 = EndPoint.createEndPoint()
    val endpoint2 = EndPoint.createEndPoint()
    val endpoint3 = EndPoint.createEndPoint()

    registerHandler(endpoint1)
    registerHandler(endpoint2)
    registerHandler(endpoint3)

    thread(start = true, isDaemon = true) { endpoint1.start() }
    thread(start = true, isDaemon = true) { endpoint2.start() }
    thread(start = true, isDaemon = true) { endpoint3.start() }

    Thread.sleep(2000)

    sendTestPacket(endpoint2)

    sendFile(endpoint1, "./test/test/", Path.of("./test/test.png"))
    sendFile(endpoint1, "./test/test/", Path.of("./test/test.txt"))
}

fun sendTestPacket(endPoint: EndPoint) {
    endPoint.handler().sendToAllSync(
        Packet(PacketType.NORMAL, endPoint.member().identifier, "test", "testing", "{}")
    )
}

fun sendFile(endPoint: EndPoint, targetFolder: String, file: Path) {
    endPoint.handler().sendFileToAll(targetFolder, file)
}

fun registerHandler(endPoint: EndPoint) {
    endPoint.eventBus().subscribe<PacketReceivedEvent> {
        if (it.packet.channel == "test") {
            println("received packet from ${it.packet.sender} this it ${endPoint.member().identifier}")
        }
    }

    endPoint.eventBus().subscribe<FileReceivedEvent> {
        val output = File(it.location, it.name)
        output.writeBytes(it.content)
        println("received file!")
    }
}
