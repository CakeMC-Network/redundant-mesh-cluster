package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.metrics.MetricsPacket
import net.cakemc.skrilla.networking.NetworkingClient

class MetricBroadcaster(
  private val nodeName: String,
  private val client: NetworkingClient,
  private val peers: List<PeerConfig>
) {
    fun start() {
        kotlin.concurrent.fixedRateTimer("metrics-$nodeName", daemon = true, period = 5000) {
            val (cpu, ram, net) = MetricsCollector.collect()
            val packet = MetricsPacket(nodeName, cpu, ram, net)

            for (peer in peers) {
                try {
                    client.clientHandler.sendPacketSync("main", packet)
                } catch (_: Exception) { }
            }
        }
    }
}
