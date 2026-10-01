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
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isTosAccepted: Boolean = false,
    val isAgeConfirmed: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessageRes: Int? = null,
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
        _uiState.update { it.copy(email = email, errorMessageRes = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessageRes = null) }
    }

    fun onTosChanged(accepted: Boolean) {
        _uiState.update { it.copy(isTosAccepted = accepted, errorMessageRes = null) }
        viewModelScope.launch { preferencesStore.setTosAccepted(accepted) }
    }

    fun onAgeChanged(confirmed: Boolean) {
        _uiState.update { it.copy(isAgeConfirmed = confirmed, errorMessageRes = null) }
        viewModelScope.launch { preferencesStore.setAgeConfirmed(confirmed) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun signIn(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_empty_credentials) }
            return
        }

        if (!state.isTosAccepted || !state.isAgeConfirmed) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_accept_terms) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }

        viewModelScope.launch {
            val result = authRepository.signIn(state.email.trim(), state.password.trim())
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessageRes = R.string.error_invalid_credentials
                    )
                }
            }
        }
    }
}
