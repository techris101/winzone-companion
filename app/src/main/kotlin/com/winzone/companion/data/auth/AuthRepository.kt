package com.winzone.companion.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val session: Flow<StoredSession?>
    suspend fun signIn(email: String, password: String): Result<StoredSession>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshIfNeeded(): Result<Unit>
    suspend fun importSession(accessToken: String, refreshToken: String, userId: String, email: String): Result<StoredSession>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionStore: SessionStore
) : AuthRepository {

    override val session: Flow<StoredSession?> = sessionStore.currentSession

    override suspend fun signIn(email: String, password: String): Result<StoredSession> = runCatching {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        supabase.auth.signInWith(Email) {
            this.email = cleanEmail
            this.password = cleanPassword
        }

        val currentSession = supabase.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Supabase returned null session after sign-in")

        val stored = StoredSession(
            accessToken = currentSession.accessToken,
            refreshToken = currentSession.refreshToken,
            userId = currentSession.user?.id ?: throw IllegalStateException("Missing user ID"),
            email = currentSession.user?.email ?: cleanEmail,
            expiresAt = System.currentTimeMillis() + (currentSession.expiresIn * 1000)
        )
        sessionStore.saveSession(stored)
        Timber.i("Successfully signed in user: %s", stored.userId)
        stored
    }.onFailure {
        Timber.e(it, "Sign in failed for email: %s", email)
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Timber.w(e, "Supabase sign out call threw exception, clearing local session anyway")
        }
        sessionStore.clear()
        Timber.i("User signed out and local session cleared")
    }

    override suspend fun refreshIfNeeded(): Result<Unit> = runCatching {
        val current = sessionStore.currentSession.value ?: return@runCatching
        val now = System.currentTimeMillis()
        // Refresh if within 5 minutes of expiration
        if (current.expiresAt - now < 5 * 60 * 1000L) {
            Timber.d("Refreshing Supabase auth session...")
            supabase.auth.refreshCurrentSession()
            val newSession = supabase.auth.currentSessionOrNull()
            if (newSession != null) {
                val updated = StoredSession(
                    accessToken = newSession.accessToken,
                    refreshToken = newSession.refreshToken,
                    userId = current.userId,
                    email = current.email,
                    expiresAt = System.currentTimeMillis() + (newSession.expiresIn * 1000)
                )
                sessionStore.saveSession(updated)
                Timber.d("Session refreshed successfully")
            }
        }
    }.onFailure {
        Timber.w(it, "Failed to refresh auth session")
    }

    override suspend fun importSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String
    ): Result<StoredSession> = runCatching {
        val stored = StoredSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = email,
            expiresAt = System.currentTimeMillis() + (3600 * 1000L)
        )
        sessionStore.saveSession(stored)
        try {
            supabase.auth.importAuthToken(accessToken)
        } catch (e: Exception) {
            Timber.w(e, "importAuthToken skipped or failed, local session stored")
        }
        Timber.i("Successfully imported auth session for user: %s", userId)
        stored
    }.onFailure {
        Timber.e(it, "Failed to import auth session for user: %s", userId)
    }
}
