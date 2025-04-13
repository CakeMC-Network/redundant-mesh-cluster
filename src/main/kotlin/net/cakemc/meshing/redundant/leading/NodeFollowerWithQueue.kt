package net.cakemc.meshing.redundant.leading

import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskQueueAcknowledgePacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskQueueRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskQueueResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskStatus
import net.cakemc.skrilla.networking.NetworkingClient
import java.util.concurrent.TimeUnit

class NodeFollowerWithQueue(
  private val client: NetworkingClient,
  private val nodeName: String
) {
    // Request tasks from the queue
    fun requestTaskFromQueue() {
        val taskRequestPacket = TaskQueueRequestPacket(nodeName)
        val response = client.clientHandler.sendPacketWithFuture("main", taskRequestPacket)
            .syncUninterruptedly(2000, TimeUnit.MILLISECONDS) as TaskQueueResponsePacket

        if (response.taskId != null) {
            val taskId = response.taskId
            println("Node $nodeName received task $taskId: ${response.taskDescription}")
            processTask(taskId)
        } else {
            println("Node $nodeName: No tasks in the queue.")
        }
    }

    // Process the task (simulating task work)
    private fun processTask(taskId: String) {
        try {
            // Simulating task work (e.g., processing data)
            Thread.sleep(3000) // Simulate work
            taskCompleted(taskId)
        } catch (e: InterruptedException) {
            taskFailed(taskId)
        }
    }

    // Acknowledge task completion
    private fun taskCompleted(taskId: String) {
        client.clientHandler.sendPacketSync("main", TaskQueueAcknowledgePacket(taskId, nodeName, TaskStatus.COMPLETED))
        println("Node $nodeName completed task $taskId.")
    }

    // Acknowledge task failure
    private fun taskFailed(taskId: String) {
        client.clientHandler.sendPacketSync("main", TaskQueueAcknowledgePacket(taskId, nodeName, TaskStatus.FAILED))
        println("Node $nodeName failed task $taskId.")
    }
}
