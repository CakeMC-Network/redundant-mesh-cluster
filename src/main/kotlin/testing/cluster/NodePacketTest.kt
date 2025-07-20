package testing.cluster

import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.networking.EndPoint
import net.cakemc.meshing.redundant.networking.codec.Packet
import net.cakemc.meshing.redundant.networking.codec.PacketType
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

fun main() {
    val endpoint1 = EndPoint.createEndPoint()
    val endpoint2 = EndPoint.createEndPoint()

    endpoint1.eventBus().subscribe<PacketReceivedEvent> { packetReceivedEvent ->
        println("packet got on client 1")
        println(packetReceivedEvent.packet)

        if (packetReceivedEvent.packet.packetType == PacketType.REQUEST) {
            endpoint2.handler().replyToPacketSync(
                packetReceivedEvent.channel, packetReceivedEvent.packet,
                Packet(UUID.randomUUID(),  PacketType.RESPONSE, "client-1", "test", "testing", "RESPONSE")

            )
        }
    }
    endpoint2.eventBus().subscribe<PacketReceivedEvent> { packetReceivedEvent ->
        println("packet got on client 2")
        println(packetReceivedEvent.packet)

        if (packetReceivedEvent.packet.packetType == PacketType.REQUEST) {
            endpoint2.handler().replyToPacketSync(
                packetReceivedEvent.channel, packetReceivedEvent.packet,
                Packet(UUID.randomUUID(),  PacketType.RESPONSE, "client-2", "test", "testing", "RESPONSE")

            )
        }
    }


    thread(start = true, isDaemon = true) { endpoint1.start() }
    thread(start = true, isDaemon = true) { endpoint2.start() }

    Thread.sleep(5000)

    endpoint1.handler().sendToAllSync(
        Packet(UUID.randomUUID(), PacketType.NORMAL, "client-1", "test", "testing", "{}")
    )
    val response = endpoint2.handler().sendPacketMainWithFuture(
        Packet(UUID.randomUUID(), PacketType.REQUEST, "client-2", "test", "testing", "requesting")
    ).syncUninterruptedly(3000, TimeUnit.MILLISECONDS)

    println("RESPONSE: " + response)

    endpoint2.handler().sendToAllSync(
        Packet(UUID.randomUUID(), PacketType.NORMAL, "client-2", "test", "testing", "{}")
    )

    val response2 = endpoint1.handler().sendPacketMainWithFuture(
        Packet(UUID.randomUUID(), PacketType.REQUEST, "client-1", "test", "testing", "requesting")
    ).syncUninterruptedly(3000, TimeUnit.MILLISECONDS)

    println("RESPONSE: " + response2)
}