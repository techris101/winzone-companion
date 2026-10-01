package com.winzone.companion.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.winzone.companion.BuildConfig
import com.winzone.companion.R
import com.winzone.companion.WinZoneApp
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.StatusError
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary
import com.winzone.companion.util.Constants
import java.io.File

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current

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
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card 1: App Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    subtitle = stringResource(R.string.settings_profile, Constants.PROFILE_ID_DEFAULT)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Diagnostics & Logs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsClickableRow(
                    icon = Icons.Default.Assessment,
                    title = stringResource(R.string.settings_diagnostics),
                    onClick = onOpenDiagnostics
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsClickableRow(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.settings_open_logs),
                    onClick = { shareLogs(context) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Sign Out
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsClickableRow(
                    icon = Icons.Default.ExitToApp,
                    title = stringResource(R.string.btn_sign_out),
                    titleColor = StatusError,
                    onClick = onSignedOut
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryEmerald)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (titleColor == StatusError) StatusError else PrimaryEmerald)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun shareLogs(context: Context) {
    try {
        val logs = WinZoneApp.ringBufferTree.getLogs().joinToString("\n")
        val cacheFile = File(context.cacheDir, "logs/winzone_logs.txt").apply {
            parentFile?.mkdirs()
            writeText(logs)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.btn_share_logs)))
    } catch (_: Exception) {}
}
