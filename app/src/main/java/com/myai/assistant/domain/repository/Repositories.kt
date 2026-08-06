package com.myai.assistant.domain.repository

import com.myai.assistant.data.local.dao.*
import com.myai.assistant.data.local.entity.*
import com.myai.assistant.data.remote.api.LlmApiService
import com.myai.assistant.data.remote.model.*
import com.myai.assistant.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing personas and their conversation histories
 */
@Singleton
class PersonaRepository @Inject constructor(
    private val personaDao: PersonaDao,
    private val messageDao: MessageDao,
    private val llmApiService: LlmApiService
) {
    
    fun getAllPersonas(): Flow<List<Persona>> {
        return personaDao.getAllPersonas().map { entities ->
            entities.map { it.toPersona() }
        }
    }

    fun getActivePersonas(): Flow<List<Persona>> {
        return personaDao.getActivePersonas().map { entities ->
            entities.map { it.toPersona() }
        }
    }

    suspend fun getPersonaById(id: String): Persona? {
        return personaDao.getPersonaById(id)?.toPersona()
    }

    suspend fun savePersona(persona: Persona) {
        personaDao.insertPersona(PersonaEntity.fromPersona(persona))
    }

    suspend fun updatePersona(persona: Persona) {
        personaDao.updatePersona(PersonaEntity.fromPersona(persona))
    }

    suspend fun deletePersona(persona: Persona) {
        personaDao.deletePersona(PersonaEntity.fromPersona(persona))
        // Also delete all messages for this persona
        messageDao.deleteAllMessagesForPersona(persona.id)
    }

    suspend fun setActivePersona(personaId: String) {
        personaDao.deactivateAllPersonas()
        val persona = personaDao.getPersonaById(personaId)
        persona?.let {
            personaDao.updatePersona(it.copy(isActive = true))
        }
    }

    suspend fun initializeDefaultPersonas() {
        val count = personaDao.getPersonaCount()
        if (count == 0) {
            val defaultPersonas = listOf(
                DefaultPersonas.FRIENDLY_COMPANION,
                DefaultPersonas.PROFESSIONAL_ADVISOR,
                DefaultPersonas.CREATIVE_PARTNER,
                DefaultPersonas.EMPATHIC_LISTENER,
                DefaultPersonas.ANALYTICAL_THINKER
            )
            defaultPersonas.forEach { persona ->
                personaDao.insertPersona(PersonaEntity.fromPersona(persona))
            }
        }
    }
}

/**
 * Repository for managing chat messages
 */
@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    
    fun getMessagesFlow(personaId: String): Flow<List<Message>> {
        return messageDao.getMessagesFlow(personaId).map { entities ->
            entities.map { it.toMessage() }.reversed() // Most recent first
        }
    }

    suspend fun getMessagesForPersona(personaId: String, limit: Int = 50): List<Message> {
        return messageDao.getMessagesForPersona(personaId, limit).map { it.toMessage() }
    }

    suspend fun saveMessage(message: Message): Long {
        return messageDao.insertMessage(MessageEntity.fromMessage(message))
    }

    suspend fun saveMessages(messages: List<Message>) {
        messageDao.insertMessages(messages.map { MessageEntity.fromMessage(it) })
    }

    suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(MessageEntity.fromMessage(message))
    }

    suspend fun clearConversation(personaId: String) {
        messageDao.deleteAllMessagesForPersona(personaId)
    }
}

/**
 * Repository for managing memories with RAG support
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao,
    private val settingsManager: com.myai.assistant.data.manager.SettingsManager
) {
    
    fun getMemoriesForPersona(personaId: String): Flow<List<Memory>> {
        return memoryDao.getMemoriesForPersona(personaId).map { entities ->
            entities.map { it.toMemory() }
        }
    }

    suspend fun saveMemory(memory: Memory): Long {
        return memoryDao.insertMemory(MemoryEntity.fromMemory(memory))
    }

    suspend fun saveMemories(memories: List<Memory>) {
        memoryDao.insertMemories(memories.map { MemoryEntity.fromMemory(it) })
    }

    suspend fun updateMemory(memory: Memory) {
        memoryDao.updateMemory(MemoryEntity.fromMemory(memory))
    }

    suspend fun deleteMemory(memory: Memory) {
        memoryDao.deleteMemory(MemoryEntity.fromMemory(memory))
    }

    suspend fun searchMemories(query: String): List<Memory> {
        return memoryDao.searchMemories(query).map { it.toMemory() }
    }

    suspend fun getTopMemories(limit: Int = 20): List<Memory> {
        return memoryDao.getTopMemories(limit).map { it.toMemory() }
    }

    suspend fun recordMemoryAccess(memoryId: Long) {
        memoryDao.incrementAccessCount(memoryId)
    }
}

/**
 * Repository for managing reminders
 */
@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {
    
    fun getPendingReminders(): Flow<List<Reminder>> {
        return reminderDao.getPendingReminders().map { entities ->
            entities.map { it.toReminder() }
        }
    }

    fun getCompletedReminders(): Flow<List<Reminder>> {
        return reminderDao.getCompletedReminders().map { entities ->
            entities.map { it.toReminder() }
        }
    }

    suspend fun saveReminder(reminder: Reminder): Long {
        return reminderDao.insertReminder(ReminderEntity.fromReminder(reminder))
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(ReminderEntity.fromReminder(reminder))
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(ReminderEntity.fromReminder(reminder))
    }

    suspend fun markAsCompleted(reminderId: Long) {
        reminderDao.markReminderAsCompleted(reminderId)
    }

    suspend fun getDueReminders(): List<Reminder> {
        return reminderDao.getDueReminders().map { it.toReminder() }
    }
}

/**
 * Repository for user behavior patterns and learning
 */
@Singleton
class UserBehaviorRepository @Inject constructor(
    private val patternDao: UserBehaviorPatternDao
) {
    
    fun getAllPatterns(): Flow<List<UserBehaviorPattern>> {
        return patternDao.getAllPatterns().map { entities ->
            entities.map { it.toPattern() }
        }
    }

    fun getPatternsByType(type: PatternType): Flow<List<UserBehaviorPattern>> {
        return patternDao.getPatternsByType(type.name).map { entities ->
            entities.map { it.toPattern() }
        }
    }

    suspend fun savePattern(pattern: UserBehaviorPattern): Long {
        return patternDao.insertPattern(UserBehaviorPatternEntity.fromPattern(pattern))
    }

    suspend fun updatePattern(pattern: UserBehaviorPattern) {
        patternDao.updatePattern(UserBehaviorPatternEntity.fromPattern(pattern))
    }

    suspend fun deletePattern(pattern: UserBehaviorPattern) {
        patternDao.deletePattern(UserBehaviorPatternEntity.fromPattern(pattern))
    }

    suspend fun getLatestPatternByType(type: PatternType): UserBehaviorPattern? {
        return patternDao.getLatestPatternByType(type.name)?.toPattern()
    }
}

/**
 * Repository for interacting with LLM APIs
 */
@Singleton
class LlmRepository @Inject constructor(
    private val llmApiService: LlmApiService,
    private val settingsManager: com.myai.assistant.data.manager.SettingsManager
) {
    
    suspend fun sendChatRequest(
        messages: List<MessageDto>,
        systemPrompt: String?,
        apiKey: String,
        model: String? = null,
        temperature: Float = 0.7f
    ): Result<ChatCompletionResponse> {
        return try {
            val selectedModel = model ?: settingsManager.defaultModelFlow.first()
            
            val request = ChatCompletionRequest(
                model = selectedModel,
                messages = messages,
                temperature = temperature,
                systemPrompt = systemPrompt
            )
            
            val response = llmApiService.createChatCompletion(
                request = request,
                authorization = "Bearer $apiKey"
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateEmbedding(text: String, apiKey: String): Result<List<Float>> {
        return try {
            val embeddingModel = settingsManager.embeddingModelFlow.first()
            
            val request = EmbeddingRequest(
                model = embeddingModel,
                input = text
            )
            
            val response = llmApiService.createChatCompletion(
                request = ChatCompletionRequest(
                    model = embeddingModel,
                    messages = emptyList()
                ),
                authorization = "Bearer $apiKey"
            )
            
            // Note: Actual embedding endpoint would be different
            // This is a placeholder - implement proper embedding API call
            Result.failure(NotImplementedError("Embedding API not fully implemented"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
