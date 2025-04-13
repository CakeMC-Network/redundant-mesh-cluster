package testing

import net.cakemc.meshing.redundant.event.impl.ClientReadyEvent
import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.skrilla.networking.NetworkingClient
import net.cakemc.skrilla.networking.NetworkingServer
import net.cakemc.skrilla.networking.packet.PacketType
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthStatus
import net.cakemc.skrilla.serial.SerializationSystem
import java.util.concurrent.TimeUnit

fun main() {
    SerializationSystem.registerEnumType(AuthStatus::class.java)
    SerializationSystem.registerEnumType(PacketType::class.java)

    val server = NetworkingServer()
    server.eventBus.subscribe<PacketReceivedEvent> { packetEvent ->

      if (packetEvent.packet is AuthRequestPacket) {
        server.clientHandler.replyToPacketSync(
          packetEvent.channel,
          packetEvent.packet,
          AuthResponsePacket(AuthStatus.SUCCESS.ordinal)
        )
      }

    }

    Thread.ofVirtual().start({
        server.initialize()
        server.start("0.0.0.0", 2233)
    })

    val client = NetworkingClient()
    client.eventBus.subscribe<ClientReadyEvent> { event ->
        val future = client.clientHandler.sendPacketWithFuture("main", AuthRequestPacket("test", "test"))

        val value = future.syncUninterruptedly(2000, TimeUnit.MILLISECONDS)
        if (value is AuthResponsePacket) {
          println(AuthStatus.values()[value.status])
        }
    }

    Thread.ofVirtual().start({
        client.initialize()
        client.connect("0.0.0.0", 2233)
    })

    while (true) { }

}