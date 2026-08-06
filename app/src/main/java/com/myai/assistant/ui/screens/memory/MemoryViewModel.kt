package com.myai.assistant.ui.screens.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.data.local.dao.MemoryDao
import com.myai.assistant.domain.model.Memory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class MemoryUiState(
    val memories: List<Memory> = emptyList()
)

@OptIn(FlowPreview::class)
class MemoryViewModel(
    private val memoryDao: MemoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        loadAllMemories()
    }

    private fun loadAllMemories() {
        viewModelScope.launch {
            memoryDao.getAllMemories().collect { memories ->
                _uiState.value = _uiState.value.copy(memories = memories)
            }
        }
    }

    fun searchMemories(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadAllMemories()
            } else {
                // 简单关键词搜索，实际应该用向量搜索
                memoryDao.searchMemoriesByContent(query).collect { memories ->
                    _uiState.value = _uiState.value.copy(memories = memories)
                }
            }
        }
    }
}
