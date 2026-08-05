package com.myai.assistant.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.data.manager.SettingsManager
import com.myai.assistant.data.remote.model.MessageDto
import com.myai.assistant.domain.model.*
import com.myai.assistant.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val personaRepository: PersonaRepository,
    private val memoryRepository: MemoryRepository,
    private val llmRepository: LlmRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentPersonaId = MutableStateFlow<String>("friendly_companion")
    val currentPersonaId: StateFlow<String> = _currentPersonaId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMessages()
        viewModelScope.launch {
            // Initialize default personas if needed
            personaRepository.initializeDefaultPersonas()
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            val personaId = _currentPersonaId.value
            val msgs = messageRepository.getMessagesForPersona(personaId, limit = 50)
            _messages.value = msgs
        }
    }

    fun switchPersona(personaId: String) {
        _currentPersonaId.value = personaId
        loadMessages()
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            val personaId = _currentPersonaId.value
            
            // Create user message
            val userMessage = Message(
                content = content,
                role = MessageRole.USER,
                personaId = personaId
            )
            
            // Save user message
            val messageId = messageRepository.saveMessage(userMessage)
            val savedUserMessage = userMessage.copy(id = messageId)
            
            // Update UI immediately
            _messages.value = listOf(savedUserMessage) + _messages.value
            
            // Get current persona for system prompt
            val persona = personaRepository.getPersonaById(personaId)
            
            // Get recent conversation history for context
            val recentMessages = messageRepository.getMessagesForPersona(personaId, limit = 10)
            
            // Call LLM API
            _isLoading.value = true
            try {
                val apiKey = settingsManager.apiKeyFlow.first()
                if (apiKey.isNullOrBlank()) {
                    // Show error: API key not set
                    val errorMessage = Message(
                        content = "请先在设置中配置 API 密钥",
                        role = MessageRole.SYSTEM,
                        personaId = personaId
                    )
                    val errorId = messageRepository.saveMessage(errorMessage)
                    _messages.value = listOf(errorMessage.copy(id = errorId)) + _messages.value
                    return@launch
                }

                // Prepare messages for API
                val apiMessages = buildList {
                    // Add system prompt if exists
                    persona?.systemPrompt?.let {
                        add(MessageDto(role = "system", content = it))
                    }
                    
                    // Add conversation history (reversed for chronological order)
                    addAll(recentMessages.reversed().map { 
                        MessageDto(
                            role = it.role.name.lowercase(),
                            content = it.content
                        )
                    })
                }

                val result = llmRepository.sendChatRequest(
                    messages = apiMessages,
                    systemPrompt = persona?.systemPrompt,
                    apiKey = apiKey
                )

                result.onSuccess { response ->
                    val assistantContent = response.choices.firstOrNull()?.message?.content ?: "抱歉，我没有理解您的问题。"
                    
                    val assistantMessage = Message(
                        content = assistantContent,
                        role = MessageRole.ASSISTANT,
                        personaId = personaId,
                        metadata = MessageMetadata(
                            modelUsed = response.model,
                            tokensUsed = response.usage?.totalTokens,
                            responseTimeMs = System.currentTimeMillis() - savedUserMessage.timestamp
                        )
                    )
                    
                    val assistantMessageId = messageRepository.saveMessage(assistantMessage)
                    _messages.value = listOf(assistantMessage.copy(id = assistantMessageId)) + _messages.value
                    
                    // Learn from this interaction (if learning is enabled)
                    if (settingsManager.enableLearningFlow.first()) {
                        learnFromInteraction(userMessage, assistantMessage)
                    }
                }

                result.onFailure { error ->
                    val errorMessage = Message(
                        content = "发生错误：${error.message}",
                        role = MessageRole.SYSTEM,
                        personaId = personaId
                    )
                    val errorId = messageRepository.saveMessage(errorMessage)
                    _messages.value = listOf(errorMessage.copy(id = errorId)) + _messages.value
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun learnFromInteraction(userMessage: Message, assistantMessage: Message) {
        // Extract important information from the conversation
        // This is a simplified version - implement proper NLP/ML for production
        
        // Save conversation to memory
        val conversationMemory = Memory(
            content = "User: ${userMessage.content}\nAssistant: ${assistantMessage.content}",
            type = MemoryType.CONVERSATION,
            personaId = userMessage.personaId,
            importance = 0.5f
        )
        memoryRepository.saveMemory(conversationMemory)
        
        // Analyze for user facts/preferences (simplified logic)
        val userContent = userMessage.content.lowercase()
        if (userContent.contains("我叫") || userContent.contains("我是")) {
            val factMemory = Memory(
                content = userMessage.content,
                type = MemoryType.USER_FACT,
                personaId = null, // Global memory
                importance = 0.9f
            )
            memoryRepository.saveMemory(factMemory)
        }
        
        // Update behavior patterns
        // (Implement pattern detection logic here)
    }

    fun clearConversation() {
        viewModelScope.launch {
            messageRepository.clearConversation(_currentPersonaId.value)
            _messages.value = emptyList()
        }
    }
}
