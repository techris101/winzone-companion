package com.winzone.companion.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.winzone.companion.BuildConfig
import com.winzone.companion.R
import com.winzone.companion.WinZoneApp
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary

@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit
) {
    val logs = remember { WinZoneApp.ringBufferTree.getLogs() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = TextPrimary
                )
            }
            Text(
                text = stringResource(R.string.diag_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device & Environment Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.diag_device, "${Build.MANUFACTURER} ${Build.MODEL}", Build.VERSION.SDK_INT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "App: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Release: ${if (BuildConfig.DEBUG) "Debug" else "Production"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recent Logs (${logs.size}):",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // In-memory logs list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(logs.reversed()) { logEntry ->
                Text(
                    text = logEntry,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
