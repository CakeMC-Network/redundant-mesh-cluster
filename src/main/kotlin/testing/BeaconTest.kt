package testing

import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.networking.Networking
import net.cakemc.meshing.redundant.networking.codec.Packet
import net.cakemc.meshing.redundant.networking.codec.PacketType
import kotlin.concurrent.thread

fun main() {
    val endpoint1 = Networking.createEndPoint()
    val endpoint2 = Networking.createEndPoint()
    val endpoint3 = Networking.createEndPoint()

    endpoint1.eventBus().subscribe<PacketReceivedEvent> { println("received packet from ${it.packet.sender} this it (1)") }
    endpoint2.eventBus().subscribe<PacketReceivedEvent> { println("received packet from ${it.packet.sender} this it (2)") }
    endpoint3.eventBus().subscribe<PacketReceivedEvent> { println("received packet from ${it.packet.sender} this it (3)") }

    thread(start = true, isDaemon = true) { endpoint1.start() }
    thread(start = true, isDaemon = true) { endpoint2.start() }
    thread(start = true, isDaemon = true) { endpoint3.start() }

    Thread.sleep(2000)

    endpoint1.handler().sendToAllSync(
        Packet(PacketType.NORMAL, "client-1", "test", "testing", "{}")
    )
}