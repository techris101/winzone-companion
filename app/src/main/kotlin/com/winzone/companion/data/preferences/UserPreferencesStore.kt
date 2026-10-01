package com.winzone.companion.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val onboardingKey = booleanPreferencesKey("onboarding_complete_v1")
    private val tosKey = booleanPreferencesKey("tos_accepted_v1")
    private val ageKey = booleanPreferencesKey("age_confirmed_v1")
    private val verboseLoggingKey = booleanPreferencesKey("verbose_logging")

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[onboardingKey] ?: false
    }

    val isTosAccepted: Flow<Boolean> = context.dataStore.data.map {
        it[tosKey] ?: false
    }

    val isAgeConfirmed: Flow<Boolean> = context.dataStore.data.map {
        it[ageKey] ?: false
    }

    val isVerboseLoggingEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[verboseLoggingKey] ?: false
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardingKey] = complete }
    }

    suspend fun setTosAccepted(accepted: Boolean) {
        context.dataStore.edit { it[tosKey] = accepted }
    }

    suspend fun setAgeConfirmed(confirmed: Boolean) {
        context.dataStore.edit { it[ageKey] = confirmed }
    }

    suspend fun setVerboseLogging(enabled: Boolean) {
        context.dataStore.edit { it[verboseLoggingKey] = enabled }
    }
}
