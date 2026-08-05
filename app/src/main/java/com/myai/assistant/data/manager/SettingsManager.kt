package com.myai.assistant.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Manages app-wide settings and user preferences using DataStore
 */
class SettingsManager(private val context: Context) {
    
    // Preference keys
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val API_PROVIDER = stringPreferencesKey("api_provider")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val DEFAULT_PERSONA_ID = stringPreferencesKey("default_persona_id")
        val ENABLE_REMINDERS = booleanPreferencesKey("enable_reminders")
        val ENABLE_LEARNING = booleanPreferencesKey("enable_learning")
        val MEMORY_RETENTION_DAYS = intPreferencesKey("memory_retention_days")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val AUTO_SAVE_CONVERSATIONS = booleanPreferencesKey("auto_save_conversations")
        val EMBEDDING_MODEL = stringPreferencesKey("embedding_model")
        val MAX_CONTEXT_MESSAGES = intPreferencesKey("max_context_messages")
        val TEMPERATURE = floatPreferencesKey("temperature")
    }

    // API Settings
    val apiKeyFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[API_KEY]
        }

    val apiProviderFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[API_PROVIDER] ?: "openai"
        }

    val defaultModelFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_MODEL] ?: "gpt-4"
        }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }

    suspend fun saveApiProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[API_PROVIDER] = provider
        }
    }

    suspend fun saveDefaultModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_MODEL] = model
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }

    // Persona Settings
    val defaultPersonaIdFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_PERSONA_ID]
        }

    suspend fun saveDefaultPersonaId(personaId: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PERSONA_ID] = personaId
        }
    }

    // Feature Toggles
    val enableRemindersFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ENABLE_REMINDERS] ?: true
        }

    val enableLearningFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ENABLE_LEARNING] ?: true
        }

    suspend fun toggleReminders(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_REMINDERS] = enabled
        }
    }

    suspend fun toggleLearning(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_LEARNING] = enabled
        }
    }

    // Memory Settings
    val memoryRetentionDaysFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[MEMORY_RETENTION_DAYS] ?: 90
        }

    suspend fun saveMemoryRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[MEMORY_RETENTION_DAYS] = days
        }
    }

    // UI Settings
    val themeModeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: "system"
        }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    // Advanced Settings
    val maxContextMessagesFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[MAX_CONTEXT_MESSAGES] ?: 20
        }

    val temperatureFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[TEMPERATURE] ?: 0.7f
        }

    suspend fun saveMaxContextMessages(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_CONTEXT_MESSAGES] = count
        }
    }

    suspend fun saveTemperature(temp: Float) {
        context.dataStore.edit { preferences ->
            preferences[TEMPERATURE] = temp
        }
    }

    suspend fun saveEmbeddingModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[EMBEDDING_MODEL] = model
        }
    }

    val embeddingModelFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[EMBEDDING_MODEL] ?: "text-embedding-ada-002"
        }
}
