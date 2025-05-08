package net.cakemc.skrilla.networking.codec

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.event.impl.ClientCloseEvent
import net.cakemc.meshing.redundant.event.impl.ClientConnectEvent
import net.cakemc.meshing.redundant.event.impl.ClientDisconnectEvent
import net.cakemc.meshing.redundant.event.impl.ClientReadyEvent
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.meshing.redundant.networking.EndpointType
import net.cakemc.skrilla.networking.handler.ConnectionHandler
import net.cakemc.meshing.redundant.networking.codec.Packet
import net.cakemc.meshing.redundant.networking.codec.PacketType
import java.util.logging.Level

@Sharable
class BossHandler(
    val member: Member,
    val connectionHandler: ConnectionHandler,
    val eventBus: EventBus,
    var type: EndpointType
) : SimpleChannelInboundHandler<Packet>() {

    val logger = Logger.getLogger("boos-handler")

    override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet) {
        val responseId = packet.responseUUID

        if (packet.packetType.equals(PacketType.RESPONSE.ordinal)) {

            val pending = connectionHandler.pendingPackets.get(responseId)
            if (pending != null)
                pending.set(packet)
        }

        connectionHandler.packetReceived(ctx.channel(), packet)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        when (type) {
            EndpointType.CLIENT -> channelInactiveClient(ctx)
            EndpointType.SERVER -> channelInactiveServer(ctx)
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        when (type) {
            EndpointType.CLIENT -> channelActiveClient(ctx)
            EndpointType.SERVER -> channelActiveServer(ctx)
        }
    }

    fun channelActiveServer(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()

        logger.log(Level.INFO, "[Server:${member.address.port}] Client connected: ${channel.remoteAddress()}")
        connectionHandler.registerChannel(channel.remoteAddress().toString(), channel)
        eventBus.publish(ClientDisconnectEvent(channel))
    }

    fun channelInactiveServer(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()

        logger.log(Level.INFO, "[Server:${member.address.port}] Client disconnected ${channel.remoteAddress()}")
        connectionHandler.unregisterChannel(connectionHandler.getChannelNameByContext(channel)?: channel.remoteAddress().toString())
        eventBus.publish(ClientConnectEvent(channel))
    }

    fun channelActiveClient(ctx: ChannelHandlerContext) {
        logger.log(Level.INFO, "[${member.identifier}] Connected to server at ${member.address.host}:${member.address.port}")
        eventBus.publish(ClientReadyEvent(ctx!!.channel()))
    }

    fun channelInactiveClient(ctx: ChannelHandlerContext) {
        logger.log(Level.INFO, "[${member.identifier}] disconnected from server at ${member.address.host}:${member.address.port}")
        eventBus.publish(ClientCloseEvent(ctx!!.channel()))
    }


}