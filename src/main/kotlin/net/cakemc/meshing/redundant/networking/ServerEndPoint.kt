package net.cakemc.meshing.redundant.networking

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.distributed.DistributedMap
import net.cakemc.meshing.redundant.distributed.SimpleDistributedMap
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.leader.LeaderElectionSystem
import net.cakemc.meshing.redundant.leader.LeaderSelectionData
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.meshing.redundant.networking.handler.ServerChannelInitializer
import net.cakemc.skrilla.networking.codec.BossHandler
import net.cakemc.skrilla.networking.handler.ConnectionHandler
import java.util.logging.Level

class ServerEndPoint(
    private val host: String,
    private val port: Int,
    private val id: String = "client-${(1000..9999).random()}"
) : EndPoint {
    private val logger = Logger.getLogger("server-endpoint")

    val member: Member = Member(id, Member.MemberAddress(host, port))

    val leaderData: LeaderSelectionData = LeaderSelectionData(
        member.identifier, System.currentTimeMillis()
    )

    lateinit var parent: ParentEndPoint

    val distributedMaps: MutableList<DistributedMap<*, *>> = mutableListOf()

    override fun <Key, Value> map(name: String): DistributedMap<Key, Value> {
        val mapEntry = distributedMaps.stream().filter { it.resolveName().equals(name) }.findFirst().orElse(null)
        if (mapEntry == null) {
            val map = SimpleDistributedMap<Key, Value>(
                this, id, name
            )
            this.distributedMaps.add(map)
            return map
        }

        @Suppress("UNCHECKED_CAST")
        return mapEntry as DistributedMap<Key, Value>;
    }

    /**
     * Returns the boss event loop group for the server.
     *
     * @return the [EventLoopGroup] used for accepting connections
     */
    // JsonContainer
    var bossGroup: EventLoopGroup? = null
        private set

    /**
     * Returns the worker event loop group for the server.
     *
     * @return the [EventLoopGroup] used for processing connections
     */
    var workerGroup: EventLoopGroup? = null
        private set

    /**
     * Returns the class of the server channel being used.
     *
     * @return the [Class] of the server channel
     */
    var channelType: Class<out ServerChannel?>? = null
        private set

    private var channel: Channel? = null

    var eventBus: EventBus
    val connectionHandler: ConnectionHandler
    val electionSystem: LeaderElectionSystem
    val bossHandler: BossHandler

    init {
        val ioHandlerFactory =
            if (EPOLL) (if (KQUEUE) KQueueIoHandler.newFactory()
            else EpollIoHandler.newFactory()) else NioIoHandler.newFactory()

        this.bossGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
        this.workerGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)

        this.channelType =
            if (EPOLL) (if (KQUEUE) KQueueServerSocketChannel::class.java
            else EpollServerSocketChannel::class.java) else NioServerSocketChannel::class.java

        this.eventBus = EventBus()
        this.connectionHandler = ConnectionHandler(
            eventBus, member, EndpointType.SERVER
        )
        this.electionSystem = LeaderElectionSystem(this)

        this.bossHandler = BossHandler(
            this, member, connectionHandler,
            electionSystem, eventBus, EndpointType.SERVER
        )
    }

    override fun start() {
        try {
            val bootstrap = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(channelType)

                .childOption(ChannelOption.IP_TOS, 24)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) // Data settings

                .childOption(ChannelOption.WRITE_SPIN_COUNT, 1)

                .option(ChannelOption.SO_REUSEADDR, true)

                .childHandler(ServerChannelInitializer(bossHandler))

            channel = bootstrap.bind(port).sync().channel()
            connectionHandler.registerMainChannel(channel!!)
            logger.log(Level.INFO, "[Server:$port] Started")
            channel?.closeFuture()?.sync()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "[Server:$port] Error starting server: ${e.message}")
        }
    }

    override fun close() {
        logger.log(Level.INFO, "[Server:$port] Shutting down")
        channel?.close()

        bossGroup!!.shutdownGracefully()
        workerGroup!!.shutdownGracefully()
    }

    override fun handler(): ConnectionHandler {
        return connectionHandler
    }

    override fun eventBus(): EventBus {
        return eventBus
    }

    override fun member(): Member {
        return member
    }

    override fun isLeader(): Boolean {
        return this.id.equals(leaderData.leaderId)
    }

    override fun leaderInfo(): LeaderSelectionData {
        return this.leaderData
    }

    override fun parent(): ParentEndPoint {
        return parent
    }

    override fun initializeParent(parent: ParentEndPoint) {
        this.parent = parent
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