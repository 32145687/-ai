package com.myai.assistant.domain.model

/**
 * 提醒模型 - 对应数据库中的 ReminderEntity
 */
data class Reminder(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val scheduledTime: Long,
    val repeatPattern: RepeatPattern? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Priority = Priority.MEDIUM,
    val relatedPersonaId: String? = null,
    val notificationSent: Boolean = false
) {
    fun isOverdue(): Boolean = !isCompleted && scheduledTime < System.currentTimeMillis()

    enum class RepeatPattern { DAILY, WEEKLY, MONTHLY }
    enum class Priority { LOW, MEDIUM, HIGH }
}
