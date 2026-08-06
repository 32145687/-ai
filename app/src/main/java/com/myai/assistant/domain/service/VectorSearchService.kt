package com.myai.assistant.domain.service

import com.myai.assistant.data.local.dao.MemoryDao
import com.myai.assistant.data.local.entity.MemoryEntity
import com.myai.assistant.domain.model.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * 向量搜索服务接口
 * 负责执行向量相似度搜索，用于检索相关记忆
 */
interface VectorSearchService {
    /**
     * 在记忆中搜索与查询向量最相似的记忆
     * @param queryVector 查询向量
     * @param personaId 人格 ID（可选，用于过滤）
     * @param limit 返回结果数量限制
     * @param minSimilarity 最小相似度阈值
     * @return 按相似度排序的记忆列表
     */
    suspend fun searchMemoriesByVector(
        queryVector: List<Float>,
        personaId: String? = null,
        limit: Int = 10,
        minSimilarity: Float = 0.3f
    ): List<MemoryWithSimilarity>
    
    /**
     * 通过文本查询搜索记忆（自动向量化后搜索）
     * @param queryText 查询文本
     * @param personaId 人格 ID（可选）
     * @param limit 返回结果数量限制
     * @return 按相似度排序的记忆列表
     */
    suspend fun searchMemoriesByText(
        queryText: String,
        personaId: String? = null,
        limit: Int = 10
    ): List<MemoryWithSimilarity>
}

/**
 * 带有相似度的记忆结果
 */
data class MemoryWithSimilarity(
    val memory: Memory,
    val similarity: Float
)

/**
 * 基于内存的向量搜索服务实现
 * 使用余弦相似度算法在内存中计算向量相似度
 * 
 * 优点：简单、无需额外依赖、适合中小规模数据
 * 缺点：大数据量时性能较低，需要考虑优化或使用专门的向量数据库
 */
@Singleton
class InMemoryVectorSearchService @Inject constructor(
    private val memoryDao: MemoryDao,
    private val embeddingService: EmbeddingService
) : VectorSearchService {
    
    companion object {
        private const val DEFAULT_LIMIT = 10
        private const val MIN_SIMILARITY_THRESHOLD = 0.3f
    }
    
    override suspend fun searchMemoriesByVector(
        queryVector: List<Float>,
        personaId: String?,
        limit: Int,
        minSimilarity: Float
    ): List<MemoryWithSimilarity> {
        return withContext(Dispatchers.Default) {
            try {
                // 获取所有带向量的记忆
                val memories = if (personaId != null) {
                    memoryDao.getMemoriesForPersona(personaId)
                } else {
                    memoryDao.getTopMemories(limit = 1000) // 限制最大加载量
                }
                
                // 过滤出有向量的记忆
                val memoriesWithEmbeddings = memories.filter { 
                    it.embeddingJson != null 
                }
                
                // 计算每个记忆与查询向量的相似度
                val results = memoriesWithEmbeddings.mapNotNull { entity ->
                    val vector = parseEmbedding(entity.embeddingJson)
                    if (vector != null && vector.isNotEmpty()) {
                        val similarity = cosineSimilarity(queryVector, vector)
                        if (similarity >= minSimilarity) {
                            MemoryWithSimilarity(
                                memory = entity.toMemory(),
                                similarity = similarity
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                
                // 按相似度降序排序并返回前 N 个
                results.sortedByDescending { it.similarity }.take(limit)
            } catch (e: Exception) {
                // 如果搜索失败，返回空列表
                emptyList()
            }
        }
    }
    
    override suspend fun searchMemoriesByText(
        queryText: String,
        personaId: String?,
        limit: Int
    ): List<MemoryWithSimilarity> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 将查询文本向量化
                val embeddingResult = embeddingService.generateEmbedding(queryText)
                
                // 2. 使用向量搜索记忆
                searchMemoriesByVector(
                    queryVector = embeddingResult.vector,
                    personaId = personaId,
                    limit = limit,
                    minSimilarity = MIN_SIMILARITY_THRESHOLD
                )
            } catch (e: Exception) {
                // 如果向量化失败，降级为关键词搜索
                fallbackKeywordSearch(queryText, personaId, limit)
            }
        }
    }
    
    /**
     * 降级方案：关键词搜索
     * 当向量化不可用时使用
     */
    private suspend fun fallbackKeywordSearch(
        query: String,
        personaId: String?,
        limit: Int
    ): List<MemoryWithSimilarity> {
        return withContext(Dispatchers.IO) {
            val results = if (personaId != null) {
                memoryDao.getMemoriesForPersona(personaId)
            } else {
                memoryDao.searchMemories(query)
            }
            
            // 简单的关键词匹配评分
            results.filter { 
                it.content.contains(query, ignoreCase = true) 
            }.map { entity ->
                MemoryWithSimilarity(
                    memory = entity.toMemory(),
                    similarity = 0.5f // 固定相似度
                )
            }.take(limit)
        }
    }
    
    /**
     * 解析 JSON 字符串为 Float 列表
     */
    private fun parseEmbedding(jsonString: String?): List<Float>? {
        if (jsonString.isNullOrBlank()) return null
        
        return try {
            // 简单的 JSON 数组解析：[0.1, 0.2, 0.3, ...]
            val trimmed = jsonString.trim().removePrefix("[").removeSuffix("]")
            trimmed.split(",")
                .map { it.trim().toFloatOrNull() ?: 0f }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 计算两个向量的余弦相似度
     * 公式：cos(θ) = (A·B) / (||A|| * ||B||)
     * 返回值范围：[-1, 1]，越接近 1 表示越相似
     */
    fun cosineSimilarity(vectorA: List<Float>, vectorB: List<Float>): Float {
        if (vectorA.isEmpty() || vectorB.isEmpty()) return 0f
        if (vectorA.size != vectorB.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        
        if (normA == 0f || normB == 0f) return 0f
        
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}

/**
 * 向量相似度计算工具函数
 */
object VectorUtils {
    
    /**
     * 计算欧几里得距离
     * 距离越小表示越相似
     */
    fun euclideanDistance(vectorA: List<Float>, vectorB: List<Float>): Float {
        if (vectorA.size != vectorB.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        var sum = 0f
        for (i in vectorA.indices) {
            val diff = vectorA[i] - vectorB[i]
            sum += diff * diff
        }
        
        return sqrt(sum)
    }
    
    /**
     * 归一化向量（L2 归一化）
     */
    fun normalizeVector(vector: List<Float>): List<Float> {
        val magnitude = sqrt(vector.sumOf { it * it })
        if (magnitude == 0f) return vector
        
        return vector.map { it / magnitude }
    }
}
