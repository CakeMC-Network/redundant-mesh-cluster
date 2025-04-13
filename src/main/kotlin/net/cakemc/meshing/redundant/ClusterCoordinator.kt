package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.status.ClusterReadyBroadcastPacket
import net.cakemc.meshing.redundant.networking.packet.packets.status.NodeJoinAnnouncementPacket
import net.cakemc.skrilla.networking.NetworkingClient

class ClusterCoordinator(
    private val client: NetworkingClient,
    private val peers: List<PeerConfig>,
    private val nodeName: String
) {
    init {
        val expected = peers.map { it.name }.toSet() + nodeName
        ClusterState.setExpectedNodes(expected)
    }

    fun handleJoin(packet: NodeJoinAnnouncementPacket) {
        ClusterState.nodeConnected(packet.nodeName)

        if (ClusterState.allNodesConnected() && !ClusterState.isReady()) {
            broadcastClusterReady()
        }
    }

    private fun broadcastClusterReady() {
        ClusterState.markClusterReady()
        val broadcastPacket = ClusterReadyBroadcastPacket(ClusterState.getConnectedNodes().toList())

        println("[Coordinator] Cluster is fully built. Broadcasting ClusterReady...")

        for (peer in peers) {
            try {
                client.clientHandler.sendPacketSync("main", broadcastPacket)
            } catch (e: Exception) {
                println("[Coordinator] Failed to notify ${peer.name}")
            }
        }
    }
}
