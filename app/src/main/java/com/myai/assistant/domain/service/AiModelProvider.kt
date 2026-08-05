package com.myai.assistant.domain.service

import com.myai.assistant.data.remote.model.ChatCompletionRequest
import com.myai.assistant.data.remote.model.MessageDto
import kotlinx.coroutines.flow.Flow

/**
 * AI 模型提供者统一接口
 * 支持云端和本地模型的策略模式
 */
interface AiModelProvider {
    
    /**
     * 流式聊天接口（用于 UI 打字机效果）
     * @param messages 对话历史
     * @param systemPrompt 系统提示词（人设）
     * @param apiKey API 密钥
     * @return 文本流
     */
    fun chatStream(
        messages: List<MessageDto>,
        systemPrompt: String?,
        apiKey: String,
        model: String? = null
    ): Flow<String>
    
    /**
     * 阻塞式完成接口（用于记忆提取等后台任务）
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @param temperature 温度参数
     * @return 完整响应文本
     */
    suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.7f,
        apiKey: String? = null,
        model: String? = null
    ): String
}
