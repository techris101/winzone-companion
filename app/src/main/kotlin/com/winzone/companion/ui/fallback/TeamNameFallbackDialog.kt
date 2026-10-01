package com.winzone.companion.ui.fallback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.winzone.companion.R
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary

@Composable
fun TeamNameFallbackDialog(
    initialTeamA: String = "",
    initialTeamB: String = "",
    onSubmit: (teamA: String, teamB: String) -> Unit,
    onCancel: () -> Unit
) {
    var teamA by remember { mutableStateOf(initialTeamA) }
    var teamB by remember { mutableStateOf(initialTeamB) }

    AlertDialog(
        onDismissRequest = { /* non-dismissible outside */ },
        title = {
            Text(
                text = stringResource(R.string.fallback_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.fallback_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = teamA,
                    onValueChange = { teamA = it },
                    label = { Text(stringResource(R.string.label_team_a)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = PrimaryEmerald,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = teamB,
                    onValueChange = { teamB = it },
                    label = { Text(stringResource(R.string.label_team_b)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = PrimaryEmerald,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(teamA.trim(), teamB.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryEmerald,
                    contentColor = DarkBackground
                )
            ) {
                Text(stringResource(R.string.btn_submit))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
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
