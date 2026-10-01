package com.winzone.companion.ui.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.winzone.companion.R
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.StatusError
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary

@Composable
fun JoinScreen(
    onContinueToPermissions: (matchId: String, side: String) -> Unit,
    viewModel: JoinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(color = PrimaryEmerald)
        } else if (uiState.matchSnapshot != null) {
            val snapshot = uiState.matchSnapshot!!
            val playerRoleText = if (snapshot.side.wireName == "b") {
                stringResource(R.string.join_player_role, "Two", "B")
            } else {
                stringResource(R.string.join_player_role, "One", "A")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.join_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.home_match_id, snapshot.shortId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = playerRoleText,
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryEmerald
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.join_status, snapshot.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            onContinueToPermissions(snapshot.matchId, snapshot.side.wireName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryEmerald,
                            contentColor = DarkBackground
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_continue_permissions),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        } else {
            Text(
                text = uiState.errorMessage ?: stringResource(R.string.join_not_participant),
                style = MaterialTheme.typography.bodyLarge,
                color = StatusError
            )
        }
    }
}
