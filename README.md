# Redundant Mesh Cluster

>A self-repairing and mutating mesh-based cluster system built with Netty and Kotlin.

## Overview

This project implements a dynamic cluster where each node can operate as either a client or a server depending on network conditions. If the main server node goes offline, the cluster autonomously promotes one of the remaining nodes to take its place, maintaining connectivity and system integrity.

### Key Features

- ⚙️ Self-healing: Nodes detect server failure and elect a new one.
- 🔁 Auto-Reconnect: Disconnected clients automatically attempt to rejoin the cluster.
- 🧠 Smart Role Assignment: Nodes dynamically decide to act as server or client based on availability.
- 📦 Packet-Based Communication: Uses Netty channels and custom codecs for fast, structured messaging.
- 🧵 Multi-threaded: Built for concurrent networking and failover logic.
- 🎟️ Event-Driven: many out of the box events based on Client/Server behavior.

## Getting Started

```kotlin
val endpoint = Networking.createEndPoint()
endpoint.start()
endpoint.sendPacket(packet)
````

>Kill any node — the cluster will adapt and restore itself.

## Self Repair Example:
```kotlin
val endpoint1 = Networking.createEndPoint()
val endpoint2 = Networking.createEndPoint()
val endpoint3 = Networking.createEndPoint()
val endpoint4 = Networking.createEndPoint()

thread(start = true, isDaemon = true) { endpoint1.start() }
thread(start = true, isDaemon = true) { endpoint2.start() }
thread(start = true, isDaemon = true) { endpoint3.start() }
thread(start = true, isDaemon = true) { endpoint4.start() }

Thread.sleep(5000)
println("Main: Killing endpoint1")
endpoint1.close()
```

## Event Example:
>Basic example using a Dummy Event:
```kotlin
class Event(val message: String)

fun main() {
  val eventBus = EventBus()

  eventBus.subscribe<Event> { event ->
    println(event.message)
  }

  eventBus.publish(Event("TEST"))
}
```
>Networking example using events:
```kotlin
    val endpoint1 = Networking.createEndPoint()
val endpoint2 = Networking.createEndPoint()

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
```

---

> 🔗 Built for resilience. Designed for autonomy.  
> If one falls, another rises. Welcome to the future of decentralized networking.

Made with ❤️ by the CakeMC team.
