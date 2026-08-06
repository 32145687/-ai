package com.myai.assistant.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.data.manager.SettingsManager
import com.myai.assistant.data.remote.model.MessageDto
import com.myai.assistant.domain.model.*
import com.myai.assistant.domain.repository.*
import com.myai.assistant.domain.service.*
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
    private val settingsManager: SettingsManager,
    private val ragService: RagService,
    private val memoryExtractionService: MemoryExtractionService,
    private val embeddingService: EmbeddingService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _personas = MutableStateFlow<List<Persona>>(emptyList())
    val personas: StateFlow<List<Persona>> = _personas.asStateFlow()

    private val _selectedPersona = MutableStateFlow<Persona?>(null)
    val selectedPersona: StateFlow<Persona?> = _selectedPersona.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    init {
        loadPersonas()
        loadMessages()
        viewModelScope.launch {
            // Initialize default personas if needed
            personaRepository.initializeDefaultPersonas()
        }
    }

    fun loadPersonas() {
        viewModelScope.launch {
            val personaList = personaRepository.getAllPersonas()
            _personas.value = personaList
            // Select first persona by default
            if (personaList.isNotEmpty() && _selectedPersona.value == null) {
                _selectedPersona.value = personaList.first()
            }
        }
    }

    fun selectPersona(persona: Persona) {
        _selectedPersona.value = persona
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            val personaId = _selectedPersona.value?.id ?: return@launch
            val msgs = messageRepository.getMessagesForPersona(personaId, limit = 50)
            _messages.value = msgs
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            val persona = _selectedPersona.value ?: return@launch
            val personaId = persona.id
            
            // Create user message
            val userMessage = Message(
                content = content,
                role = MessageRole.USER,
                personaId = personaId,
                avatarUrl = null // User doesn't need custom avatar
            )
            
            // Save user message
            val messageId = messageRepository.saveMessage(userMessage)
            val savedUserMessage = userMessage.copy(id = messageId)
            
            // Update UI immediately
            _messages.value = listOf(savedUserMessage) + _messages.value
            
            // Get recent conversation history for context
            val recentMessages = messageRepository.getMessagesForPersona(personaId, limit = 10)
            
            // Call LLM API
            _isThinking.value = true
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
                    persona.systemPrompt?.let {
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
                    systemPrompt = persona.systemPrompt,
                    apiKey = apiKey
                )

                result.onSuccess { response ->
                    val assistantContent = response.choices.firstOrNull()?.message?.content ?: "抱歉，我没有理解您的问题。"
                    
                    val assistantMessage = Message(
                        content = assistantContent,
                        role = MessageRole.ASSISTANT,
                        personaId = personaId,
                        avatarUrl = persona.avatarUrl,
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
                _isThinking.value = false
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
            val personaId = _selectedPersona.value?.id ?: return@launch
            messageRepository.clearConversation(personaId)
            _messages.value = emptyList()
        }
    }
}
