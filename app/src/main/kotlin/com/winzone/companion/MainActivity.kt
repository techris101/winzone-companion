package com.winzone.companion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.winzone.companion.data.auth.SessionStore
import com.winzone.companion.data.preferences.UserPreferencesStore
import com.winzone.companion.ui.nav.AppNav
import com.winzone.companion.ui.nav.NavRoutes
import com.winzone.companion.ui.theme.DarkBackground
import com.winzone.companion.ui.theme.WinZoneTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var preferencesStore: UserPreferencesStore

    private var pendingMatchId: String? = null

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

                    val startDestination = when {
                        !isOnboardingComplete -> NavRoutes.ONBOARDING
                        currentSession == null -> NavRoutes.LOGIN
                        pendingMatchId != null -> NavRoutes.join(pendingMatchId!!)
                        else -> NavRoutes.HOME
                    }

                    AppNav(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.data ?: return
        Timber.i("Handling intent URI: %s", data.toString())

        // Match scheme: winzone://join?match_id={uuid}
        // or https://winzone.example/app/join?match_id={uuid}
        val matchId = data.getQueryParameter("match_id")
        if (!matchId.isNullOrBlank()) {
            Timber.i("Extracted deep-link match ID: %s", matchId)
            pendingMatchId = matchId
        }
    }
}
