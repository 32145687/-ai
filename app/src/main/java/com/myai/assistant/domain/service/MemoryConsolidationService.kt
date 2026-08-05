package com.myai.assistant.domain.service

import com.myai.assistant.data.local.dao.MemoryDao
import com.myai.assistant.data.local.entity.MemoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * 记忆巩固与管理服务
 * 负责记忆的定期清理、合并、重要性衰减等后台任务
 */
@Singleton
class MemoryConsolidationService @Inject constructor(
    private val memoryDao: MemoryDao,
    private val vectorSearchService: VectorSearchService
) {

    /**
     * 执行记忆巩固任务
     * 应在后台线程定期调用（如每天一次）
     */
    suspend fun runConsolidation() = withContext(Dispatchers.IO) {
        // 1. 执行重要性衰减
        decayImportance()
        
        // 2. 清理低重要性记忆
        cleanupLowImportanceMemories()
        
        // 3. 合并相似记忆
        mergeSimilarMemories()
        
        // 4. 重新向量化过期记忆（可选）
        // reVectorizeOldMemories()
    }

    /**
     * 重要性衰减算法
     * 基于艾宾浩斯遗忘曲线：重要性随时间指数衰减
     * 新记忆衰减慢，旧记忆衰减快
     */
    private suspend fun decayImportance() {
        val allMemories = memoryDao.getAllMemories().first()
        val nowEpochDay = java.time.LocalDate.now().toEpochDay()

        allMemories.forEach { memory ->
            val daysSinceCreated = (nowEpochDay - memory.createdAt.toLocalDate().toEpochDay()).toInt()
            
            // 艾宾浩斯衰减因子：e^(-0.05 * days)
            val decayFactor = exp(-0.05 * daysSinceCreated)
            
            // 基础重要性 * 衰减因子
            val newImportance = (memory.importance * decayFactor).coerceIn(0.1f, 10f)
            
            // 如果重要性低于阈值，标记为待删除
            val shouldArchive = newImportance < 1.0f
            
            memoryDao.updateMemoryImportance(memory.id, newImportance, shouldArchive)
        }
    }

    /**
     * 清理低重要性或已归档的记忆
     * 释放存储空间，保持数据库轻量
     */
    private suspend fun cleanupLowImportanceMemories() {
        // 删除重要性<0.5 且已归档超过 30 天的记忆
        memoryDao.deleteArchivedMemoriesOlderThan(30)
        
        // 删除重要性<0.2 的未归档记忆（极端不重要的信息）
        memoryDao.deleteMemoriesWithImportanceBelow(0.2f)
    }

    /**
     * 合并相似记忆
     * 避免数据库中存储大量重复或高度相似的记忆
     */
    private suspend fun mergeSimilarMemories() {
        val allMemories = memoryDao.getAllMemories().first()
        
        // 按人格分组处理
        allMemories.groupBy { it.personaId }.forEach { (_, memories) ->
            // 对每个记忆，查找相似度>0.9 的其他记忆
            val processedIds = mutableSetOf<Long>()
            
            memories.forEach { memory ->
                if (memory.id in processedIds) return@forEach
                
                // 使用向量搜索查找相似记忆
                val similarMemories = vectorSearchService.findSimilarMemories(
                    queryText = memory.content,
                    personaId = memory.personaId,
                    threshold = 0.9f, // 高相似度阈值
                    limit = 5
                )
                
                // 排除自己
                val toMerge = similarMemories.filter { it.id != memory.id }
                
                if (toMerge.isNotEmpty()) {
                    // 合并策略：保留最重要的那个，其他标记为已合并
                    val mostImportant = (listOf(memory) + toMerge).maxBy { it.importance }
                    val others = (listOf(memory) + toMerge).filter { it.id != mostImportant.id }
                    
                    // 更新内容（可选：拼接内容）
                    val mergedContent = buildString {
                        append(mostImportant.content)
                        others.forEach { other ->
                            if (other.content != mostImportant.content) {
                                append("; ${other.content}")
                            }
                        }
                    }
                    
                    val updatedMemory = mostImportant.copy(content = mergedContent)
                    memoryDao.insertMemory(updatedMemory) // 插入更新版
                    
                    // 删除旧的重复记忆
                    others.forEach { memoryDao.deleteMemoryById(it.id) }
                    
                    processedIds.add(updatedMemory.id)
                }
            }
        }
    }

    /**
     * 获取记忆统计信息
     */
    suspend fun getMemoryStats(): MemoryStats = withContext(Dispatchers.IO) {
        val allMemories = memoryDao.getAllMemories().first()
        
        MemoryStats(
            totalCount = allMemories.size,
            byType = MemoryType.values().associateWith { type ->
                allMemories.count { it.type == type }
            },
            averageImportance = allMemories.averageOrNull { it.importance } ?: 0f,
            archivedCount = allMemories.count { it.isArchived }
        )
    }
}

/**
 * 记忆统计数据结构
 */
data class MemoryStats(
    val totalCount: Int,
    val byType: Map<MemoryType, Int>,
    val averageImportance: Float,
    val archivedCount: Int
)
