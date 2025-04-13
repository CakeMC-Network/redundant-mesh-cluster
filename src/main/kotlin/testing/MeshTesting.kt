package testing

import net.cakemc.meshing.redundant.MeshNode
import net.cakemc.meshing.redundant.PeerConfig

fun main() {
  val node1 = MeshNode("Node1", 3001, listOf(
    PeerConfig("Node2", "127.0.0.1", 3002),
    PeerConfig("Node3", "127.0.0.1", 3003)
  ))

  val node2 = MeshNode("Node2", 3002, listOf(
    PeerConfig("Node1", "127.0.0.1", 3001),
    PeerConfig("Node3", "127.0.0.1", 3003)
  ))

  val node3 = MeshNode("Node3", 3003, listOf(
    PeerConfig("Node1", "127.0.0.1", 3001),
    PeerConfig("Node2", "127.0.0.1", 3002)
  ))

  node1.start()
  node2.start()
  node3.start()

  while (true) { Thread.sleep(1000) }
}
