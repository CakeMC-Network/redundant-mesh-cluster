package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskQueueResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.task.TaskStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object TaskQueueManager {
    private val taskQueue = ConcurrentLinkedQueue<Pair<String, String>>() // taskId -> taskDescription
    private val taskStatusMap = ConcurrentHashMap<String, TaskStatus>() // taskId -> TaskStatus
    private val taskAssignments = ConcurrentHashMap<String, String>() // taskId -> assignedNode

    // Enqueue a new task
    fun enqueueTask(taskId: String, taskDescription: String) {
        taskQueue.add(Pair(taskId, taskDescription))
        taskStatusMap[taskId] = TaskStatus.PENDING
        println("Task $taskId added to the queue.")
    }

    // Dequeue the next task (for node to request)
    fun dequeueTask(nodeName: String): TaskQueueResponsePacket {
        val task = taskQueue.poll()
        if (task != null) {
            taskAssignments[task.first] = nodeName
            taskStatusMap[task.first] = TaskStatus.IN_PROGRESS
            return TaskQueueResponsePacket(task.first, task.second)
        } else {
            return TaskQueueResponsePacket(null, null) // No task available
        }
    }

    // Acknowledge task completion or failure
    fun acknowledgeTaskCompletion(taskId: String, nodeName: String, status: TaskStatus) {
        taskStatusMap[taskId] = status
        taskAssignments.remove(taskId) // Task completed, remove from assignment
        println("Task $taskId completed by $nodeName with status $status.")
    }

    // Check task status
    fun getTaskStatus(taskId: String): TaskStatus? {
        return taskStatusMap[taskId]
    }

    // Check if there are tasks in the queue
    fun hasPendingTasks(): Boolean {
        return taskQueue.isNotEmpty()
    }

    fun getAllTaskInfo(): List<Map<String, String?>> {
        return taskStatusMap.map { (id, status) ->
            mapOf(
                "taskId" to id,
                "status" to status.toString(),
                "assignedNode" to taskAssignments[id]
            )
        }
    }

}
