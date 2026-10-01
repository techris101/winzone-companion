package com.winzone.companion.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.winzone.companion.R
import com.winzone.companion.ui.fallback.TeamNameFallbackDialog
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.DarkSurface
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.StatusError
import com.winzone.companion.ui.theme.StatusWarning
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary
import com.winzone.companion.util.Time

@Composable
fun CaptureScreen(
    matchId: String,
    onStopCaptureConfirmed: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isCapturing by viewModel.isCapturing.collectAsState()
    val lastAnalysis by viewModel.lastAnalysis.collectAsState()
    val isFallbackOpen by viewModel.isFallbackDialogOpen.collectAsState()
    val isKickoffTriggered by viewModel.isKickoffTriggered.collectAsState()
    val isFinalizeTriggered by viewModel.isFinalizeTriggered.collectAsState()

    var showStopConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isCapturing) {
        if (!isCapturing && !showStopConfirmDialog) {
            // Service was stopped externally
            onStopCaptureConfirmed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(R.string.capture_active_header),
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryEmerald,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            // Agreement Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (statusText, statusColor, statusIcon) = when {
                        isFinalizeTriggered -> Triple(
                            stringResource(R.string.capture_agreement_final),
                            PrimaryEmerald,
                            Icons.Default.CheckCircle
                        )
                        isKickoffTriggered -> Triple(
                            stringResource(R.string.capture_agreement_kickoff),
                            PrimaryEmerald,
                            Icons.Default.CheckCircle
                        )
                        else -> Triple(
                            stringResource(R.string.capture_agreement_waiting),
                            StatusWarning,
                            Icons.Default.HourglassEmpty
                        )
                    }

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = statusColor,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Last Analysis Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    val analysis = lastAnalysis

                    val layoutStr = analysis?.layout?.name ?: "INITIALIZING"
                    val scoreStr = if (analysis?.scoreA != null && analysis.scoreB != null) {
                        "${analysis.scoreA} - ${analysis.scoreB}"
                    } else {
                        "-- : --"
                    }
                    val teamAStr = analysis?.teamA ?: "Team A"
                    val teamBStr = analysis?.teamB ?: "Team B"
                    val clockStr = Time.formatClockSeconds(analysis?.clockSeconds)
                    val confStr = analysis?.let { "%.0f".format(it.ocrConfidence * 100) } ?: "0"

                    Text(
                        text = stringResource(R.string.capture_layout, layoutStr),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$teamAStr vs $teamBStr",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.capture_score, analysis?.scoreA ?: 0, analysis?.scoreB ?: 0),
                        style = MaterialTheme.typography.headlineMedium,
                        color = PrimaryEmerald
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.capture_clock, clockStr),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.capture_confidence, confStr),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Stop Capture Button
        Button(
            onClick = { showStopConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusError,
                contentColor = TextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.StopCircle,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.btn_stop_capture),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    // Stop Confirmation Dialog
    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_stop_capture_title),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_stop_capture_body),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmDialog = false
                        viewModel.stopCapture(context)
                        onStopCaptureConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(stringResource(R.string.btn_confirm_stop))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStopConfirmDialog = false }) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        color = TextSecondary
                    )
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Team Name Fallback Dialog Overlay
    if (isFallbackOpen) {
        TeamNameFallbackDialog(
            onSubmit = { teamA, teamB ->
                viewModel.submitFallbackTeamNames(teamA, teamB)
            },
            onCancel = {
                viewModel.dismissFallbackDialog()
            }
        )
    }
}
