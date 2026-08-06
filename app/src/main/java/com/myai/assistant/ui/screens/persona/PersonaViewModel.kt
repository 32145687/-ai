package com.myai.assistant.ui.screens.persona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.data.local.dao.PersonaDao
import com.myai.assistant.domain.model.Persona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PersonaUiState(
    val personas: List<Persona> = emptyList(),
    val activePersonaId: Long? = null
)

class PersonaViewModel(
    private val personaDao: PersonaDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonaUiState())
    val uiState: StateFlow<PersonaUiState> = _uiState.asStateFlow()

    init {
        loadPersonas()
    }

    private fun loadPersonas() {
        viewModelScope.launch {
            personaDao.getAllPersonasWithMemoryCount().collect { personas ->
                val activeId = personas.find { it.isActive }?.id
                _uiState.value = _uiState.value.copy(
                    personas = personas,
                    activePersonaId = activeId
                )
            }
        }
    }

    fun createPersona(persona: Persona) {
        viewModelScope.launch {
            personaDao.insertPersona(persona.copy(isActive = false))
        }
    }

    fun updatePersona(id: Long, persona: Persona) {
        viewModelScope.launch {
            personaDao.updatePersona(persona.copy(id = id))
        }
    }

    fun deletePersona(id: Long) {
        viewModelScope.launch {
            personaDao.deletePersona(id)
        }
    }

    fun activatePersona(id: Long) {
        viewModelScope.launch {
            // 先取消所有激活状态
            personaDao.deactivateAllPersonas()
            // 激活选中的
            personaDao.activatePersona(id)
        }
    }
}
