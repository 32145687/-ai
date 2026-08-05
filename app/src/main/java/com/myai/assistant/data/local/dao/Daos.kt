package com.myai.assistant.data.local.dao

import androidx.room.*
import com.myai.assistant.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    @Query("SELECT * FROM personas ORDER BY createdAt DESC")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    @Query("SELECT * FROM personas WHERE id = :id")
    suspend fun getPersonaById(id: String): PersonaEntity?

    @Query("SELECT * FROM personas WHERE isActive = 1 ORDER BY name")
    fun getActivePersonas(): Flow<List<PersonaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity)

    @Update
    suspend fun updatePersona(persona: PersonaEntity)

    @Delete
    suspend fun deletePersona(persona: PersonaEntity)

    @Query("UPDATE personas SET isActive = 0")
    suspend fun deactivateAllPersonas()

    @Query("SELECT COUNT(*) FROM personas")
    suspend fun getPersonaCount(): Int
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE personaId = :personaId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForPersona(personaId: String, limit: Int = 50, offset: Int = 0): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE personaId = :personaId ORDER BY timestamp DESC")
    fun getMessagesFlow(personaId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE personaId = :personaId")
    suspend fun deleteAllMessagesForPersona(personaId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE personaId = :personaId")
    suspend fun getMessageCountForPersona(personaId: String): Int

    @Query("SELECT * FROM messages WHERE personaId = :personaId AND timestamp > :since ORDER BY timestamp ASC")
    suspend fun getMessagesSince(personaId: String, since: Long): List<MessageEntity>
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE type = :type ORDER BY importance DESC, accessCount DESC")
    fun getMemoriesByType(type: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE personaId = :personaId OR personaId IS NULL ORDER BY importance DESC")
    fun getMemoriesForPersona(personaId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET accessCount = accessCount + 1, lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun incrementAccessCount(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM memories ORDER BY importance DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int = 20): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE type = :type AND (personaId = :personaId OR personaId IS NULL)")
    suspend fun getMemoriesByTypeAndPersona(type: String, personaId: String): List<MemoryEntity>

    // Semantic search would be implemented with custom queries or FTS
    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' ORDER BY importance DESC")
    suspend fun searchMemories(query: String): List<MemoryEntity>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY scheduledTime ASC")
    fun getPendingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND scheduledTime <= :now")
    suspend fun getDueReminders(now: Long = System.currentTimeMillis()): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCompleted = 1, notificationSent = 1 WHERE id = :id")
    suspend fun markReminderAsCompleted(id: Long)

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY scheduledTime DESC")
    fun getCompletedReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE relatedPersonaId = :personaId")
    fun getRemindersForPersona(personaId: String): Flow<List<ReminderEntity>>
}

@Dao
interface UserBehaviorPatternDao {
    @Query("SELECT * FROM user_behavior_patterns ORDER BY confidence DESC, lastObserved DESC")
    fun getAllPatterns(): Flow<List<UserBehaviorPatternEntity>>

    @Query("SELECT * FROM user_behavior_patterns WHERE patternType = :type")
    fun getPatternsByType(type: String): Flow<List<UserBehaviorPatternEntity>>

    @Query("SELECT * FROM user_behavior_patterns WHERE id = :id")
    suspend fun getPatternById(id: Long): UserBehaviorPatternEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: UserBehaviorPatternEntity): Long

    @Update
    suspend fun updatePattern(pattern: UserBehaviorPatternEntity)

    @Delete
    suspend fun deletePattern(pattern: UserBehaviorPatternEntity)

    @Query("SELECT * FROM user_behavior_patterns WHERE patternType = :type LIMIT 1")
    suspend fun getLatestPatternByType(type: String): UserBehaviorPatternEntity?
}
