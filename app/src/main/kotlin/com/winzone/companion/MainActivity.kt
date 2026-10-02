package com.winzone.companion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.winzone.companion.data.auth.AuthRepository
import com.winzone.companion.data.auth.SessionStore
import com.winzone.companion.data.auth.StoredSession
import com.winzone.companion.data.preferences.UserPreferencesStore
import com.winzone.companion.ui.nav.AppNav
import com.winzone.companion.ui.nav.NavRoutes
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.WinZoneTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var preferencesStore: UserPreferencesStore

    private var pendingMatchId: String? = null
    private val navEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_WinZone)

        handleIntent(intent)

        setContent {
            WinZoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()
                    val isOnboardingComplete by preferencesStore.isOnboardingComplete.collectAsState(initial = false)
                    val currentSession by sessionStore.currentSession.collectAsState()

                    LaunchedEffect(navController) {
                        navEvents.collect { route ->
                            Timber.i("Deep link nav event triggered: %s", route)
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    }

                    val startDestination = when {
                        pendingMatchId != null && currentSession != null -> NavRoutes.join(pendingMatchId!!)
                        !isOnboardingComplete && currentSession == null -> NavRoutes.ONBOARDING
                        currentSession == null -> NavRoutes.LOGIN
                        pendingMatchId != null -> NavRoutes.join(pendingMatchId!!)
                        else -> NavRoutes.HOME
                    }

                    AppNav(
                        navController = navController,
                        startDestination = startDestination,
                        pendingMatchId = pendingMatchId,
                        onMatchConsumed = { pendingMatchId = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.data ?: return
        Timber.i("Handling intent URI: %s", data.toString())

        // Match scheme: winzone://join?match_id={uuid}&access_token={token}&refresh_token={refresh}&user_id={uid}&email={email}
        // or https://winzone.example/app/join?...
        val matchId = data.getQueryParameter("match_id")
        val accessToken = data.getQueryParameter("access_token")
        val refreshToken = data.getQueryParameter("refresh_token")
        val userId = data.getQueryParameter("user_id")
        val email = data.getQueryParameter("email")

        if (!accessToken.isNullOrBlank() && !userId.isNullOrBlank()) {
            Timber.i("Extracted deep-link session for user: %s", userId)
            val stored = StoredSession(
                accessToken = accessToken,
                refreshToken = refreshToken.orEmpty(),
                userId = userId,
                email = email.orEmpty(),
                expiresAt = System.currentTimeMillis() + (3600 * 1000L)
            )
            sessionStore.saveSession(stored)
            lifecycleScope.launch {
                authRepository.importSession(accessToken, refreshToken.orEmpty(), userId, email.orEmpty())
                preferencesStore.setOnboardingComplete(true)
                preferencesStore.setTosAccepted(true)
                preferencesStore.setAgeConfirmed(true)
            }
        }

        if (!matchId.isNullOrBlank()) {
            Timber.i("Extracted deep-link match ID: %s", matchId)
            pendingMatchId = matchId
            navEvents.tryEmit(NavRoutes.join(matchId))
        }
    }
}
