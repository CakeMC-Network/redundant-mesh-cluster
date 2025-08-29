package net.cakemc.meshing.redundant.networking

import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.distributed.DistributedMap
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.leader.LeaderSelectionData
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

    fun isLeader(): Boolean
    fun leaderInfo(): LeaderSelectionData

    fun parent(): ParentEndPoint
    fun initializeParent(parent: ParentEndPoint)

    fun <Key, Value> map(name: String): DistributedMap<Key, Value>

    companion object {
        private val logger = Logger.getLogger("network-manager")

        private var isServerCreated = false

        fun createEndPointAndConnect(
            identifier: String = "client-${(1000..9999).random()}",
            port: Int = 5000,
            host: String = "127.0.0.1"
        ): EndPoint {
            val endPoint = createEndPoint(identifier, port, host)
            thread(start = true, isDaemon = true) { endPoint.start() }

            return endPoint
        }

        fun createEndPoint(
            identifier: String = "client-${(1000..9999).random()}",
            port: Int = 5000,
            host: String = "127.0.0.1"
        ): EndPoint {
            val endpoint: EndPoint = if (isPortAvailable(port) && !isServerCreated) {
                val serverEndpoint: EndPoint = ServerEndPoint(host, port, identifier)
                val wrappedEndPoint = ParentEndPoint(serverEndpoint)
                serverEndpoint.initializeParent(wrappedEndPoint)
                isServerCreated = true

                wrappedEndPoint
            } else {
                val clientEndpoint: EndPoint = ClientEndPoint(host, port, identifier)
                val wrappedEndPoint = ParentEndPoint(clientEndpoint)
                clientEndpoint.initializeParent(wrappedEndPoint)

                wrappedEndPoint
            }

            return endpoint
        }

        fun promoteToServer(
            parent: ParentEndPoint,
            identifier: String,
            port: Int,
            host: String = "127.0.0.1"
        ) {
            if (isPortAvailable(port)) {
                logger.log(Level.INFO, "[Promote] No active server. Creating one.")
                val server = ServerEndPoint(host, port, identifier)
                parent.close()

                // TODO COPY OVER HANDLER, EVENTBUS and so on

                parent.wrapped = server
                thread(start = true, isDaemon = true) { server.start() }
            }
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

