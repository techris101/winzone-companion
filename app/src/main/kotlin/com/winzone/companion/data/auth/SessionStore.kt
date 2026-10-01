package com.winzone.companion.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAt: Long
)

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "winzone_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize EncryptedSharedPreferences, falling back to standard private prefs")
            context.getSharedPreferences("winzone_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _currentSession = MutableStateFlow<StoredSession?>(null)
    val currentSession: StateFlow<StoredSession?> = _currentSession.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        if (accessToken != null && refreshToken != null && userId != null) {
            _currentSession.value = StoredSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId,
                email = email.orEmpty(),
                expiresAt = expiresAt
            )
        } else {
            _currentSession.value = null
        }
    }

    fun saveSession(session: StoredSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putLong(KEY_EXPIRES_AT, session.expiresAt)
            .apply()
        _currentSession.value = session
    }

    fun clear() {
        prefs.edit().clear().apply()
        _currentSession.value = null
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
