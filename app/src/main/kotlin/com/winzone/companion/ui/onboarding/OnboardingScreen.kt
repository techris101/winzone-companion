package com.winzone.companion.ui.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winzone.companion.R
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.DarkCard
import com.winzone.companion.ui.theme.PrimaryEmerald
import com.winzone.companion.ui.theme.TextPrimary
import com.winzone.companion.ui.theme.TextSecondary

data class OnboardingStep(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStep(R.string.onboarding_title_1, R.string.onboarding_body_1, Icons.Default.Visibility),
            OnboardingStep(R.string.onboarding_title_2, R.string.onboarding_body_2, Icons.Default.Notifications),
            OnboardingStep(R.string.onboarding_title_3, R.string.onboarding_body_3, Icons.Default.MonetizationOn)
        )
    }

    var currentStepIndex by remember { mutableStateOf(0) }
    val step = steps[currentStepIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (currentStepIndex < steps.size - 1) {
                TextButton(onClick = onFinished) {
                    Text(
                        text = stringResource(R.string.btn_skip),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Center card content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(DarkCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(step.bodyRes),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom pagination dots and action button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (index == currentStepIndex) 10.dp else 8.dp)
                            .background(
                                color = if (index == currentStepIndex) PrimaryEmerald else TextSecondary.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onFinished()
                    }
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
                    text = if (currentStepIndex == steps.size - 1) {
                        stringResource(R.string.btn_get_started)
                    } else {
                        stringResource(R.string.btn_next)
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
