package com.winzone.companion.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenShare
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.winzone.companion.R
import com.winzone.companion.capture.CaptureService
import com.winzone.companion.util.Constants
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary

@Composable
fun PermissionScreen(
    matchId: String,
    side: String,
    onCaptureStarted: () -> Unit
) {
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }

    var projectionResultCode by remember { mutableStateOf<Int?>(null) }
    var projectionData by remember { mutableStateOf<Intent?>(null) }

    // Launcher for Notification Permission
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Launcher for MediaProjection
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            projectionResultCode = result.resultCode
            projectionData = result.data
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.perm_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.perm_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card 1: Notification Permission
        PermissionItemCard(
            title = stringResource(R.string.perm_notif_title),
            body = stringResource(R.string.perm_notif_body),
            icon = Icons.Default.Notifications,
            isGranted = hasNotificationPermission,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Battery Optimization (Optional)
        PermissionItemCard(
            title = stringResource(R.string.perm_battery_title),
            body = stringResource(R.string.perm_battery_body),
            icon = Icons.Default.BatteryAlert,
            isGranted = isBatteryOptimizationIgnored,
            isOptional = true,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Screen Capture Consent
        PermissionItemCard(
            title = stringResource(R.string.perm_capture_title),
            body = stringResource(R.string.perm_capture_body),
            icon = Icons.Default.ScreenShare,
            isGranted = projectionResultCode != null,
            enabled = hasNotificationPermission,
            onGrant = {
                val mpm = context.getSystemService(MediaProjectionManager::class.java)
                mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Start Capture Button
        val canStart = hasNotificationPermission && projectionResultCode != null && projectionData != null
        Button(
            onClick = {
                val intent = Intent(context, CaptureService::class.java).apply {
                    putExtra(Constants.EXTRA_RESULT_CODE, projectionResultCode!!)
                    putExtra(Constants.EXTRA_RESULT_DATA, projectionData!!)
                    putExtra(Constants.EXTRA_MATCH_ID, matchId)
                    putExtra(Constants.EXTRA_MY_SIDE, side)
                }
                ContextCompat.startForegroundService(context, intent)
                onCaptureStarted()
            },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryEmerald,
                contentColor = DarkBackground
            )
        ) {
            Text(
                text = stringResource(R.string.btn_start_capture),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    body: String,
    icon: ImageVector,
    isGranted: Boolean,
    isOptional: Boolean = false,
    enabled: Boolean = true,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) PrimaryEmerald else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.btn_granted),
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onGrant,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isOptional) stringResource(R.string.btn_allow) else stringResource(R.string.btn_grant),
                        color = if (enabled) PrimaryEmerald else TextSecondary
                    )
                }
            }
        }
    }
}
