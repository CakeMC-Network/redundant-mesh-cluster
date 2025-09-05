import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.networking.EndPoint
import net.cakemc.meshing.redundant.networking.codec.Packet
import net.cakemc.meshing.redundant.networking.codec.PacketType
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ClusterNode(val id: String, private val endpoint: EndPoint) {

    init {
        // Listen for packets
        endpoint.eventBus().subscribe<PacketReceivedEvent> { event ->
            val packet = event.packet

            when (packet.packetType) {
                PacketType.REQUEST -> {
                    if (packet.payload == "PING") {
                        // Respond with PONG
                        endpoint.handler().replyToPacketSync(
                            event.channel, packet,
                            Packet(
                                UUID.randomUUID(), PacketType.RESPONSE,
                                System.currentTimeMillis(), id,
                                "ping", "pong", "PONG"
                            )
                        )
                        println("[$id] Replied with PONG to ${packet.sender}")
                    } else {
                        // Handle other requests
                        endpoint.handler().replyToPacketSync(
                            event.channel, packet,
                            Packet(
                                UUID.randomUUID(), PacketType.RESPONSE,
                                System.currentTimeMillis(), id,
                                "request", "reply", "OK"
                            )
                        )
                    }
                }
                PacketType.RESPONSE -> {
                    println("[$id] Got RESPONSE from ${packet.sender}: ${packet.payload}")
                }
                PacketType.NORMAL -> {
                    println("[$id] Got broadcast: ${packet.payload}")
                }
            }
        }
    }

    fun start() {
        thread(start = true, isDaemon = true) { endpoint.start() }
    }

    fun pingAll() {
        endpoint.handler().sendToAllSync(
            Packet(
                UUID.randomUUID(), PacketType.REQUEST,
                System.currentTimeMillis(), id,
                "ping", "ping", "PING"
            )
        )
        println("[$id] Sent PING to all")
    }

    fun broadcast(message: String) {
        endpoint.handler().sendToAllSync(
            Packet(
                UUID.randomUUID(), PacketType.NORMAL,
                System.currentTimeMillis(), id,
                "broadcast", "msg", message
            )
        )
    }
}

fun main() {
    val endpoint1 = EndPoint.createEndPoint()
    val endpoint2 = EndPoint.createEndPoint()
    val endpoint3 = EndPoint.createEndPoint()

    val node1 = ClusterNode("client-1", endpoint1)
    val node2 = ClusterNode("client-2", endpoint2)
    val node3 = ClusterNode("client-3", endpoint3)

    node1.start()
    node2.start()
    node3.start()

    // Periodically ping all nodes from each node
    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate({
        println("\n=== Cluster round ===")
        node1.pingAll()
        node2.pingAll()
        node3.pingAll()
    }, 2, 5, TimeUnit.SECONDS)

    // Broadcast a demo message later
    Thread.sleep(10000)
    node1.broadcast("Hello from node1!")
    node2.broadcast("Hello from node2!")
    node3.broadcast("Hello from node3!")
}
