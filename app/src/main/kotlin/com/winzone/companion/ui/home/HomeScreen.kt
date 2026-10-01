package com.winzone.companion.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.winzone.companion.R
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.DarkSurface
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onJoinMatch: (matchId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryEmerald
                )
                Text(
                    text = stringResource(R.string.greeting_user, uiState.userEmail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Row {
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh),
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = { viewModel.signOut(onSignedOut) }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = stringResource(R.string.btn_sign_out),
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryEmerald)
            }
        } else if (uiState.openMatch != null) {
            val match = uiState.openMatch!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_open_match_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.home_match_id, match.shortId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )

                    if (match.stake != null) {
                        Text(
                            text = stringResource(R.string.home_stake, "%.2f".format(match.stake)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onJoinMatch(match.matchId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryEmerald,
                            contentColor = DarkBackground
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_join_match),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.home_no_match_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.home_no_match_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = viewModel::refresh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.btn_refresh),
                            color = PrimaryEmerald
                        )
                    }
                }
            }
        }
    }
}
