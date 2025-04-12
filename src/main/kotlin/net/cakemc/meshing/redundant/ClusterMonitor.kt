package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.ping.NodeFailureBroadcastPacket
import net.cakemc.meshing.redundant.networking.packet.packets.ping.PingPacket
import net.cakemc.meshing.redundant.networking.packet.packets.ping.PongPacket
import net.cakemc.skrilla.networking.NetworkingClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

class ClusterMonitor(
    private val nodeName: String,
    private val client: NetworkingClient,
    private val peers: List<PeerConfig>,
    private val onNodeFailure: (PeerConfig) -> Unit
) {

    private val failedNodes = ConcurrentHashMap.newKeySet<String>()
    private val heartbeatInterval = 5000L // ms
    private val timeoutThreshold = 2 // misses before marking dead
    private val missedPings = ConcurrentHashMap<String, Int>()

    fun start() {
        fixedRateTimer(name = "heartbeat-$nodeName", daemon = true, initialDelay = 1000, period = heartbeatInterval) {
            for (peer in peers) {
                if (peer.name in failedNodes) continue

                try {
                    val future = client.clientHandler.sendPacketWithFuture("main", PingPacket(from = nodeName))
                    val result = future.syncUninterruptedly(2000, TimeUnit.MILLISECONDS)

                    if (result !is PongPacket) throw Exception("Invalid pong")
                    missedPings[peer.name] = 0
                } catch (e: Exception) {
                    val count = missedPings.getOrDefault(peer.name, 0) + 1
                    missedPings[peer.name] = count

                    if (count >= timeoutThreshold) {
                        println("[$nodeName] Node ${peer.name} considered DOWN")
                        failedNodes.add(peer.name)
                        onNodeFailure(peer)
                        broadcastFailure(peer.name)
                    }
                }
            }
        }
    }

    private fun broadcastFailure(failedNode: String) {
        for (peer in peers) {
            if (peer.name == failedNode || failedNodes.contains(peer.name)) continue
            try {
                client.clientHandler.sendPacketSync("main", NodeFailureBroadcastPacket(failedNode, nodeName))
            } catch (e: Exception) {
                println("[$nodeName] Couldn't notify ${peer.name} about $failedNode")
            }
        }
    }

    fun attemptRecovery(reconnect: (PeerConfig) -> Unit) {
        fixedRateTimer(name = "recovery-$nodeName", daemon = true, initialDelay = 10000, period = 10000) {
            for (peer in peers) {
                if (failedNodes.contains(peer.name)) {
                    println("[$nodeName] Trying to reconnect to ${peer.name}...")
                    try {
                        reconnect(peer)
                        failedNodes.remove(peer.name)
                        missedPings[peer.name] = 0
                        println("[$nodeName] Recovered node ${peer.name}")
                    } catch (_: Exception) {
                        println("[$nodeName] Still can't reach ${peer.name}")
                    }
                }
            }
        }
    }
}
