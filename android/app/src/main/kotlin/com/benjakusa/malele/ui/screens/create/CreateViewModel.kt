package com.benjakusa.malele.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjakusa.malele.data.network.dto.JobDto
import com.benjakusa.malele.data.network.dto.SectionDto
import com.benjakusa.malele.data.repository.GenerationRepository
import com.benjakusa.malele.data.repository.GenerationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionInput(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Verse 1",
    val durationSeconds: Int = 20,
    val lyrics: String = "",
    val instruments: String = "",
    val vocals: String = "",
    val mood: String = "",
    val negative: String = "",
)

data class CreateUiState(
    val prompt: String = "",
    val genre: String? = null,
    val vocals: Boolean = true,
    val instrumental: Boolean = false,
    val language: String = "en",
    val durationSeconds: Int = 120,
    val sections: List<SectionInput> = emptyList(),
    val isLoading: Boolean = false,
    val submittedJob: JobDto? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val generationRepository: GenerationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    fun setPrompt(prompt: String) {
        _state.value = _state.value.copy(prompt = prompt, errorMessage = null)
    }

    fun setGenre(genre: String?) {
        _state.value = _state.value.copy(genre = genre)
    }

    fun setVocals(vocals: Boolean) {
        _state.value = _state.value.copy(vocals = vocals, instrumental = !vocals)
    }

    fun setLanguage(language: String) {
        _state.value = _state.value.copy(language = language)
    }

    fun setDuration(seconds: Int) {
        _state.value = _state.value.copy(durationSeconds = seconds)
    }

    fun addSection() {
        _state.value = _state.value.copy(
            sections = _state.value.sections + SectionInput(name = "Section ${_state.value.sections.size + 1}")
        )
    }

    fun updateSection(id: String, update: (SectionInput) -> SectionInput) {
        _state.value = _state.value.copy(
            sections = _state.value.sections.map { if (it.id == id) update(it) else it }
        )
    }

    fun removeSection(id: String) {
        _state.value = _state.value.copy(
            sections = _state.value.sections.filter { it.id != id }
        )
    }

    fun submit() {
        val s = _state.value
        if (s.prompt.isBlank()) {
            _state.value = s.copy(errorMessage = "Please describe your song")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, errorMessage = null, submittedJob = null)
            val sectionDtos = s.sections.map { input ->
                SectionDto(
                    name = input.name,
                    durationSeconds = input.durationSeconds,
                    lyrics = input.lyrics.ifBlank { null },
                    instruments = input.instruments,
                    vocals = input.vocals,
                    mood = input.mood,
                    negative = input.negative.ifBlank { null },
                )
            }.ifEmpty { null }
            val result = generationRepository.submit(
                prompt = s.prompt.trim(),
                genre = s.genre,
                vocals = s.vocals,
                instrumental = s.instrumental,
                durationSeconds = s.durationSeconds,
                language = s.language,
                sections = sectionDtos,
            )
            when (result) {
                is GenerationResult.Success -> _state.value = _state.value.copy(
                    isLoading = false,
                    submittedJob = result.job,
                )
                is GenerationResult.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                )
            }
        }
    }

    fun dismissSuccess() {
        _state.value = _state.value.copy(submittedJob = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
