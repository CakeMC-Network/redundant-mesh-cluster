package net.cakemc.meshing.redundant.networking

import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.distributed.DistributedMap
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.skrilla.networking.handler.ConnectionHandler
import java.net.ServerSocket
import java.util.logging.Level
import kotlin.concurrent.thread

interface EndPoint {
    fun start()
    fun close()
    fun handler(): ConnectionHandler
    fun eventBus(): EventBus
    fun member(): Member

    fun <Key, Value> map(name: String): DistributedMap<Key, Value>

    companion object {
        private val logger = Logger.getLogger("network-manager")

        @Volatile
        private var activeEndpoint: EndPoint? = null

        private var isServerCreated = false

        fun createEndPointAndConnect(port: Int = 5000, host: String = "127.0.0.1"): EndPoint {
            val endPoint = createEndPoint(port, host)
            thread(start = true, isDaemon = true) { endPoint.start() }

            return endPoint
        }

        fun createEndPoint(port: Int = 5000, host: String = "127.0.0.1"): EndPoint {
            val endpoint: EndPoint = if (isPortAvailable(port) && !isServerCreated) {
                val serverEndpoint = ServerEndPoint(host, port)
                activeEndpoint = serverEndpoint
                isServerCreated = true
                serverEndpoint
            } else {
                val clientEndpoint = ClientEndPoint(host, port)
                activeEndpoint = clientEndpoint
                clientEndpoint
            }

            return endpoint
        }

        fun promoteToServer(port: Int, host: String = "127.0.0.1") {
            if (isPortAvailable(port)) {
                logger.log(Level.INFO, "[Promote] No active server. Creating one.")
                val server = ServerEndPoint(host, port)
                activeEndpoint?.close()
                activeEndpoint = server
                thread(start = true, isDaemon = true) { server.start() }
            }
        }

        fun replaceActiveEndpoint(newEndpoint: EndPoint) {
            activeEndpoint?.close()
            activeEndpoint = newEndpoint
        }

        fun isPortAvailable(port: Int): Boolean {
            return try {
                ServerSocket(port).use { true }
            } catch (e: Exception) {
                false
            }
        }

        fun isServerRunning(port: Int): Boolean = !isPortAvailable(port)
    }
}

