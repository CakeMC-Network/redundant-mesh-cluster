package testing.cluster

import net.cakemc.meshing.redundant.networking.EndPoint
import kotlin.concurrent.thread

fun main() {
    var endpoint1 = EndPoint.createEndPoint(identifier = "node-1")
    val endpoint2 = EndPoint.createEndPoint(identifier = "node-2")
    val endpoint3 = EndPoint.createEndPoint(identifier = "node-3")
    val endpoint4 = EndPoint.createEndPoint(identifier = "node-4")

    thread(start = true, isDaemon = true) { endpoint1.start() }
    thread(start = true, isDaemon = true) { endpoint2.start() }
    thread(start = true, isDaemon = true) { endpoint3.start() }
    thread(start = true, isDaemon = true) { endpoint4.start() }

    Thread.sleep(5000)
    println("Main: Killing endpoint1")
    endpoint1.close()

    Thread.sleep(10000)
    dumpLeaderInfo(endpoint2, endpoint3, endpoint4)

    Thread.sleep(5000)
    println("Main: Restarting Node 1")

    endpoint1 = EndPoint.createEndPoint(identifier = "node-1")
    thread(start = true, isDaemon = true) { endpoint1.start() }

    Thread.sleep(10000)

    dumpLeaderInfo(endpoint1, endpoint2, endpoint3, endpoint4)

    while (true) Thread.sleep(1000)
}

fun dumpLeaderInfo(vararg member: EndPoint) {
    member.forEach {
        println("leader of ${it.member().identifier} is ${it.leaderInfo().leaderId} i am the leader? ${it.isLeader()}")
    }
}