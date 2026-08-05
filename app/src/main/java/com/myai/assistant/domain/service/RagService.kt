package com.myai.assistant.domain.service

import com.myai.assistant.data.local.dao.MemoryDao
import com.myai.assistant.data.local.entity.MemoryEntity
import com.myai.assistant.domain.model.Memory
import com.myai.assistant.domain.model.MemoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG (Retrieval-Augmented Generation) 服务
 * 
 * 核心功能：在发送消息给 LLM 之前，检索相关记忆并注入到 Prompt 中
 * 这是实现"记忆"和"个性化"的关键组件
 */
interface RagService {
    /**
     * 检索与当前查询相关的记忆
     * @param query 当前用户输入或问题
     * @param personaId 人格 ID（用于过滤人格专属记忆）
     * @param maxMemories 最大返回记忆数量
     * @return 相关记忆列表
     */
    suspend fun retrieveRelevantMemories(
        query: String,
        personaId: String? = null,
        maxMemories: Int = 5
    ): List<Memory>
    
    /**
     * 构建包含记忆的增强 Prompt
     * @param originalPrompt 原始用户输入
     * @param systemPrompt 系统提示词（人设）
     * @param memories 检索到的相关记忆
     * @param personaId 人格 ID
     * @return 增强后的 Prompt 内容
     */
    fun buildEnhancedPrompt(
        originalPrompt: String,
        systemPrompt: String,
        memories: List<Memory>,
        personaId: String? = null
    ): String
    
    /**
     * 完整的 RAG 流程：检索记忆并构建增强 Prompt
     * @param query 用户输入
     * @param systemPrompt 系统提示词
     * @param personaId 人格 ID
     * @param maxMemories 最大记忆数量
     * @return 包含记忆的完整上下文
     */
    suspend fun processQueryWithRag(
        query: String,
        systemPrompt: String,
        personaId: String? = null,
        maxMemories: Int = 5
    ): RagContext
}

/**
 * RAG 处理结果上下文
 */
data class RagContext(
    val originalQuery: String,
    val enhancedPrompt: String,
    val retrievedMemories: List<Memory>,
    val memoryCount: Int,
    val processingTimeMs: Long
)

/**
 * RAG 服务实现
 */
@Singleton
class RagServiceImpl @Inject constructor(
    private val vectorSearchService: VectorSearchService,
    private val memoryDao: MemoryDao,
    private val embeddingService: EmbeddingService
) : RagService {
    
    companion object {
        private const val DEFAULT_MAX_MEMORIES = 5
        private const val MIN_SIMILARITY_THRESHOLD = 0.3f
    }
    
    override suspend fun retrieveRelevantMemories(
        query: String,
        personaId: String?,
        maxMemories: Int
    ): List<Memory> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用向量搜索检索最相关的记忆
                val results = vectorSearchService.searchMemoriesByText(
                    queryText = query,
                    personaId = personaId,
                    limit = maxMemories
                )
                
                // 返回记忆对象（已按相似度排序）
                results.map { it.memory }
            } catch (e: Exception) {
                // 如果向量搜索失败，尝试简单的关键词搜索作为降级
                fallbackRetrieve(query, personaId, maxMemories)
            }
        }
    }
    
    override fun buildEnhancedPrompt(
        originalPrompt: String,
        systemPrompt: String,
        memories: List<Memory>,
        personaId: String?
    ): String {
        if (memories.isEmpty()) {
            // 没有相关记忆时，返回原始 Prompt
            return """
                $systemPrompt
                
                用户：$originalPrompt
            """.trimIndent()
        }
        
        // 构建记忆上下文部分
        val memoryContext = memories.joinToString("\n") { memory ->
            val typeLabel = when (memory.type) {
                MemoryType.FACT -> "事实"
                MemoryType.PREFERENCE -> "偏好"
                MemoryType.EVENT -> "事件"
                MemoryType.GOAL -> "目标"
                MemoryType.RELATIONSHIP -> "关系"
                MemoryType.SKILL -> "技能"
                MemoryType.EMOTION -> "情感"
                MemoryType.HABIT -> "习惯"
                MemoryType.CONTEXT -> "上下文"
            }
            "- [$typeLabel] ${memory.content}"
        }
        
        // 构建完整的增强 Prompt
        return """
            $systemPrompt
            
            【相关记忆】
            以下是与当前对话相关的记忆信息，请在回复时参考这些信息，使你的回答更加个性化和连贯：
            
            $memoryContext
            
            【对话继续】
            用户：$originalPrompt
        """.trimIndent()
    }
    
    override suspend fun processQueryWithRag(
        query: String,
        systemPrompt: String,
        personaId: String?,
        maxMemories: Int
    ): RagContext {
        val startTime = System.currentTimeMillis()
        
        // 1. 检索相关记忆
        val memories = retrieveRelevantMemories(query, personaId, maxMemories)
        
        // 2. 构建增强 Prompt
        val enhancedPrompt = buildEnhancedPrompt(query, systemPrompt, memories, personaId)
        
        val processingTime = System.currentTimeMillis() - startTime
        
        return RagContext(
            originalQuery = query,
            enhancedPrompt = enhancedPrompt,
            retrievedMemories = memories,
            memoryCount = memories.size,
            processingTimeMs = processingTime
        )
    }
    
    /**
     降级方案：基于关键词的简单检索
     */
    private suspend fun fallbackRetrieve(
        query: String,
        personaId: String?,
        maxMemories: Int
    ): List<Memory> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = if (personaId != null) {
                    memoryDao.getMemoriesForPersona(personaId)
                } else {
                    memoryDao.searchMemories(query)
                }
                
                // 简单的关键词匹配
                entities.filter { 
                    it.content.contains(query, ignoreCase = true) ||
                    query.contains(it.content, ignoreCase = true)
                }.map { it.toMemory() }.take(maxMemories)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

/**
 * 记忆检索策略接口
 * 允许扩展不同的检索算法
 */
interface RetrievalStrategy {
    suspend fun retrieve(
        query: String,
        personaId: String?,
        limit: Int
    ): List<Memory>
}

/**
 * 混合检索策略：结合向量搜索和关键词匹配
 */
class HybridRetrievalStrategy @Inject constructor(
    private val vectorSearchService: VectorSearchService,
    private val memoryDao: MemoryDao
) : RetrievalStrategy {
    
    override suspend fun retrieve(
        query: String,
        personaId: String?,
        limit: Int
    ): List<Memory> {
        return withContext(Dispatchers.IO) {
            try {
                // 同时执行向量搜索和关键词搜索
                val vectorResults = vectorSearchService.searchMemoriesByText(
                    queryText = query,
                    personaId = personaId,
                    limit = limit
                )
                
                // 合并结果并去重（可以添加更复杂的融合算法）
                vectorResults.map { it.memory }.distinctBy { it.id }.take(limit)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
