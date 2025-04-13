package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskAssignmentPacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskStatus
import net.cakemc.skrilla.networking.NetworkingClient

class LeaderCoordinator(
  private val client: NetworkingClient,
  private val peers: List<PeerConfig>,
  private val nodeName: String
) {

    private val taskManager = TaskManager

    // Assign task to a node
    fun assignTaskToNode(taskId: String, taskDescription: String) {
        val nodeToAssign = selectNodeForTask() // This can be any logic like selecting least loaded node
        taskManager.assignTask(taskId, nodeToAssign)

        // Notify all nodes about the task assignment
        val assignmentPacket = TaskAssignmentPacket(taskId, nodeToAssign)
        notifyAllNodes(assignmentPacket)

        // Optionally, you can also directly notify the assigned node:
        client.clientHandler.sendPacketSync("main", assignmentPacket)
    }

    // Notify all nodes about a task assignment
    private fun notifyAllNodes(packet: TaskAssignmentPacket) {
        for (peer in peers) {
            try {
                client.clientHandler.sendPacketSync("main", packet)
            } catch (e: Exception) {
                println("[$nodeName] Error notifying peer about task assignment.")
            }
        }
    }

    // Select a node to assign a task to (for simplicity, just picking the first available peer)
    private fun selectNodeForTask(): String {
        // In a real scenario, you might pick the node based on load, resource availability, etc.
        return peers.first().name
    }

    // Monitor the completion or failure of tasks
    fun monitorTasks(taskId: String) {
        val status = taskManager.getTaskStatus(taskId)
        if (status == TaskStatus.COMPLETED) {
            println("Task $taskId completed. Handling task completion...")
        } else if (status == TaskStatus.FAILED) {
            println("Task $taskId failed. Reassigning task...")
            assignTaskToNode(taskId, "Reassigning task due to failure")
        }
    }
}
