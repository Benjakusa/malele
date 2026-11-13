package com.benjakusa.malele.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjakusa.malele.data.network.dto.JobDto
import com.benjakusa.malele.data.network.dto.LyricsGenerateRequest
import com.benjakusa.malele.data.network.dto.SectionDto
import com.benjakusa.malele.data.local.TokenStore
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
    val errorCode: String? = null,
    val lyricsLoadingSectionId: String? = null,
    val jobState: String = "",
    val jobProvider: String = "",
    val audioReady: Boolean = false,
    val isPolling: Boolean = false,
    val audioUrl: String = "",
    val authToken: String = "",
    val isSaving: Boolean = false,
    val savedSongId: String? = null,
)

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val generationRepository: GenerationRepository,
    private val tokenStore: TokenStore,
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

    fun writeLyricsForSection(sectionId: String) {
        val section = _state.value.sections.find { it.id == sectionId } ?: return
        val prompt = _state.value.prompt
        if (prompt.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Please describe the song first")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(lyricsLoadingSectionId = sectionId, errorMessage = null)
            try {
                val request = LyricsGenerateRequest(
                    theme = "$prompt. Section: ${section.name}. Mood: ${section.mood.ifBlank { "unspecified" }}",
                    genre = _state.value.genre,
                    language = _state.value.language,
                    mood = section.mood.ifBlank { null },
                    sections = listOf(section.name),
                )
                val response = generationRepository.lyricsApi.generate(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    val lyricsText = body?.sections?.get(section.name) 
                        ?: body?.sections?.values?.firstOrNull()
                        ?: ""
                    if (lyricsText.isNotBlank()) {
                        updateSection(sectionId) { it.copy(lyrics = lyricsText) }
                    } else {
                        _state.value = _state.value.copy(errorMessage = "AI returned empty lyrics")
                    }
                } else {
                    _state.value = _state.value.copy(
                        errorMessage = "Lyrics AI failed: HTTP ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = "Lyrics AI error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(lyricsLoadingSectionId = null)
            }
        }
    }

    private var pollJobCoroutine: kotlinx.coroutines.Job? = null

    fun pollJob(jobId: String) {
        pollJobCoroutine?.cancel()
        pollJobCoroutine = viewModelScope.launch {
            _state.value = _state.value.copy(isPolling = true)
            var attempts = 0
            while (attempts < 30) {  // max 60 seconds
                kotlinx.coroutines.delay(2000)
                attempts++
                val job = generationRepository.pollJob(jobId)
                if (job != null) {
                    _state.value = _state.value.copy(
                        jobState = job.state,
                        jobProvider = job.provider,
                    )
                    if (job.state == "completed") {
                        val backendUrl = com.benjakusa.malele.BuildConfig.BACKEND_URL.trimEnd('/')
                        _state.value = _state.value.copy(
                            isPolling = false,
                            audioReady = true,
                            audioUrl = "$backendUrl/v1/generations/${job.id}/audio",
                            authToken = tokenStore.getAccessToken() ?: "",
                        )
                        return@launch
                    }
                    if (job.state == "failed" || job.state == "cancelled") {
                        _state.value = _state.value.copy(
                            isPolling = false,
                            errorMessage = "Job ${job.state}: ${job.errorMessage ?: "unknown"}",
                        )
                        return@launch
                    }
                }
            }
            _state.value = _state.value.copy(
                isPolling = false,
                errorMessage = "Polling timed out after 60 seconds",
            )
        }
    }

    fun stopPolling() {
        pollJobCoroutine?.cancel()
        pollJobCoroutine = null
        _state.value = _state.value.copy(isPolling = false)
    }

    fun resetSubmit() {
        pollJobCoroutine?.cancel()
        pollJobCoroutine = null
        _state.value = _state.value.copy(
            submittedJob = null,
            jobState = "",
            jobProvider = "",
            audioReady = false,
            isPolling = false,
            errorMessage = null,
        )
    }

    fun saveJob(title: String? = null) {
        val job = _state.value.submittedJob ?: return
        if (_state.value.savedSongId != null || _state.value.isSaving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val song = generationRepository.saveJob(job.id, title)
            if (song != null) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    savedSongId = song.id,
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save song",
                )
            }
        }
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
                is GenerationResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        submittedJob = result.job,
                        jobState = result.job.state,
                        jobProvider = result.job.provider,
                        audioReady = false,
                    )
                    pollJob(result.job.id)
                }
                is GenerationResult.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                    errorCode = result.code,
                )
            }
        }
    }

    fun dismissSuccess() {
        _state.value = _state.value.copy(submittedJob = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null, errorCode = null)
    }
}
