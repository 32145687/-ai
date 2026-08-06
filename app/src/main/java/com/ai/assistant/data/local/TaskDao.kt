package com.ai.assistant.data.local

import androidx.room.*
import com.ai.assistant.domain.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY scheduledTime ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND scheduledTime > :now ORDER BY scheduledTime ASC")
    fun getUpcomingTasks(now: Long = System.currentTimeMillis()): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    @Query("SELECT * FROM tasks WHERE relatedPersonaId = :personaId ORDER BY scheduledTime ASC")
    fun getTasksByPersona(personaId: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun toggleTaskCompletion(taskId: String, completed: Boolean)

    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND scheduledTime < :threshold")
    suspend fun deleteOldCompletedTasks(threshold: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
}
