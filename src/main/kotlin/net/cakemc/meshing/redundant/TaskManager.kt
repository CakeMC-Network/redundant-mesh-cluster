package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskRequestPacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskStatus
import java.util.concurrent.ConcurrentHashMap

object TaskManager {
    private val tasks = ConcurrentHashMap<String, TaskStatus>()
    private val taskAssignments = ConcurrentHashMap<String, String>() // taskId -> assignedNode

    // Request a task (either by the leader or a node)
    fun requestTask(taskId: String, taskDescription: String): TaskRequestPacket {
        return TaskRequestPacket(taskId, taskDescription)
    }

    // Assign task to a node (only by leader)
    fun assignTask(taskId: String, assignedNode: String) {
        taskAssignments[taskId] = assignedNode
        tasks[taskId] = TaskStatus.IN_PROGRESS
        println("Task $taskId assigned to $assignedNode.")
    }

    // Mark task as completed
    fun completeTask(taskId: String, nodeName: String) {
        tasks[taskId] = TaskStatus.COMPLETED
        println("Task $taskId completed by $nodeName.")
    }

    // Mark task as failed
    fun failTask(taskId: String, nodeName: String) {
        tasks[taskId] = TaskStatus.FAILED
        println("Task $taskId failed on $nodeName.")
    }

    // Get task status
    fun getTaskStatus(taskId: String): TaskStatus? {
        return tasks[taskId]
    }

    // Get the node assigned to the task
    fun getAssignedNode(taskId: String): String? {
        return taskAssignments[taskId]
    }
}
