package net.cakemc.meshing.redundant.networking

import net.cakemc.meshing.redundant.Member
import net.cakemc.meshing.redundant.distributed.DistributedMap
import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.meshing.redundant.leader.LeaderSelectionData
import net.cakemc.skrilla.networking.handler.ConnectionHandler

class ParentEndPoint(
    var wrapped: EndPoint
) : EndPoint {

    override fun start() {
        wrapped.start()
    }

    override fun close() {
        wrapped.close()
    }

    override fun parent(): ParentEndPoint {
        return this
    }

    override fun initializeParent(parent: ParentEndPoint) {
        throw IllegalStateException("can't set parent as a parent endpoint!")
    }

    override fun handler(): ConnectionHandler {
        return wrapped.handler()
    }

    override fun eventBus(): EventBus {
        return wrapped.eventBus()
    }

    override fun member(): Member {
        return wrapped.member()
    }

    override fun isLeader(): Boolean {
        return wrapped.isLeader()
    }

    override fun leaderInfo(): LeaderSelectionData {
        return wrapped.leaderInfo()
    }

    override fun <Key, Value> map(name: String): DistributedMap<Key, Value> {
        return wrapped.map<Key, Value>(name)
    }

}