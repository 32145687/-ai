package com.myai.assistant.data.remote.api

import com.myai.assistant.data.remote.model.ChatCompletionRequest
import com.myai.assistant.data.remote.model.ChatCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interface for calling cloud-based LLM APIs
 * Supports multiple providers (OpenAI, Claude, etc.)
 */
interface LlmApiService {
    
    /**
     * OpenAI-compatible chat completion endpoint
     */
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequest,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json"
    ): Response<ChatCompletionResponse>

    /**
     * Alternative endpoint for other providers
     */
    @POST("chat/completions")
    suspend fun createChatCompletionGeneric(
        @Body request: ChatCompletionRequest,
        @Header("Authorization") authorization: String,
        @Header("api-key") apiKey: String? = null,
        @Header("Content-Type") contentType: String = "application/json"
    ): Response<ChatCompletionResponse>
    
    /**
     * OpenAI-compatible embeddings endpoint
     */
    @POST("v1/embeddings")
    suspend fun createEmbedding(
        @Body request: EmbeddingRequest,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json"
    ): Response<EmbeddingResponse>
}
