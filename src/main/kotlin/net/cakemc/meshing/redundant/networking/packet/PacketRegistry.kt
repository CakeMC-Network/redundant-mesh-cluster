package net.cakemc.skrilla.networking.packet

import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.metrics.LeadershipChangePacket
import net.cakemc.meshing.redundant.networking.packet.packets.metrics.MetricsPacket
import net.cakemc.meshing.redundant.networking.packet.packets.ping.NodeFailureBroadcastPacket
import net.cakemc.meshing.redundant.networking.packet.packets.ping.PingPacket
import net.cakemc.meshing.redundant.networking.packet.packets.ping.PongPacket
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionRenewalRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionRenewalResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionValidationRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.session.SessionValidationResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.status.ClusterReadyBroadcastPacket
import net.cakemc.meshing.redundant.networking.packet.packets.status.ClusterStatusRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.status.NodeJoinAnnouncementPacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.*
import net.cakemc.skrilla.serial.SerializationSystem
import java.util.concurrent.ConcurrentHashMap

class PacketRegistry {

    val serializer = SerializationSystem()
    val packetMap: MutableMap<Int, Class<out Packet>> = ConcurrentHashMap()

    init {
        // AUTH
        registerPacketById(0x0, AuthRequestPacket::class.java)
        registerPacketById(0x1, AuthResponsePacket::class.java)

        // SESSION
        registerPacketById(0x2, SessionRenewalRequestPacket::class.java)
        registerPacketById(0x3, SessionValidationRequestPacket::class.java)
        registerPacketById(0x4, SessionRenewalResponsePacket::class.java)
        registerPacketById(0x5, SessionValidationResponsePacket::class.java)

        // PING
        registerPacketById(0x6, PingPacket::class.java)
        registerPacketById(0x7, NodeFailureBroadcastPacket::class.java)
        registerPacketById(0x8, PongPacket::class.java)

        // METRICS
        registerPacketById(0x9, LeadershipChangePacket::class.java)
        registerPacketById(0x10, MetricsPacket::class.java)

        // STATUS
        registerPacketById(0x11, ClusterReadyBroadcastPacket::class.java)
        registerPacketById(0x12, ClusterStatusRequestPacket::class.java)
        registerPacketById(0x12, NodeJoinAnnouncementPacket::class.java)

        // TASK
        registerPacketById(0x13, TaskAssignmentPacket::class.java)
        registerPacketById(0x14, TaskCompletionPacket::class.java)
        registerPacketById(0x15, TaskQueueAcknowledgePacket::class.java)
        registerPacketById(0x16, TaskQueueRequestPacket::class.java)
        registerPacketById(0x17, TaskQueueResponsePacket::class.java)
        registerPacketById(0x18, TaskRequestPacket::class.java)
        registerPacketById(0x19, TaskStatusPacket::class.java)
    }

    fun registerPacketById(identity: Int, packetClass: Class<out Packet>) {
        packetMap.put(identity, packetClass)
    }

    fun packetIdByClass(clazz: Class<out Packet>): Int {
        var foundPacketId = 0
        packetMap.forEach { packetId, packetClazz ->
            if (packetClazz.name.equals(clazz.name)) {
                foundPacketId = packetId
                return@forEach
            }
        }
        return foundPacketId
    }

    fun createPacketOutOfId(id: Int): Class<out Packet>? {
        return packetMap.getOrDefault(id, null)
    }

}