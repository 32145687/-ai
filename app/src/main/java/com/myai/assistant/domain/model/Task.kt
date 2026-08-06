package com.myai.assistant.domain.model

/**
 * 智能任务模型 - 用于提醒和待办事项
 */
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val scheduledTime: Long, // Timestamp
    val recurrence: Recurrence = Recurrence.NONE,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val relatedPersonaId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notificationSent: Boolean = false
) {
    fun isOverdue(): Boolean = !isCompleted && scheduledTime < System.currentTimeMillis()

    enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }
    enum class Priority { LOW, MEDIUM, HIGH }
}
