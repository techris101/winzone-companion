package com.winzone.companion.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winzone.companion.data.auth.AuthRepository
import com.winzone.companion.data.auth.SessionStore
import com.winzone.companion.data.match.MatchRepository
import com.winzone.companion.data.match.MatchSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userEmail: String = "",
    val openMatch: MatchSnapshot? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val matchRepository: MatchRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.currentSession.collect { session ->
                _uiState.update { it.copy(userEmail = session?.email.orEmpty()) }
                if (session != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val session = sessionStore.currentSession.value ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val match = matchRepository.findOpenMatch(session.userId)
            _uiState.update { it.copy(isLoading = false, openMatch = match) }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}
