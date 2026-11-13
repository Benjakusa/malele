package com.benjakusa.malele.ui.screens.songdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjakusa.malele.BuildConfig
import com.benjakusa.malele.data.local.TokenStore
import com.benjakusa.malele.data.repository.SongActions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongDetailUiState(
    val isDownloading: Boolean = false,
    val isSaving: Boolean = false,
    val toastMessage: String? = null,
    val downloadedFile: java.io.File? = null,
)

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val songActions: SongActions,
    private val tokenStore: TokenStore,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SongDetailUiState())
    val state: StateFlow<SongDetailUiState> = _state.asStateFlow()

    fun audioSource(jobId: String): String {
        val local = songActions.localAudioFile(appContext, jobId)
        if (local.exists() && local.length() > 0) {
            return local.toURI().toString()
        }
        val base = BuildConfig.BACKEND_URL.trimEnd('/')
        return "$base/v1/generations/$jobId/audio"
    }

    fun ensureLocalCopy(jobId: String, title: String) {
        if (songActions.hasLocalAudio(appContext, jobId)) return
        viewModelScope.launch {
            songActions.persistForPlayback(appContext, jobId, title)
        }
    }

    fun authToken(): String = tokenStore.getAccessToken() ?: ""

    fun prepareShare(jobId: String, title: String, onReady: (java.io.File) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDownloading = true)
            val file = songActions.downloadToCache(jobId, title)
            _state.value = _state.value.copy(isDownloading = false)
            if (file != null) {
                _state.value = _state.value.copy(downloadedFile = file)
                onReady(file)
            } else {
                _state.value = _state.value.copy(toastMessage = "Failed to download audio")
            }
        }
    }

    fun saveToDevice(context: android.content.Context, jobId: String, title: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val uri = songActions.saveToDevice(context, jobId, title)
            _state.value = _state.value.copy(
                isSaving = false,
                toastMessage = if (uri != null) "Saved to Music/Malele/" else "Save failed",
            )
        }
    }

    fun shareIntent(context: android.content.Context, file: java.io.File): android.content.Intent {
        return songActions.shareIntent(context, file)
    }

    fun clearToast() {
        _state.value = _state.value.copy(toastMessage = null)
    }
}
