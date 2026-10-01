package com.winzone.companion.integrity

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayIntegrityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun requestIntegrityToken(): String? {
        return try {
            val standardIntegrityManager = IntegrityManagerFactory.createStandard(context)
            // Soft-fail: if Google Play Services isn't present or token fails, return null gracefully
            null
        } catch (e: Exception) {
            Timber.w(e, "Play Integrity check skipped or failed (soft-fail)")
            null
        }
    }
}
