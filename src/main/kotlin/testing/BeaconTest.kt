package testing

import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.networking.EndPoint
import net.cakemc.meshing.redundant.networking.codec.Packet
import net.cakemc.meshing.redundant.networking.codec.PacketType
import kotlin.concurrent.thread

fun main() {
    val endpoint1 = EndPoint.createEndPoint()
    val endpoint2 = EndPoint.createEndPoint()
    val endpoint3 = EndPoint.createEndPoint()

    endpoint1.eventBus().subscribe<PacketReceivedEvent> { println("received packet from ${it.packet.sender} this it (1)") }
    endpoint2.eventBus().subscribe<PacketReceivedEvent> { println("received packet from ${it.packet.sender} this it (2)") }
    endpoint3.eventBus().subscribe<PacketReceivedEvent> { println("received packet from ${it.packet.sender} this it (3)") }

    thread(start = true, isDaemon = true) { endpoint1.start() }
    thread(start = true, isDaemon = true) { endpoint2.start() }
    thread(start = true, isDaemon = true) { endpoint3.start() }

    Thread.sleep(2000)

    endpoint2.handler().sendToAllSync(
        Packet(PacketType.NORMAL, "channel-2", "test", "testing", "{}")
    )
}