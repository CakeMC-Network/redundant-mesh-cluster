package net.cakemc.meshing.redundant.networking

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioSocketChannel
import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.meshing.redundant.networking.handler.ClientChannelInitializer
import net.cakemc.skrilla.networking.codec.BossHandler
import net.cakemc.skrilla.networking.handler.ConnectionHandler
import java.util.logging.Level
import kotlin.concurrent.thread
import kotlin.math.pow

class ClientEndPoint(
    private val host: String,
    private val port: Int,
    private val id: String = "client-${(1000..9999).random()}"
) : EndPoint {

    private val logger = Logger.getLogger("client-endpoint")

    val member: Member = Member(id, Member.MemberAddress(host, port))

    var group: EventLoopGroup? = null
        private set

    var channelType: Class<out Channel?>? = null
        private set

    var channel: Channel? = null

    val eventBus: EventBus
    val connectionHandler: ConnectionHandler
    val bossHandler: BossHandler

    @Volatile
    private var running = true
    private var reconnectAttempts = 0

    init {
        val ioHandlerFactory =
            if (EPOLL) (if (KQUEUE) KQueueIoHandler.newFactory()
            else EpollIoHandler.newFactory()) else NioIoHandler.newFactory()

        this.group = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
        this.channelType = if (EPOLL) (if (KQUEUE) KQueueSocketChannel::class.java
        else EpollSocketChannel::class.java) else NioSocketChannel::class.java

        this.eventBus = EventBus()
        this.connectionHandler = ConnectionHandler(
            eventBus, member, EndpointType.CLIENT
        )

        this.bossHandler = BossHandler(
            member, connectionHandler, eventBus,
            EndpointType.CLIENT
        )
    }

    override fun start() {
        thread(name = "reconnect-$id", isDaemon = true) {
            while (running) {
                try {
                    val bootstrap = Bootstrap()
                    bootstrap.group(group)
                        .channel(NioSocketChannel::class.java)
                        .option(ChannelOption.SO_KEEPALIVE, true)

                        .option(ChannelOption.SO_REUSEADDR, false)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)

                        .handler(
                            ClientChannelInitializer(
                                bossHandler,
                            )
                        )

                    val future = bootstrap.connect(host, port).sync()

                    channel = future.channel()
                    connectionHandler.registerMainChannel(channel!!)

                    channel?.closeFuture()?.sync()
                    logger.log(Level.INFO, "[$id] Disconnected from server")

                    // If disconnected, retry the connection
                    attemptReconnection()
                } catch (ex: Exception) {
                    logger.log(Level.WARNING, "[$id] Failed to connect: ${ex.message}")
                    attemptReconnection()
                }
            }
        }
    }

    private fun attemptReconnection() {
        if (!running) return

        // Exponential backoff (max 10 retries, delay doubles each time)
        if (reconnectAttempts < 10) {
            reconnectAttempts++
            val delay = (2.0.pow(reconnectAttempts)).toLong() // Exponential backoff
            logger.log(Level.INFO, "[$id] Reconnecting in $delay seconds...")
            Thread.sleep(delay * 1000) // Sleep before reconnecting

            if (!Networking.isServerRunning(port)) {
                logger.log(Level.INFO, "[$id] Trying to promote to server")
                Networking.promoteToServer(port)
            }
        } else {
            logger.log(Level.WARNING, "[$id] Reconnect attempts exceeded. Closing client.")
            close()
        }
    }

    override fun close() {
        running = false
        logger.log(Level.INFO, "[$id] Closing")
        channel?.close()

        group!!.shutdownGracefully()
    }

    override fun handler(): ConnectionHandler {
        return connectionHandler
    }

    override fun eventBus(): EventBus {
        return eventBus
    }

    companion object {
        /**
         * Indicates whether Epoll is available for use.
         */
        val EPOLL: Boolean = Epoll.isAvailable()

        /**
         * Indicates whether KQueue is available for use.
         */
        val KQUEUE: Boolean = KQueue.isAvailable()
    }
}