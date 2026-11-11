package com.benjakusa.malele.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjakusa.malele.data.repository.AuthRepository
import com.benjakusa.malele.domain.model.AuthResult
import com.benjakusa.malele.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Email and password are required")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(email.trim(), password)) {
                is AuthResult.Success -> _state.value = AuthUiState(user = result.user)
                is AuthResult.Error -> _state.value = AuthUiState(errorMessage = result.message)
            }
        }
    }

    fun register(email: String, password: String, displayName: String?) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Email and password are required")
            return
        }
        if (password.length < 8) {
            _state.value = _state.value.copy(errorMessage = "Password must be at least 8 characters")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.register(email.trim(), password, displayName?.trim()?.takeIf { it.isNotBlank() })) {
                is AuthResult.Success -> _state.value = AuthUiState(user = result.user)
                is AuthResult.Error -> _state.value = AuthUiState(errorMessage = result.message)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
