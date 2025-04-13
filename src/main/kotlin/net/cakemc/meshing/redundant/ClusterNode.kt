package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.status.ClusterReadyBroadcastPacket
import net.cakemc.meshing.redundant.networking.packet.packets.status.NodeJoinAnnouncementPacket
import net.cakemc.skrilla.networking.NetworkingClient

class ClusterNode(
    private val client: NetworkingClient,
    private val nodeName: String
) {
    fun announceSelf() {
        val joinPacket = NodeJoinAnnouncementPacket(nodeName)
        client.clientHandler.sendPacketSync("main", joinPacket)
        println("[$nodeName] Announced to cluster.")
    }

    fun onClusterReady(packet: ClusterReadyBroadcastPacket) {
        println("[$nodeName] Received ClusterReady signal! Connected nodes: ${packet.nodes}")
    }
}
