package com.myai.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.myai.assistant.domain.model.*

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val avatarColor: String,
    val createdAt: Long,
    val isActive: Boolean,
    val traitsJson: String, // Stored as JSON
    val conversationStyle: String
) {
    companion object {
        private val gson = Gson()

        fun fromPersona(persona: Persona): PersonaEntity {
            return PersonaEntity(
                id = persona.id,
                name = persona.name,
                description = persona.description,
                systemPrompt = persona.systemPrompt,
                avatarColor = persona.avatarColor,
                createdAt = persona.createdAt,
                isActive = persona.isActive,
                traitsJson = gson.toJson(persona.traits),
                conversationStyle = persona.conversationStyle.name
            )
        }

        fun PersonaEntity.toPersona(): Persona {
            return Persona(
                id = this.id,
                name = this.name,
                description = this.description,
                systemPrompt = this.systemPrompt,
                avatarColor = this.avatarColor,
                createdAt = this.createdAt,
                isActive = this.isActive,
                traits = gson.fromJson(traitsJson, Array<String>::class.java).toList(),
                conversationStyle = ConversationStyle.valueOf(this.conversationStyle)
            )
        }
    }
}

@Entity(tableName = "messages")
@TypeConverters(Converters::class)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val role: String,
    val personaId: String,
    val timestamp: Long,
    val modelUsed: String? = null,
    val tokensUsed: Int? = null,
    val responseTimeMs: Long? = null,
    val embeddingGenerated: Boolean = false
) {
    companion object {
        fun fromMessage(message: Message): MessageEntity {
            return MessageEntity(
                id = message.id,
                content = message.content,
                role = message.role.name,
                personaId = message.personaId,
                timestamp = message.timestamp,
                modelUsed = message.metadata?.modelUsed,
                tokensUsed = message.metadata?.tokensUsed,
                responseTimeMs = message.metadata?.responseTimeMs,
                embeddingGenerated = message.metadata?.embeddingGenerated ?: false
            )
        }

        fun MessageEntity.toMessage(): Message {
            return Message(
                id = this.id,
                content = this.content,
                role = MessageRole.valueOf(this.role),
                personaId = this.personaId,
                timestamp = this.timestamp,
                metadata = if (modelUsed != null || tokensUsed != null || responseTimeMs != null) {
                    MessageMetadata(
                        modelUsed = modelUsed,
                        tokensUsed = tokensUsed,
                        responseTimeMs = responseTimeMs,
                        embeddingGenerated = embeddingGenerated
                    )
                } else null
            )
        }
    }
}

@Entity(tableName = "memories")
@TypeConverters(Converters::class)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: String,
    val personaId: String?,
    val embeddingJson: String?, // Stored as JSON array
    val createdAt: Long,
    val updatedAt: Long,
    val importance: Float,
    val accessCount: Int,
    val lastAccessedAt: Long?,
    val metadataJson: String?
) {
    companion object {
        private val gson = Gson()

        fun fromMemory(memory: Memory): MemoryEntity {
            return MemoryEntity(
                id = memory.id,
                content = memory.content,
                type = memory.type.name,
                personaId = memory.personaId,
                embeddingJson = memory.embedding?.let { gson.toJson(it.toList()) },
                createdAt = memory.createdAt,
                updatedAt = memory.updatedAt,
                importance = memory.importance,
                accessCount = memory.accessCount,
                lastAccessedAt = memory.lastAccessedAt,
                metadataJson = memory.metadata?.let { gson.toJson(it) }
            )
        }

        fun MemoryEntity.toMemory(): Memory {
            val embedding = embeddingJson?.let {
                gson.fromJson(it, Array<Double>::class.java)?.toFloatArray()
            }
            val metadata = metadataJson?.let {
                gson.fromJson(it, MemoryMetadata::class.java)
            }
            return Memory(
                id = this.id,
                content = this.content,
                type = MemoryType.valueOf(this.type),
                personaId = this.personaId,
                embedding = embedding,
                createdAt = this.createdAt,
                updatedAt = this.updatedAt,
                importance = this.importance,
                accessCount = this.accessCount,
                lastAccessedAt = this.lastAccessedAt,
                metadata = metadata
            )
        }
    }
}

@Entity(tableName = "reminders")
@TypeConverters(Converters::class)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val scheduledTime: Long,
    val repeatPattern: String?,
    val isCompleted: Boolean,
    val createdAt: Long,
    val priority: String,
    val relatedPersonaId: String?,
    val notificationSent: Boolean
) {
    companion object {
        fun fromReminder(reminder: Reminder): ReminderEntity {
            return ReminderEntity(
                id = reminder.id,
                title = reminder.title,
                description = reminder.description,
                scheduledTime = reminder.scheduledTime,
                repeatPattern = reminder.repeatPattern?.name,
                isCompleted = reminder.isCompleted,
                createdAt = reminder.createdAt,
                priority = reminder.priority.name,
                relatedPersonaId = reminder.relatedPersonaId,
                notificationSent = reminder.notificationSent
            )
        }

        fun ReminderEntity.toReminder(): Reminder {
            return Reminder(
                id = this.id,
                title = this.title,
                description = this.description,
                scheduledTime = this.scheduledTime,
                repeatPattern = repeatPattern?.let { RepeatPattern.valueOf(it) },
                isCompleted = this.isCompleted,
                createdAt = this.createdAt,
                priority = Priority.valueOf(this.priority),
                relatedPersonaId = this.relatedPersonaId,
                notificationSent = this.notificationSent
            )
        }
    }
}

@Entity(tableName = "user_behavior_patterns")
@TypeConverters(Converters::class)
data class UserBehaviorPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patternType: String,
    val description: String,
    val frequency: Float,
    val lastObserved: Long,
    val observationCount: Int,
    val confidence: Float,
    val relatedDataJson: String
) {
    companion object {
        private val gson = Gson()

        fun fromPattern(pattern: UserBehaviorPattern): UserBehaviorPatternEntity {
            return UserBehaviorPatternEntity(
                id = pattern.id,
                patternType = pattern.patternType.name,
                description = pattern.description,
                frequency = pattern.frequency,
                lastObserved = pattern.lastObserved,
                observationCount = pattern.observationCount,
                confidence = pattern.confidence,
                relatedDataJson = gson.toJson(pattern.relatedData)
            )
        }

        fun UserBehaviorPatternEntity.toPattern(): UserBehaviorPattern {
            return UserBehaviorPattern(
                id = this.id,
                patternType = PatternType.valueOf(this.patternType),
                description = this.description,
                frequency = this.frequency,
                lastObserved = this.lastObserved,
                observationCount = this.observationCount,
                confidence = this.confidence,
                relatedData = gson.fromJson(relatedDataJson, Map::class.java)
                    ?.mapKeys { it.key.toString() }
                    ?.mapValues { it.value.toString() }
                    ?: emptyMap()
            )
        }
    }
}

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? {
        return value?.let { gson.toJson(it.toList()) }
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? {
        return value?.let {
            gson.fromJson(it, Array<Double>::class.java)?.toFloatArray()
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return gson.fromJson(value, Array<String>::class.java).toList()
    }
}
