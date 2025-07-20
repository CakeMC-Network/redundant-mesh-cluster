package net.cakemc.meshing.redundant.leader

import io.netty.channel.Channel
import net.cakemc.meshing.redundant.logger.Logger
import net.cakemc.meshing.redundant.networking.EndPoint
import net.cakemc.meshing.redundant.networking.codec.Packet
import java.util.logging.Level

class LeaderElectionSystem(
    val endpoint: EndPoint
) {
    val logger = Logger.getLogger("leader-election")

    fun packetReceived(channel: Channel, packet: Packet) {
        if (packet.channel == "leader_select" && packet.topic == "pick_announced") {
            endpoint.leaderInfo().leaderId = packet.payload
            endpoint.leaderInfo().lastLeaderChange = System.currentTimeMillis()
            logger.log(Level.INFO, "selected new leader ${packet.payload}")
        }
    }

}