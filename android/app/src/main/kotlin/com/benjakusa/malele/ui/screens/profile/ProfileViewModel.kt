package com.benjakusa.malele.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjakusa.malele.data.local.ThemePreferences
import com.benjakusa.malele.data.network.dto.UserProfileDto
import com.benjakusa.malele.data.repository.AuthRepository
import com.benjakusa.malele.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserProfileDto? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showEditNameDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val savedMessage: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val user = userRepository.getMe()
            _state.value = _state.value.copy(
                isLoading = false,
                user = user,
                errorMessage = if (user == null) "Could not load profile" else null,
            )
        }
    }

    fun openEditNameDialog() {
        _state.value = _state.value.copy(showEditNameDialog = true)
    }

    fun dismissEditNameDialog() {
        _state.value = _state.value.copy(showEditNameDialog = false)
    }

    fun saveDisplayName(name: String) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Name cannot be empty")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val updated = userRepository.updateDisplayName(name.trim())
            val currentUser = _state.value.user
            val newProfile = updated ?: currentUser?.copy(displayName = name.trim())
            _state.value = _state.value.copy(
                isSaving = false,
                user = newProfile,
                showEditNameDialog = false,
                savedMessage = "Display name updated",
            )
        }
    }

    fun openDeleteAccountDialog() {
        _state.value = _state.value.copy(showDeleteAccountDialog = true)
    }

    fun dismissDeleteAccountDialog() {
        _state.value = _state.value.copy(showDeleteAccountDialog = false)
    }

    fun setTheme(dark: Boolean) {
        viewModelScope.launch {
            themePreferences.setDarkMode(dark)
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(savedMessage = null, errorMessage = null)
    }
}
