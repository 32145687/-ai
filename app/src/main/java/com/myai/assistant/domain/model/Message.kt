package com.myai.assistant.domain.model

/**
 * Represents a chat message in the conversation
 */
data class Message(
    val id: Long = 0,
    val content: String,
    val role: MessageRole,
    val personaId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: MessageMetadata? = null,
    val avatarUrl: String? = null
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class MessageMetadata(
    val modelUsed: String? = null,
    val tokensUsed: Int? = null,
    val responseTimeMs: Long? = null,
    val embeddingGenerated: Boolean = false
)
