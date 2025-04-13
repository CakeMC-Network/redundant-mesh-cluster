package net.cakemc.meshing.redundant

object ClusterState {
    private val connectedNodes = mutableSetOf<String>()
    private lateinit var expectedNodes: Set<String>
    private var isClusterReady = false

    fun setExpectedNodes(nodes: Set<String>) {
        expectedNodes = nodes
    }

    fun nodeConnected(nodeName: String) {
        connectedNodes += nodeName
        println("Node $nodeName connected. Total connected: ${connectedNodes.size}/${expectedNodes.size}")
    }

    fun allNodesConnected(): Boolean {
        return expectedNodes.all { it in connectedNodes }
    }

    fun getConnectedNodes(): Set<String> = connectedNodes

    fun markClusterReady() {
        isClusterReady = true
    }

    fun isReady(): Boolean = isClusterReady
}
