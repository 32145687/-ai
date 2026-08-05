package com.myai.assistant.domain.service

import com.myai.assistant.data.remote.api.LlmApiService
import com.myai.assistant.data.remote.model.ChatCompletionRequest
import com.myai.assistant.data.remote.model.MessageDto
import com.myai.assistant.data.manager.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端 AI 模型提供者实现
 * 调用 OpenAI 兼容的 API（支持 GPT-4, Claude, 通义千问等）
 */
@Singleton
class CloudAiModelProvider @Inject constructor(
    private val apiService: LlmApiService,
    private val settingsManager: SettingsManager,
    private val ragService: RagService
) : AiModelProvider {

    override fun chatStream(
        messages: List<MessageDto>,
        systemPrompt: String?,
        apiKey: String,
        model: String?
    ): Flow<String> = channelFlow {
        try {
            // TODO: 实现 SSE 流式请求
            // 当前简化为非流式调用，后续可扩展为真正的 Server-Sent Events
            
            val selectedModel = model ?: settingsManager.defaultModelFlow.first()
            
            val request = ChatCompletionRequest(
                model = selectedModel,
                messages = messages,
                temperature = 0.7f,
                systemPrompt = systemPrompt,
                stream = false // 暂时使用非流式
            )
            
            val response = apiService.createChatCompletion(
                request = request,
                authorization = "Bearer $apiKey"
            )
            
            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.content ?: ""
                // 模拟打字机效果：逐字发送
                content.forEach { char ->
                    send(char.toString())
                }
            } else {
                send("API Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            send("Error: ${e.message}")
        }
    }

    override suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float,
        apiKey: String?,
        model: String?
    ): String {
        val actualApiKey = apiKey ?: settingsManager.apiKeyFlow.first()
        if (actualApiKey.isNullOrBlank()) {
            return ""
        }
        
        val selectedModel = model ?: settingsManager.defaultModelFlow.first()
        
        val messages = listOf(
            MessageDto(role = "system", content = systemPrompt),
            MessageDto(role = "user", content = userPrompt)
        )
        
        val request = ChatCompletionRequest(
            model = selectedModel,
            messages = messages,
            temperature = temperature,
            systemPrompt = null, // Already in messages
            stream = false
        )
        
        val response = apiService.createChatCompletion(
            request = request,
            authorization = "Bearer $actualApiKey"
        )
        
        return if (response.isSuccessful && response.body() != null) {
            response.body()!!.choices.firstOrNull()?.message?.content ?: ""
        } else {
            ""
        }
    }
}
