package net.cakemc.skrilla.networking.codec

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.event.impl.ClientCloseEvent
import net.cakemc.meshing.redundant.event.impl.ClientConnectEvent
import net.cakemc.meshing.redundant.event.impl.ClientDisconnectEvent
import net.cakemc.meshing.redundant.event.impl.ClientReadyEvent
import net.cakemc.meshing.redundant.event.impl.file.FileReceivedEvent
import net.cakemc.meshing.redundant.leader.LeaderElectionSystem
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.meshing.redundant.networking.EndPoint
import net.cakemc.meshing.redundant.networking.EndpointType
import net.cakemc.meshing.redundant.networking.codec.*
import net.cakemc.skrilla.networking.handler.ConnectionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

@Sharable
class BossHandler(
    val endPoint: EndPoint,
    val member: Member,
    val connectionHandler: ConnectionHandler,
    val electionSystem: LeaderElectionSystem,
    val eventBus: EventBus,
    var type: EndpointType
) : ChannelInboundHandlerAdapter() {

    val logger = Logger.getLogger("boss-handler")


    private val filesInProgress = ConcurrentHashMap<String, FileBuffer>()


    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is Packet) {
            val packet = msg
            val responseId = packet.responseUUID

            if (packet.packetType.equals(PacketType.RESPONSE)) {

                val pending = connectionHandler.pendingPackets.get(responseId)
                if (pending != null) pending.set(packet)
            }

            electionSystem.packetReceived(ctx.channel(), packet)

            connectionHandler.packetReceived(ctx.channel(), packet)

            if (type == EndpointType.SERVER) {

                connectionHandler.sendToAllSync(packet, ctx.channel())
            }
            return
        }

        if (msg is FileTransfer) {
            val transfer = msg

            if (type == EndpointType.SERVER) {
                connectionHandler.sendToAllRawSync(transfer, ctx.channel())
            }

            val calculatedHash = FileTransfer.calculateFileHash(transfer.content)
            if (calculatedHash != transfer.fileHash) {
                logger.log(Level.WARNING, "Hash mismatch for ${transfer.fileName}")
            } else {
                eventBus.publish(FileReceivedEvent(ctx.channel(), transfer.fileName, transfer.fileLocation, transfer.content))
            }
            return
        }
        if (msg is FileChunk) {
            val chunk = msg

            if (type == EndpointType.SERVER) {
                connectionHandler.sendToAllRawSync(chunk, ctx.channel())
            }

            val buffer = filesInProgress.computeIfAbsent(chunk.fileHash) {
                FileBuffer(chunk.totalChunks, ConcurrentHashMap())
            }

            buffer.chunks[chunk.chunkIndex] = chunk.data

            if (buffer.chunks.size == buffer.totalChunks) {
                val data = buffer.chunks.toSortedMap().values.fold(ByteArray(0)) { acc, bytes -> acc + bytes }

                val calculatedHash = FileTransfer.calculateFileHash(data)
                if (calculatedHash != chunk.fileHash) {
                    logger.log(Level.WARNING, "Hash mismatch for ${chunk.fileName}")
                } else {
                    eventBus.publish(FileReceivedEvent(ctx.channel(), chunk.fileName, chunk.fileLocation, data))
                }

                filesInProgress.remove(chunk.fileHash)
            }
        }
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
        eventBus.publish(ClientConnectEvent(channel))

        connectionHandler.sendPacketSync(
            channel.remoteAddress().toString(), Packet(
                PacketType.NORMAL, member.identifier, "leader_select", "pick_announced", member.identifier
            )
        )
    }

    fun channelInactiveServer(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()

        logger.log(Level.INFO, "[Server:${member.address.port}] Client disconnected ${channel.remoteAddress()}")
        connectionHandler.unregisterChannel(
            connectionHandler.getChannelNameByContext(channel) ?: channel.remoteAddress().toString()
        )
        eventBus.publish(ClientDisconnectEvent(channel))
    }

    fun channelActiveClient(ctx: ChannelHandlerContext) {
        logger.log(
            Level.INFO, "[${member.identifier}] Connected to server at ${member.address.host}:${member.address.port}"
        )
        eventBus.publish(ClientReadyEvent(ctx.channel()))
    }

    fun channelInactiveClient(ctx: ChannelHandlerContext) {
        logger.log(
            Level.INFO,
            "[${member.identifier}] disconnected from server at ${member.address.host}:${member.address.port}"
        )
        eventBus.publish(ClientCloseEvent(ctx.channel()))
    }


}