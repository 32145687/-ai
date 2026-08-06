package com.myai.assistant.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.data.local.dao.MemoryDao
import com.myai.assistant.data.manager.SettingsManager
import com.myai.assistant.domain.model.AiModelType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val selectedModel: AiModelType = AiModelType.GPT_4O,
    val memoryCount: Int = 0
)

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val memoryDao: MemoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadMemoryCount()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsManager.getApiKey().collect { key ->
                _uiState.value = _uiState.value.copy(apiKey = key ?: "")
            }
        }
        viewModelScope.launch {
            settingsManager.getBaseUrl().collect { url ->
                _uiState.value = _uiState.value.copy(baseUrl = url)
            }
        }
        viewModelScope.launch {
            settingsManager.getSelectedModel().collect { model ->
                _uiState.value = _uiState.value.copy(selectedModel = model)
            }
        }
    }

    private fun loadMemoryCount() {
        viewModelScope.launch {
            memoryDao.countAllMemories().collect { count ->
                _uiState.value = _uiState.value.copy(memoryCount = count)
            }
        }
    }

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun updateBaseUrl(url: String) {
        _uiState.value = _uiState.value.copy(baseUrl = url)
    }

    fun updateModel(model: AiModelType) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun saveSettings(context: Context) {
        viewModelScope.launch {
            settingsManager.saveApiKey(_uiState.value.apiKey)
            settingsManager.saveBaseUrl(_uiState.value.baseUrl)
            settingsManager.saveSelectedModel(_uiState.value.selectedModel)
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            memoryDao.deleteAllMemories()
        }
    }

    fun exportData(context: Context) {
        viewModelScope.launch {
            try {
                val memories = memoryDao.getAllMemories()
                val exportFile = File(context.getExternalFilesDir(null), "ai_assistant_backup_${System.currentTimeMillis()}.json")
                
                FileWriter(exportFile).use { writer ->
                    writer.append("[\n")
                    memories.forEachIndexed { index, memory ->
                        writer.append("  {\n")
                        writer.append("    \"id\": ${memory.id},\n")
                        writer.append("    \"content\": \"${memory.content.replace("\"", "\\\"")}\",\n")
                        writer.append("    \"type\": \"${memory.type}\",\n")
                        writer.append("    \"personaId\": ${memory.personaId},\n")
                        writer.append("    \"importance\": ${memory.importance},\n")
                        writer.append("    \"createdAt\": \"${memory.createdAt}\"\n")
                        writer.append("  }")
                        if (index < memories.size - 1) writer.append(",")
                        writer.append("\n")
                    }
                    writer.append("]\n")
                }
                // 实际应用中应显示成功提示或分享意图
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
