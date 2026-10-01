package com.winzone.companion.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.cd_logo),
            tint = PrimaryEmerald,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )

        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            label = { Text(stringResource(R.string.label_email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = DarkCard,
                focusedLabelColor = PrimaryEmerald,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text(stringResource(R.string.label_password)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = viewModel::togglePasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.cd_password_visibility),
                        tint = TextSecondary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = DarkCard,
                focusedLabelColor = PrimaryEmerald,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ToS Checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.onTosChanged(!uiState.isTosAccepted) }
        ) {
            Checkbox(
                checked = uiState.isTosAccepted,
                onCheckedChange = viewModel::onTosChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryEmerald,
                    checkmarkColor = DarkBackground
                )
            )
            Text(
                text = stringResource(R.string.checkbox_tos),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Age Gate Checkbox (18+)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.onAgeChanged(!uiState.isAgeConfirmed) }
        ) {
            Checkbox(
                checked = uiState.isAgeConfirmed,
                onCheckedChange = viewModel::onAgeChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryEmerald,
                    checkmarkColor = DarkBackground
                )
            )
            Text(
                text = stringResource(R.string.checkbox_age),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Error message
        if (uiState.errorMessageRes != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(uiState.errorMessageRes!!),
                style = MaterialTheme.typography.bodyMedium,
                color = StatusError,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In Button
        Button(
            onClick = { viewModel.signIn(onLoginSuccess) },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryEmerald,
                contentColor = DarkBackground
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = DarkBackground,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.btn_sign_in),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Website Link
        Text(
            text = stringResource(R.string.link_open_website),
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryEmerald,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://winzone.example"))
                context.startActivity(intent)
            }
        )
    }
}
