package net.cakemc.meshing.redundant.leading

import net.cakemc.meshing.redundant.PeerConfig
import net.cakemc.meshing.redundant.task.TaskQueueManager
import net.cakemc.meshing.redundant.networking.packet.packets.task.*
import net.cakemc.skrilla.networking.NetworkingClient
import java.util.concurrent.TimeUnit

class LeaderCoordinatorWithQueue(
    private val client: NetworkingClient,
    private val peers: List<PeerConfig>,
    private val nodeName: String
) {
    private val taskQueueManager = TaskQueueManager

    // Periodically assign tasks from the queue
    fun assignTasksFromQueue() {
        while (TaskQueueManager.hasPendingTasks()) {
            for (peer in peers) {
                val responsePacket = client.clientHandler.sendPacketWithFuture(
                    "main", TaskQueueRequestPacket(nodeName)
                ).syncUninterruptedly(2000, TimeUnit.MILLISECONDS) as TaskQueueResponsePacket

                if (responsePacket.taskId != null) {
                    val taskId = responsePacket.taskId
                    TaskQueueManager.acknowledgeTaskCompletion(taskId, peer.name, TaskStatus.IN_PROGRESS)

                    // Notify the node about the assigned task
                    val taskAssignmentPacket = TaskAssignmentPacket(taskId, peer.name)
                    client.clientHandler.sendPacketSync("main", taskAssignmentPacket)

                    println("Leader assigned task $taskId to ${peer.name}.")
                }
            }
        }
    }

    // Notify all nodes about task assignment and completion
    fun notifyTaskCompletion(taskId: String, status: TaskStatus) {
        for (peer in peers) {
            try {
                client.clientHandler.sendPacketSync("main", TaskQueueAcknowledgePacket(taskId, peer.name, status))
            } catch (e: Exception) {
                println("[$nodeName] Error notifying peer about task completion.")
            }
        }
    }
}
