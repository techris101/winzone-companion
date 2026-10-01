package com.winzone.companion.ui.join

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winzone.companion.data.match.MatchRepository
import com.winzone.companion.data.match.MatchSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinUiState(
    val matchSnapshot: MatchSnapshot? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class JoinViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val matchId: String = savedStateHandle.get<String>("matchId").orEmpty()

    private val _uiState = MutableStateFlow(JoinUiState())
    val uiState: StateFlow<JoinUiState> = _uiState.asStateFlow()

    init {
        if (matchId.isNotEmpty()) {
            join(matchId)
        }
    }

    private fun join(id: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = matchRepository.joinMatch(id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        matchSnapshot = result.getOrNull()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to join match"
                    )
                }
            }
        }
    }
}
