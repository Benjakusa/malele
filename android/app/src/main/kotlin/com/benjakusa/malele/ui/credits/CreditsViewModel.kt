package com.benjakusa.malele.ui.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjakusa.malele.data.repository.CreditsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreditsUiState(
    val balance: Int = 0,
    val reserved: Int = 0,
    val available: Int = 0,
    val isLoadingBalance: Boolean = false,
    val isPurchasing: Boolean = false,
    val purchaseMessage: String? = null,
    val errorMessage: String? = null,
    // set when a purchase is pending STK confirmation
    val pendingCheckoutId: String? = null,
)

@HiltViewModel
class CreditsViewModel @Inject constructor(
    private val repository: CreditsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreditsUiState())
    val state: StateFlow<CreditsUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingBalance = true)
            val dto = repository.getBalance()
            _state.value = if (dto != null) {
                _state.value.copy(
                    balance = dto.balance,
                    reserved = dto.reserved,
                    available = dto.available,
                    isLoadingBalance = false,
                )
            } else {
                _state.value.copy(isLoadingBalance = false)
            }
        }
    }

    fun purchase(phone: String, amountKes: Int) {
        if (_state.value.isPurchasing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isPurchasing = true,
                errorMessage = null,
                purchaseMessage = null,
            )
            val result = repository.purchase(phone, amountKes)
            result.fold(
                onSuccess = { resp ->
                    _state.value = _state.value.copy(
                        isPurchasing = false,
                        pendingCheckoutId = resp.checkoutRequestId,
                        purchaseMessage = resp.message.ifBlank {
                            "Enter your M-Pesa PIN to complete payment."
                        },
                    )
                    startPolling(resp.checkoutRequestId)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isPurchasing = false,
                        errorMessage = e.message ?: "Could not start payment",
                    )
                },
            )
        }
    }

    private fun startPolling(checkoutId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            // Poll verify every 3s up to 90s. On terminal status, refresh balance.
            repeat(30) {
                delay(3000)
                val status = repository.verify(checkoutId)
                when (status) {
                    "completed" -> {
                        _state.value = _state.value.copy(
                            pendingCheckoutId = null,
                            purchaseMessage = "Payment received. Credits added.",
                            isPurchasing = false,
                        )
                        refresh()
                        return@launch
                    }
                    "failed" -> {
                        _state.value = _state.value.copy(
                            pendingCheckoutId = null,
                            isPurchasing = false,
                            errorMessage = "Payment failed or was cancelled.",
                        )
                        return@launch
                    }
                    // "pending", "not_found", "query_failed" → keep waiting
                }
            }
            // Timed out — balance refresh will still catch it if callback landed late
            _state.value = _state.value.copy(
                isPurchasing = false,
                pendingCheckoutId = null,
                errorMessage = "Payment confirmation timed out. If you were charged, pull to refresh in a moment.",
            )
            refresh()
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            errorMessage = null,
            purchaseMessage = null,
        )
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
