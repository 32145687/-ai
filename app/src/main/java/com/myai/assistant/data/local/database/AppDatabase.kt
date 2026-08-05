package com.myai.assistant.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myai.assistant.data.local.dao.*
import com.myai.assistant.data.local.entity.*

@Database(
    entities = [
        PersonaEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        ReminderEntity::class,
        UserBehaviorPatternEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personaDao(): PersonaDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun userBehaviorPatternDao(): UserBehaviorPatternDao

    companion object {
        const val DATABASE_NAME = "ai_assistant_db"
    }
}
