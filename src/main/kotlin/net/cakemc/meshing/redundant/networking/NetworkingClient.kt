package net.cakemc.skrilla.networking

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.event.impl.ClientReadyEvent
import net.cakemc.skrilla.networking.codec.BossHandler
import net.cakemc.skrilla.networking.codec.packet.PacketDecoder
import net.cakemc.skrilla.networking.codec.packet.PacketEncoder
import net.cakemc.skrilla.networking.handler.ClientHandler
import net.cakemc.skrilla.networking.packet.PacketRegistry
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class NetworkingClient(
  val eventBus: EventBus = EventBus(),
  val clientHandler: ClientHandler = ClientHandler(eventBus),
  val packetRegistry: PacketRegistry = PacketRegistry()
) {

  private var reconnecting = false
  private var shutdown = false
  private var lastHost: String? = null
  private var lastPort: Int? = null

  var group: EventLoopGroup? = null
    private set

  var channel: Class<out Channel?>? = null
    private set

  var bootstrap: Bootstrap? = null
    private set

  var channelFuture: ChannelFuture? = null
    private set

  val activeChannel: Channel?
    get() = channelFuture?.channel()

  fun initialize() {
    val ioHandlerFactory =
      if (EPOLL) (if (KQUEUE) KQueueIoHandler.newFactory()
      else EpollIoHandler.newFactory()) else NioIoHandler.newFactory()

    this.group = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
    this.channel = if (EPOLL) (if (KQUEUE) KQueueSocketChannel::class.java
    else EpollSocketChannel::class.java) else NioSocketChannel::class.java
  }

  fun connect(host: String, port: Int) {
    this.lastHost = host
    this.lastPort = port

    try {
      val bootstrap = Bootstrap()
        .group(group)
        .channel(channel)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .handler(object : ChannelInitializer<SocketChannel>() {
          override fun initChannel(ch: SocketChannel) {
            val pipeline = ch.pipeline()
            pipeline.addLast(
              PacketDecoder(packetRegistry),
              PacketEncoder(packetRegistry),
              BossHandler(clientHandler, eventBus)
            )
          }
        })

      this.bootstrap = bootstrap

      val future = bootstrap.connect(host, port).sync()

      if (future.channel() != null) {
        clientHandler.registerChannel("main", future.channel())

        eventBus.publish(ClientReadyEvent(activeChannel!!))
      }

      thread(isDaemon = true, name = "reconnect-watcher") {
        future.channel().closeFuture().sync()
        if (!shutdown) {
          attemptReconnect()
        }
      }

    } catch (e: Exception) {
      attemptReconnect()
    }
  }

  private fun attemptReconnect() {
    if (reconnecting || shutdown) return
    reconnecting = true

    val executor = Executors.newSingleThreadScheduledExecutor()
    executor.scheduleAtFixedRate({
      if (shutdown) {
        executor.shutdown()
        return@scheduleAtFixedRate
      }

      try {
        val future = bootstrap!!.connect(lastHost, lastPort!!).sync()
        if (future.isSuccess) {
          channelFuture = future

          if (activeChannel != null) {
            clientHandler.registerChannel("main", activeChannel!!)
            eventBus.publish(ClientReadyEvent(activeChannel!!))
          }

          reconnecting = false
          executor.shutdown()
        }
      } catch (e: Exception) {
        println("Reconnect failed: ${e.message}")
      }
    }, 5, 10, TimeUnit.SECONDS)
  }

  fun disconnect() {
    shutdown = true
    channelFuture?.channel()?.close()?.syncUninterruptibly()
    group?.shutdownGracefully()?.syncUninterruptibly()
  }

  companion object {
    val EPOLL: Boolean = Epoll.isAvailable()
    val KQUEUE: Boolean = KQueue.isAvailable()
  }
}
