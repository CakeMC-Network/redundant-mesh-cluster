package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.metrics.LeadershipChangePacket
import net.cakemc.meshing.redundant.networking.packet.packets.metrics.MetricsPacket
import net.cakemc.skrilla.networking.NetworkingClient
import java.util.concurrent.ConcurrentHashMap

class LeaderElectionEngine(
    private val nodeName: String,
    private val peers: List<PeerConfig>,
    private val client: NetworkingClient
) {
    private val metricMap = ConcurrentHashMap<String, MetricsPacket>()
    private var currentLeader: String = nodeName

    fun onMetric(packet: MetricsPacket) {
        metricMap[packet.nodeName] = packet
        evaluateLeader()
    }

    fun onNodeFailure(deadNode: String) {
        metricMap.remove(deadNode)
        if (deadNode == currentLeader) {
            println("[$nodeName] Leader died! Re-electing...")
            evaluateLeader(reason = "Previous leader $deadNode failed")
        }
    }

    private fun evaluateLeader(reason: String = "Score update") {
        val scores = metricMap.mapValues { (_, m) ->
            (1.0 - m.cpuUsage) + (1.0 - m.ramUsage) + (1.0 - m.networkUsage)
        }

        val best = scores.maxByOrNull { it.value }
        if (best != null && best.key != currentLeader) {
            currentLeader = best.key
            broadcastNewLeader(currentLeader, reason)
        }
    }

    private fun broadcastNewLeader(leader: String, reason: String) {
        println("[$nodeName] New leader elected: $leader (reason: $reason)")
        val packet = LeadershipChangePacket(leader, reason)

        for (peer in peers) {
            try {
                client.clientHandler.sendPacketSync("main", packet)
            } catch (_: Exception) { }
        }
    }

    fun getCurrentLeader(): String = currentLeader
}
