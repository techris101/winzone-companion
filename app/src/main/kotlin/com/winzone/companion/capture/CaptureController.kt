package com.winzone.companion.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureController @Inject constructor() {

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _currentMatchId = MutableStateFlow<String?>(null)
    val currentMatchId: StateFlow<String?> = _currentMatchId.asStateFlow()

    private val _lastAnalysis = MutableStateFlow<FrameAnalysis?>(null)
    val lastAnalysis: StateFlow<FrameAnalysis?> = _lastAnalysis.asStateFlow()

    private val _manualTeamA = MutableStateFlow<String?>(null)
    val manualTeamA: StateFlow<String?> = _manualTeamA.asStateFlow()

    private val _manualTeamB = MutableStateFlow<String?>(null)
    val manualTeamB: StateFlow<String?> = _manualTeamB.asStateFlow()

    private val _isFallbackDialogOpen = MutableStateFlow(false)
    val isFallbackDialogOpen: StateFlow<Boolean> = _isFallbackDialogOpen.asStateFlow()

    private val _isSamplerPaused = MutableStateFlow(false)
    val isSamplerPaused: StateFlow<Boolean> = _isSamplerPaused.asStateFlow()

    fun updateCapturing(active: Boolean, matchId: String? = null) {
        _isCapturing.value = active
        _currentMatchId.value = matchId
        if (!active) {
            _lastAnalysis.value = null
            _isFallbackDialogOpen.value = false
            _isSamplerPaused.value = false
        }
    }

    fun updateAnalysis(analysis: FrameAnalysis) {
        _lastAnalysis.value = analysis
    }

    fun setFallbackTeamNames(teamA: String, teamB: String) {
        _manualTeamA.value = teamA
        _manualTeamB.value = teamB
        _isFallbackDialogOpen.value = false
        _isSamplerPaused.value = false
    }

    fun showFallbackDialog() {
        _isFallbackDialogOpen.value = true
        _isSamplerPaused.value = true
    }

    fun dismissFallbackDialog() {
        _isFallbackDialogOpen.value = false
        _isSamplerPaused.value = false
    }

    fun clearManualTeams() {
        _manualTeamA.value = null
        _manualTeamB.value = null
    }
}
