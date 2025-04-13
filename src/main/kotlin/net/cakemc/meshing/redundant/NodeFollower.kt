package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskAssignmentPacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskCompletionPacket
import net.cakemc.skrilla.networking.NetworkingClient

class NodeFollower(
  private val client: NetworkingClient,
  private val nodeName: String
) {

    // Handle task assignment
    fun handleTaskAssignment(packet: TaskAssignmentPacket) {
        val taskId = packet.taskId
        println("Node $nodeName received task $taskId. Starting task...")
        
        // Simulate task processing (in real-life this could be real work)
        try {
            Thread.sleep(2000) // Simulate work
            taskCompleted(taskId)
        } catch (e: InterruptedException) {
            taskFailed(taskId)
        }
    }

    // Mark task as completed and inform the leader
    private fun taskCompleted(taskId: String) {
        client.clientHandler.sendPacketSync("main", TaskCompletionPacket(taskId, nodeName))
        println("Task $taskId completed by $nodeName.")
    }

    // Mark task as failed and inform the leader
    private fun taskFailed(taskId: String) {
        client.clientHandler.sendPacketSync("main", TaskCompletionPacket(taskId, nodeName))
        println("Task $taskId failed on $nodeName.")
    }
}
