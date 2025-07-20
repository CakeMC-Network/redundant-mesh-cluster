package net.cakemc.meshing.redundant.networking.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.meshing.redundant.networking.codec.*
import net.cakemc.skrilla.networking.codec.BossHandler
import java.util.logging.Level

open class ClientChannelInitializer(
    private val bossHandler: BossHandler,
) : ChannelInitializer<SocketChannel>() {

    private val logger = Logger.getLogger("client-initializer")

    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()

        pipeline.addFirst("decoder", DiscriminatorDecoder())
        pipeline.addAfter("decoder", "packet_encoder", PacketEncoder())
        pipeline.addAfter("packet_encoder", "file_encoder", FileTransferEncoder())
        pipeline.addAfter("file_encoder", "file_chunk_encoder", FileChunkEncoder())

        pipeline.addAfter("file_chunk_encoder", "boss", bossHandler)
    }
}
