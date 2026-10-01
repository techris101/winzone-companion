package com.winzone.companion.ui.capture

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.winzone.companion.agreement.AgreementGate
import com.winzone.companion.capture.CaptureController
import com.winzone.companion.capture.CaptureService
import com.winzone.companion.capture.FrameAnalysis
import com.winzone.companion.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureController: CaptureController,
    private val agreementGate: AgreementGate
) : ViewModel() {

    val isCapturing: StateFlow<Boolean> = captureController.isCapturing
    val lastAnalysis: StateFlow<FrameAnalysis?> = captureController.lastAnalysis
    val isFallbackDialogOpen: StateFlow<Boolean> = captureController.isFallbackDialogOpen
    val isKickoffTriggered: StateFlow<Boolean> = agreementGate.isKickoffTriggered
    val isFinalizeTriggered: StateFlow<Boolean> = agreementGate.isFinalizeTriggered

    fun submitFallbackTeamNames(teamA: String, teamB: String) {
        captureController.setFallbackTeamNames(teamA, teamB)
    }

    fun dismissFallbackDialog() {
        captureController.dismissFallbackDialog()
    }

    fun stopCapture(context: Context) {
        val intent = Intent(context, CaptureService::class.java).apply {
            action = Constants.ACTION_STOP_CAPTURE
        }
        context.startService(intent)
    }
}
