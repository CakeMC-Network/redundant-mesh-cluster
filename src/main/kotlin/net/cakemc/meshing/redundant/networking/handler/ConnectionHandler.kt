package net.cakemc.skrilla.networking.handler

import io.netty.channel.Channel
import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.networking.EndpointType
import net.cakemc.meshing.redundant.networking.codec.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.name
import kotlin.io.path.readBytes

class ConnectionHandler(
    val eventBus: EventBus,
    val member: Member,
    var type: EndpointType,
) {

    val contextMap: MutableMap<String, Channel> = ConcurrentHashMap()
    val pendingPackets: MutableMap<UUID, PacketFuture> = ConcurrentHashMap()

    fun packetReceived(channel: Channel, packet: Packet) {
        val event = PacketReceivedEvent(channel, packet)
        eventBus.publish(event)
    }

    fun getChannel(name: String): Channel? {
        return contextMap.get(name)
    }

    fun isChannelRegistered(name: String): Boolean {
        return contextMap.containsKey(name)
    }

    fun getChannels(): Set<String> {
        return contextMap.keys
    }

    fun getChannelList(): MutableCollection<Channel> {
        return contextMap.values
    }

    fun registerChannel(name: String, context: Channel) {
        this.contextMap.put(name, context)
    }

    fun registerMainChannel(context: Channel) {
        this.contextMap.put("__main__", context)
    }

    fun unregisterChannel(name: String) {
        this.contextMap.remove(name)
    }

    fun clearChannels() {
        this.contextMap.clear()
    }

    fun getChannelNameByContext(ctx: Channel): String? {
        val entry = this.contextMap.entries.stream().filter { it.value.equals(ctx) }.findFirst().orElse(null)

        if (entry == null) {
            return null
        }
        return entry.key
    }

    fun closeChannel(name: String) {
        if (isChannelRegistered(name)) getChannel(name)!!.close()
    }

    // sending methods

    fun sendPacketMainSync(packet: Packet) {
        if (this.type == EndpointType.SERVER) {
            sendToAllSync(packet)
        } else {
            sendPacketSync("__main__", packet)
        }
    }

    fun sendPacketMainRawSync(packet: Any) {
        if (this.type == EndpointType.SERVER) {
            sendToAllRawSync(packet)
        } else {
            sendPacketRawSync("__main__", packet)
        }
    }

    fun sendPacketMainWithFuture(packet: Packet): PacketFuture {
        if (this.type == EndpointType.SERVER) {
            if (contextMap.isEmpty()) {
                throw IllegalStateException("No clients connected to the server.")
            }

            val packets: MutableList<Packet?> = LinkedList()

            contextMap.forEach { name, channel ->
                try {
                    val futurePacket = sendPacketWithFuture(name, packet).syncUninterruptedly(3, TimeUnit.SECONDS)
                    if (futurePacket == null) {
                        println("[WARN] Response from '$name' was null.")
                    }
                    packets.add(futurePacket)
                } catch (e: Exception) {
                    packets.add(null)
                }
            }

            val packetFuture = PacketFuture()
            packetFuture.set(packets[0])
            return packetFuture
        }

        // Client behavior
        return sendPacketWithFuture("__main__", packet)
    }

    fun sendPacketMainAsync(packet: Packet) {
        if (this.type == EndpointType.SERVER) {
            sendToAllAsync(packet)
        } else {
            sendPacketAsync("__main__", packet)
        }

    }

    fun sendPacketMainAnyAsync(packet: Any) {
        if (this.type == EndpointType.SERVER) {
            sendToAllRawAsync(packet)
        } else {
            sendPacketRawAsync("__main__", packet)
        }

    }

    fun sendPacketMainWithFutureAsync(packet: Packet): PacketFuture {
        return sendPacketWithFutureAsync("__main__", packet)
    }

    fun sendPacketSync(name: String, packet: Packet) {
        if (this.contextMap.containsKey(name)) this.contextMap.get(name)!!.writeAndFlush(packet)
    }

    fun sendPacketRawSync(name: String, packet: Any) {
        if (this.contextMap.containsKey(name)) this.contextMap.get(name)!!.writeAndFlush(packet)
    }

    fun sendPacketWithFuture(name: String, packet: Packet): PacketFuture {
        if (this.contextMap.containsKey(name)) this.contextMap.get(name)!!.writeAndFlush(packet)

        val future = PacketFuture()
        this.pendingPackets.put(packet.responseUUID, future)

        return future
    }

    fun replyToPacketSync(channel: Channel, received: Packet, reply: Packet) {
        val replyId = received.responseUUID

        reply.packetType = PacketType.RESPONSE
        reply.responseUUID = replyId

        channel.writeAndFlush(reply)
    }

    fun sendToAllSync(packet: Packet) {
        this.contextMap.values.forEach { it.writeAndFlush(packet) }
    }

    fun sendToAllSync(packet: Any) {
        this.contextMap.values.forEach { it.writeAndFlush(packet) }
    }

    fun sendToAllSync(packet: Packet, vararg excluded: Channel) {
        this.contextMap.values.forEach {
            if (excluded.contains(it)) return

            it.writeAndFlush(packet)
        }
    }

    fun sendToAllRawSync(packet: Any, vararg excluded: Channel) {
        this.contextMap.values.forEach {
            if (excluded.contains(it)) return

            it.writeAndFlush(packet)
        }
    }

    fun sendPacketAsync(name: String, packet: Packet) {
        if (this.contextMap.containsKey(name)) {
            val context = this.contextMap[name]
            if (context != null) {
                Thread.ofVirtual().start {
                    context.writeAndFlush(packet)
                }
            }
        }
    }

    fun sendPacketRawAsync(name: String, packet: Any) {
        if (this.contextMap.containsKey(name)) {
            val context = this.contextMap[name]
            if (context != null) {
                Thread.ofVirtual().start {
                    context.writeAndFlush(packet)
                }
            }
        }
    }

    fun sendPacketWithFutureAsync(name: String, packet: Packet): PacketFuture {
        val future = PacketFuture()
        // Add the future to pending packets
        this.pendingPackets.put(packet.responseUUID, future)

        if (this.contextMap.containsKey(name)) {
            val context = this.contextMap[name]
            if (context != null) {
                Thread.ofVirtual().start {
                    context.writeAndFlush(packet)
                }
            }
        }

        return future
    }

    fun sendToAllAsync(packet: Packet) {
        this.contextMap.values.forEach { context ->
            Thread.ofVirtual().start {
                context.writeAndFlush(packet)
            }
        }
    }

    fun sendToAllRawAsync(packet: Any) {
        this.contextMap.values.forEach { context ->
            Thread.ofVirtual().start {
                context.writeAndFlush(packet)
            }
        }
    }

    fun replyToPacketAsync(channel: Channel, received: Packet, reply: Packet) {
        val replyId = received.responseUUID
        reply.packetType = PacketType.RESPONSE
        reply.responseUUID = replyId

        Thread.ofVirtual().start {
            channel.writeAndFlush(reply)
        }

    }

    fun sendFileToAll(targetFolder: String, file: Path) {
        val chunkSize = 8192
        val fileHash = FileTransfer.calculateFileHash(file)

        val bytes = file.readBytes()

        val totalChunks = (bytes.size + chunkSize - 1) / chunkSize

        if (totalChunks == 1) {
            sendToAllRawSync(
                FileTransfer(
                    bytes, file.name, targetFolder, fileHash
                )
            )
            return
        }

        for (i in 0 until totalChunks) {
            val from = i * chunkSize
            val to = minOf(from + chunkSize, bytes.size)
            val chunkData = bytes.copyOfRange(from, to)

            val chunk = FileChunk(
                fileName = file.name,
                fileLocation = targetFolder,
                fileHash = fileHash,
                totalChunks = totalChunks,
                chunkIndex = i,
                data = chunkData
            )
            sendToAllRawSync(chunk)
        }
    }

    fun sendFile(name: String, targetFolder: String, file: Path) {
        val chunkSize = 8192
        val fileHash = FileTransfer.calculateFileHash(file)

        val bytes = file.readBytes()

        val totalChunks = (bytes.size + chunkSize - 1) / chunkSize

        if (totalChunks == 1) {
            sendPacketRawSync(name,
                FileTransfer(
                    bytes, file.name, targetFolder, fileHash
                )
            )
            return
        }

        for (i in 0 until totalChunks) {
            val from = i * chunkSize
            val to = minOf(from + chunkSize, bytes.size)
            val chunkData = bytes.copyOfRange(from, to)

            val chunk = FileChunk(
                fileName = file.name,
                fileLocation = targetFolder,
                fileHash = fileHash,
                totalChunks = totalChunks,
                chunkIndex = i,
                data = chunkData
            )
            sendPacketRawSync(name, chunk)
        }
    }

}