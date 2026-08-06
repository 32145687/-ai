package com.ai.assistant.domain.model

import java.util.Date

/**
 * 智能任务模型
 * @param id 唯一标识
 * @param title 任务标题
 * @param description 详细描述（由LLM提取）
 * @param scheduledTime 计划执行时间
 * @param recurrence 重复规则 (NONE, DAILY, WEEKLY, MONTHLY)
 * @param priority 优先级 (LOW, MEDIUM, HIGH)
 * @param isCompleted 是否完成
 * @param relatedPersonaId 关联的人格ID（可选，某些提醒只对特定人格有效）
 * @param createdAt 创建时间
 */
data class Task(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val scheduledTime: Long, // Timestamp
    val recurrence: Recurrence = Recurrence.NONE,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val relatedPersonaId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isOverdue(): Boolean = !isCompleted && scheduledTime < System.currentTimeMillis()

    enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }
    enum class Priority { LOW, MEDIUM, HIGH }
}

/**
 * 提醒触发事件
 */
data class ReminderEvent(
    val taskId: String,
    val title: String,
    val message: String,
    val triggerTime: Long
)
