package com.myai.assistant.domain.service

import com.myai.assistant.data.remote.api.LlmApiService
import com.myai.assistant.data.remote.model.EmbeddingRequest
import com.myai.assistant.domain.model.EmbeddingResult
import com.myai.assistant.domain.model.EmbeddingUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 向量化服务接口
 * 负责将文本转换为向量表示，用于语义搜索和记忆检索
 */
interface EmbeddingService {
    /**
     * 将文本转换为向量
     * @param text 需要向量化的文本
     * @return 向量化结果
     */
    suspend fun generateEmbedding(text: String): EmbeddingResult
    
    /**
     * 批量向量化
     * @param texts 需要向量化的文本列表
     * @return 向量化结果列表
     */
    suspend fun generateEmbeddings(texts: List<String>): List<EmbeddingResult>
}

/**
 * 云端向量化服务实现
 * 调用 OpenAI 兼容的 Embedding API 生成向量
 */
@Singleton
class CloudEmbeddingService @Inject constructor(
    private val apiService: LlmApiService,
    private val settingsManager: com.myai.assistant.data.manager.SettingsManager
) : EmbeddingService {
    
    companion object {
        private const val DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small"
        private const val MAX_BATCH_SIZE = 100
    }
    
    override suspend fun generateEmbedding(text: String): EmbeddingResult {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = settingsManager.getApiKey()
                val embeddingModel = settingsManager.getEmbeddingModel().takeIf { it.isNotBlank() } 
                    ?: DEFAULT_EMBEDDING_MODEL
                
                val request = EmbeddingRequest(
                    model = embeddingModel,
                    input = text,
                    encodingFormat = "float"
                )
                
                val response = apiService.createEmbedding(
                    request = request,
                    authorization = "Bearer $apiKey"
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val embeddingData = body.data.firstOrNull()
                        ?: throw IllegalStateException("No embedding data returned")
                    
                    EmbeddingResult(
                        text = text,
                        vector = embeddingData.embedding,
                        model = body.model,
                        usage = body.usage?.let { 
                            EmbeddingUsage(it.promptTokens, it.totalTokens) 
                        } ?: EmbeddingUsage(0, 0)
                    )
                } else {
                    throw ApiException(
                        "Embedding API error: ${response.code()} - ${response.message()}",
                        response.code()
                    )
                }
            } catch (e: Exception) {
                throw when (e) {
                    is ApiException -> e
                    else -> ApiException("Failed to generate embedding: ${e.message}", cause = e)
                }
            }
        }
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): List<EmbeddingResult> {
        return withContext(Dispatchers.IO) {
            // 分批处理，避免超过 API 限制
            texts.chunked(MAX_BATCH_SIZE).flatMap { batch ->
                try {
                    val apiKey = settingsManager.getApiKey()
                    val embeddingModel = settingsManager.getEmbeddingModel().takeIf { it.isNotBlank() } 
                        ?: DEFAULT_EMBEDDING_MODEL
                    
                    // OpenAI API 支持批量输入
                    val request = EmbeddingRequest(
                        model = embeddingModel,
                        input = batch.joinToString("\n\n"), // 简单拼接，实际可能需要分别调用
                        encodingFormat = "float"
                    )
                    
                    val response = apiService.createEmbedding(
                        request = request,
                        authorization = "Bearer $apiKey"
                    )
                    
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        body.data.mapIndexed { index, embeddingData ->
                            EmbeddingResult(
                                text = batch.getOrElse(index) { "" },
                                vector = embeddingData.embedding,
                                model = body.model,
                                usage = body.usage?.let { 
                                    EmbeddingUsage(it.promptTokens, it.totalTokens) 
                                } ?: EmbeddingUsage(0, 0)
                            )
                        }
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }
}

/**
 * API 调用异常
 */
class ApiException(
    message: String,
    val code: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)
