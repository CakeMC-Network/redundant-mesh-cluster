package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.event.impl.ClientReadyEvent
import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthStatus
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionRenewalRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionRenewalResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionValidationRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionValidationResponsePacket
import net.cakemc.skrilla.networking.NetworkingClient
import net.cakemc.skrilla.networking.NetworkingServer
import net.cakemc.skrilla.serial.SerializationSystem
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MeshNode(
    val nodeName: String,
    private val port: Int,
    private val peers: List<PeerConfig>
) {

    private val server = NetworkingServer()
    private val client = NetworkingClient()
    private val connectedPeers = ConcurrentHashMap<String, Boolean>()

    private var sessionToken: String = ""

    fun start() {
        registerTypes()
        setupServer()
        setupClient()

        startServer()
        connectToPeers()
    }

    private fun registerTypes() {
        SerializationSystem.registerEnumType(AuthStatus::class.java)
    }

    private fun setupServer() {
      server.eventBus.subscribe<PacketReceivedEvent> { event ->
        val packet = event.packet

        // auth
        if (packet is AuthRequestPacket) {
          println("Authenticating ${packet.username}")
          val response = AuthService.authenticate(packet.username, packet.password)
          server.clientHandler.replyToPacketSync(event.channel, packet, response)
        }

        // session
        if (packet is SessionValidationRequestPacket) {
          val valid = SessionManager.validateSession(packet.token)
          val username = SessionManager.getSessionUser(packet.token)
          val response = SessionValidationResponsePacket(
            isValid = valid,
            username = if (valid) username else null,
            message = if (valid) "Session valid." else "Session invalid or expired."
          )
          server.clientHandler.replyToPacketSync(event.channel, packet, response)
        }

        if (packet is SessionRenewalRequestPacket) {
          val newSession = SessionManager.renewSession(packet.token)
          val response = if (newSession != null) {
            SessionRenewalResponsePacket(
              success = true,
              newExpiration = newSession.expiresAt,
              message = "Session renewed."
            )
          } else {
            SessionRenewalResponsePacket(
              success = false,
              message = "Session expired or invalid."
            )
          }
          server.clientHandler.replyToPacketSync(event.channel, packet, response)
        }

      }
    }

    private fun setupClient() {
      client.eventBus.subscribe<ClientReadyEvent> {
        val future = client.clientHandler.sendPacketWithFuture("main", AuthRequestPacket("test", "test123"))
        val response = future.syncUninterruptedly(2000, TimeUnit.MILLISECONDS)

        if (response is AuthResponsePacket) {
          println("Auth result: ${AuthStatus.values()[response.status]}")
          println("Message: ${response.message}")
          println("Session Token: ${response.sessionToken}")

          // TODO IMPLEMENT LOGIN WITH ACCEPTING DENY CLOSES CLIENT
          // TODO add peer too $connectedPeers!

          sessionToken = response.sessionToken!!
        }
      }

    }

    private fun startServer() {
        thread(start = true, isDaemon = true) {
            server.initialize()
            server.start("0.0.0.0", port)
            println("[$nodeName] Server listening on port $port")
        }
    }

    private fun connectToPeers() {
        thread(start = true, isDaemon = true) {
            client.initialize()
            for (peer in peers) {
                println("[$nodeName] Attempting to connect to ${peer.name} at ${peer.host}:${peer.port}")
                client.connect(peer.host, peer.port)
                Thread.sleep(500) // slight delay between attempts
            }
        }
    }

}