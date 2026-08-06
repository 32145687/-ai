package com.myai.assistant.domain.model

/**
 * Represents a memory unit that can be stored and retrieved
 * Used for long-term memory and RAG (Retrieval Augmented Generation)
 */
data class Memory(
    val id: Long = 0,
    val content: String,
    val type: MemoryType,
    val personaId: String? = null, // null means global memory
    val embedding: FloatArray? = null, // Vector embedding for semantic search
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val importance: Float = 0.5f, // 0-1 scale for memory importance
    val accessCount: Int = 0, // How many times this memory was accessed
    val lastAccessedAt: Long? = null,
    val metadata: MemoryMetadata? = null
)

enum class MemoryType {
    CONVERSATION,      // Chat history
    USER_FACT,         // Facts about the user (name, preferences, etc.)
    USER_HABIT,        // User habits and patterns
    LEARNED_KNOWLEDGE, // Knowledge learned from interactions
    TASK_REMINDER,     // Reminder tasks
    EMOTIONAL_STATE,   // Emotional context
    PREFERENCE,        // User preferences
    SKILL,            // Learned skills or capabilities
    RELATIONSHIP      // Relationship context with user
}

data class MemoryMetadata(
    val sourceMessageId: Long? = null,
    val relatedMemories: List<Long> = emptyList(),
    val tags: List<String> = emptyList(),
    val confidence: Float = 1.0f,
    val expirationTime: Long? = null // For temporary memories
)

/**
 * Represents a reminder/task
 */
data class Reminder(
    val id: Long = 0,
    val title: String,
    val description: String,
    val scheduledTime: Long,
    val repeatPattern: RepeatPattern? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Priority = Priority.MEDIUM,
    val relatedPersonaId: String? = null,
    val notificationSent: Boolean = false
)

enum class RepeatPattern {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

/**
 * Represents user behavior patterns learned over time
 */
data class UserBehaviorPattern(
    val id: Long = 0,
    val patternType: PatternType,
    val description: String,
    val frequency: Float, // 0-1
    val lastObserved: Long,
    val observationCount: Int = 1,
    val confidence: Float = 0.5f,
    val relatedData: Map<String, String> = emptyMap()
)

enum class PatternType {
    ACTIVE_TIME,           // When user is most active
    COMMUNICATION_STYLE,   // How user prefers to communicate
    TOPIC_INTEREST,        // Topics user cares about
    RESPONSE_PREFERENCE,   // Preferred response length/style
    MOOD_PATTERN,          // Emotional patterns over time
    TASK_COMPLETION,       // How user handles tasks
    LEARNING_STYLE         // How user learns best
}
