package com.myai.assistant.domain.service

import com.myai.assistant.data.local.entity.MemoryEntity
import com.myai.assistant.data.local.entity.MemoryType
import com.myai.assistant.domain.model.Message
import com.myai.assistant.domain.model.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能记忆提取服务
 * 利用 LLM 分析对话内容，自动提取结构化记忆（习惯、偏好、事实等）
 */
@Singleton
class MemoryExtractionService @Inject constructor(
    private val aiModelProvider: AiModelProvider
) {

    /**
     * 分析最近一段对话，提取需要长期存储的记忆
     * @param recentMessages 最近的对话历史
     * @param persona 当前人格（不同人格关注点不同）
     * @return 提取出的记忆列表
     */
    fun extractMemoriesFromConversation(
        recentMessages: List<Message>,
        persona: Persona
    ): Flow<List<MemoryEntity>> = flow {
        if (recentMessages.size < 2) {
            emit(emptyList())
            return@flow
        }

        // 构建提示词，要求 LLM 以 JSON 格式返回提取结果
        val systemPrompt = """
            你是一个专业的记忆分析师。你的任务是从用户与助手的对话中提取关键的长期记忆。
            当前助手的人格是：${persona.name} (${persona.description})。
            
            请分析以下对话，提取符合以下条件的信息：
            1. 用户的个人偏好（喜欢/讨厌的食物、活动、颜色等）
            2. 用户的习惯（作息时间、工作流程、日常行为）
            3. 重要事实（姓名、职业、居住地、家庭成员、宠物）
            4. 待办事项或计划（未来的会议、目标、截止日期）
            5. 情感状态或性格特征（焦虑、乐观、内向等）
            
            忽略临时性的闲聊或无关紧要的信息。
            如果对话中没有值得长期存储的信息，返回空列表。
            
            请以严格的 JSON 数组格式返回，每个对象包含：
            - content: 记忆的具体内容（简练的陈述句）
            - type: 记忆类型 (PREFERENCE, HABIT, FACT, TASK, EMOTIONAL)
            - importance: 重要性评分 (1-10)
            
            示例输出：
            [
              {"content": "用户每天早上 7 点起床喝咖啡", "type": "HABIT", "importance": 8},
              {"content": "用户讨厌吃香菜", "type": "PREFERENCE", "importance": 6}
            ]
        """.trimIndent()

        val conversationText = recentMessages.joinToString("\n") { msg ->
            "${if (msg.isUser) "用户" else persona.name}: ${msg.content}"
        }

        val userPrompt = "请分析以下对话并提取记忆：\n$conversationText"

        try {
            // 调用 LLM 进行提取
            val jsonResponse = aiModelProvider.generateCompletion(
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.3f // 低温度以保证输出稳定性
            )
            
            // 解析 JSON 并转换为 MemoryEntity
            val memories = parseJsonToMemories(jsonResponse, persona.id)
            emit(memories)
        } catch (e: Exception) {
            // 失败时返回空列表，不阻断主流程
            emit(emptyList())
        }
    }

    /**
     * 解析 LLM 返回的 JSON 字符串为 MemoryEntity 列表
     * 实际项目中建议使用 kotlinx.serialization 或 Gson
     */
    private fun parseJsonToMemories(json: String, personaId: String): List<MemoryEntity> {
        // TODO: 接入真实的 JSON 解析库 (kotlinx.serialization)
        // 这里为了演示逻辑简化处理，实际需实现完整解析
        return emptyList()
    }
    
    /**
     * 供外部调用的简单完成接口（用于内部提取任务）
     */
    suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.7f
    ): String {
        // 这个方法是临时的，实际应该复用 aiModelProvider 的内部逻辑
        // 由于 aiModelProvider 主要设计为 Flow 流式返回，这里需要一个阻塞式的变体
        // 在 ChatViewModel 中会统一处理
        throw NotImplementedError("应通过 aiModelProvider 的统一接口调用")
    }
}
