package com.winzone.companion.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winzone.companion.R
import com.winzone.companion.data.auth.AuthRepository
import com.winzone.companion.data.preferences.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isTosAccepted: Boolean = false,
    val isAgeConfirmed: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessageRes: Int? = null,
    val errorMessageText: String? = null,
    val isPasswordVisible: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesStore: UserPreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesStore.isTosAccepted.collect { accepted ->
                _uiState.update { it.copy(isTosAccepted = accepted) }
            }
        }
        viewModelScope.launch {
            preferencesStore.isAgeConfirmed.collect { confirmed ->
                _uiState.update { it.copy(isAgeConfirmed = confirmed) }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessageRes = null, errorMessageText = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessageRes = null, errorMessageText = null) }
    }

    fun onTosChanged(accepted: Boolean) {
        _uiState.update { it.copy(isTosAccepted = accepted, errorMessageRes = null, errorMessageText = null) }
        viewModelScope.launch { preferencesStore.setTosAccepted(accepted) }
    }

    fun onAgeChanged(confirmed: Boolean) {
        _uiState.update { it.copy(isAgeConfirmed = confirmed, errorMessageRes = null, errorMessageText = null) }
        viewModelScope.launch { preferencesStore.setAgeConfirmed(confirmed) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun signIn(onSuccess: () -> Unit) {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password.trim()

        if (email.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessageRes = R.string.error_empty_credentials,
                    errorMessageText = null
                )
            }
            return
        }

        if (!state.isTosAccepted || !state.isAgeConfirmed) {
            _uiState.update {
                it.copy(
                    errorMessageRes = R.string.error_accept_terms,
                    errorMessageText = null
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessageRes = null, errorMessageText = null) }

        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                val exception = result.exceptionOrNull()
                val rawMsg = exception?.localizedMessage ?: exception?.message ?: ""
                Timber.e(exception, "Sign-in failed for %s: %s", email, rawMsg)

                val (msgRes, detailedMsg) = when {
                    rawMsg.contains("Invalid login credentials", ignoreCase = true) ||
                    rawMsg.contains("invalid_grant", ignoreCase = true) -> {
                        Pair(R.string.error_invalid_credentials, null)
                    }
                    rawMsg.contains("Email not confirmed", ignoreCase = true) -> {
                        Pair(null, "Email address has not been confirmed yet.")
                    }
                    rawMsg.isNotBlank() -> {
                        Pair(null, rawMsg)
                    }
                    else -> {
                        Pair(R.string.error_invalid_credentials, null)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessageRes = msgRes,
                        errorMessageText = detailedMsg
                    )
                }
            }
        }
    }
}
