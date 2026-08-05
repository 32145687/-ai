package com.myai.assistant.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for LLM chat completion API
 * Compatible with OpenAI's API format
 */
data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String,
    
    @SerializedName("messages")
    val messages: List<MessageDto>,
    
    @SerializedName("temperature")
    val temperature: Float = 0.7f,
    
    @SerializedName("top_p")
    val topP: Float? = null,
    
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    
    @SerializedName("stream")
    val stream: Boolean = false,
    
    @SerializedName("presence_penalty")
    val presencePenalty: Float? = null,
    
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Float? = null,
    
    @SerializedName("system")
    val systemPrompt: String? = null
)

/**
 * Individual message in the conversation
 */
data class MessageDto(
    @SerializedName("role")
    val role: String, // "system", "user", or "assistant"
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("name")
    val name: String? = null
)

/**
 * Response model from LLM chat completion API
 */
data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("object")
    val `object`: String,
    
    @SerializedName("created")
    val created: Long,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("choices")
    val choices: List<Choice>,
    
    @SerializedName("usage")
    val usage: Usage?
)

data class Choice(
    @SerializedName("index")
    val index: Int,
    
    @SerializedName("message")
    val message: MessageDto,
    
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    
    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * Embedding request for generating vector representations
 */
data class EmbeddingRequest(
    @SerializedName("model")
    val model: String,
    
    @SerializedName("input")
    val input: String,
    
    @SerializedName("encoding_format")
    val encodingFormat: String = "float"
)

/**
 * Embedding response
 */
data class EmbeddingResponse(
    @SerializedName("object")
    val `object`: String,
    
    @SerializedName("data")
    val data: List<EmbeddingData>,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("usage")
    val usage: Usage?
)

data class EmbeddingData(
    @SerializedName("object")
    val `object`: String,
    
    @SerializedName("embedding")
    val embedding: List<Float>,
    
    @SerializedName("index")
    val index: Int
)
