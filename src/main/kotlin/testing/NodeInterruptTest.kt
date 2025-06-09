package testing

import net.cakemc.meshing.redundant.networking.EndPoint
import kotlin.concurrent.thread

fun main() {
    val endpoint1 = EndPoint.createEndPoint()
    val endpoint2 = EndPoint.createEndPoint()
    val endpoint3 = EndPoint.createEndPoint()
    val endpoint4 = EndPoint.createEndPoint()

    thread(start = true, isDaemon = true) { endpoint1.start() }
    thread(start = true, isDaemon = true) { endpoint2.start() }
    thread(start = true, isDaemon = true) { endpoint3.start() }
    thread(start = true, isDaemon = true) { endpoint4.start() }

    Thread.sleep(5000)
    println("Main: Killing endpoint1")
    endpoint1.close()

    while (true) Thread.sleep(1000)
}
