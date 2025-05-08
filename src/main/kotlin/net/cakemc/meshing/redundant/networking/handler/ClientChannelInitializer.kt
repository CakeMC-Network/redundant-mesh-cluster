package net.cakemc.meshing.redundant.networking.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.skrilla.networking.codec.BossHandler
import net.cakemc.meshing.redundant.networking.codec.PacketDecoder
import net.cakemc.meshing.redundant.networking.codec.PacketEncoder
import java.util.logging.Level

open class ClientChannelInitializer(
    private val bossHandler: BossHandler,
) : ChannelInitializer<SocketChannel>() {

    private val logger = Logger.getLogger("client-initializer")

    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()

        pipeline.addFirst("decoder", PacketDecoder())
        pipeline.addAfter("decoder", "encoder", PacketEncoder())
        pipeline.addAfter("encoder", "boss", bossHandler)
    }
}
