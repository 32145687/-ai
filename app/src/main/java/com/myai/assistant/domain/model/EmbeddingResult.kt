package com.myai.assistant.domain.model

/**
 * 表示一段文本的向量化结果
 */
data class EmbeddingResult(
    val text: String,
    val vector: List<Float>,
    val model: String,
    val usage: EmbeddingUsage
)

data class EmbeddingUsage(
    val promptTokens: Int,
    val totalTokens: Int
)
