package com.winzone.companion.capture

import com.winzone.companion.agreement.AgreementGate
import com.winzone.companion.data.match.Side
import com.winzone.companion.data.remote.RetryPolicy
import com.winzone.companion.data.remote.SupabaseRpcClient
import com.winzone.companion.data.state.OfflineQueueDao
import com.winzone.companion.data.state.PendingStateEntity
import com.winzone.companion.data.state.ScreenStatePayload
import com.winzone.companion.integrity.DeviceIntegrityManager
import com.winzone.companion.util.Constants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.math.abs

class StateReporter(
    private val rpc: SupabaseRpcClient,
    private val agreementGate: AgreementGate,
    private val offlineQueueDao: OfflineQueueDao,
    private val integrityManager: DeviceIntegrityManager,
    private val matchId: String,
    private val mySide: Side
) {
    private val mutex = Mutex()
    private var lastSent: FrameAnalysis? = null
    private var lastSubmitTimeMs: Long = 0L

    suspend fun submit(analysis: FrameAnalysis, manualTeamA: String? = null, manualTeamB: String? = null) = mutex.withLock {
        // Enforce rate limiter (min 1.5s between network attempts)
        val now = System.currentTimeMillis()
        if (now - lastSubmitTimeMs < Constants.MIN_SUBMIT_INTERVAL_MS) {
            return@withLock
        }

        // Apply manual team name overrides if provided from fallback dialog
        val effectiveAnalysis = if (!manualTeamA.isNullOrBlank() || !manualTeamB.isNullOrBlank()) {
            analysis.copy(
                teamA = manualTeamA ?: analysis.teamA,
                teamB = manualTeamB ?: analysis.teamB
            )
        } else {
            analysis
        }

        // Duplicate suppression
        if (isDuplicate(effectiveAnalysis, lastSent)) {
            return@withLock
        }

        val integrity = integrityManager.checkIntegrity().name
        val fingerprint = integrityManager.getDeviceFingerprint()
        val payload = effectiveAnalysis.toPayload(matchId, mySide, integrity, fingerprint)

        lastSubmitTimeMs = now
        val result = RetryPolicy.retryWithBackoff {
            rpc.submitScreenState(payload)
        }

        if (result.isSuccess) {
            lastSent = effectiveAnalysis
            agreementGate.onLocalState(payload, isSubmitSuccess = true)
            flushOfflineQueue()
        } else {
            Timber.w("Submission failed, queuing frame in local database")
            queueOffline(payload)
            agreementGate.onLocalState(payload, isSubmitSuccess = false)
        }
    }

    private fun isDuplicate(current: FrameAnalysis, previous: FrameAnalysis?): Boolean {
        if (previous == null) return false
        if (current.layout != previous.layout) return false
        if (current.scoreA != previous.scoreA || current.scoreB != previous.scoreB) return false

        val currClock = current.clockSeconds
        val prevClock = previous.clockSeconds
        if (currClock != null && prevClock != null) {
            if (abs(currClock - prevClock) > 1) return false
        } else if (currClock != prevClock) {
            return false
        }

        return true
    }

    private suspend fun queueOffline(payload: ScreenStatePayload) {
        try {
            val jsonString = Json.encodeToString(payload)
            offlineQueueDao.insert(
                PendingStateEntity(
                    matchId = matchId,
                    payloadJson = jsonString,
                    createdAtMs = System.currentTimeMillis()
                )
            )
            offlineQueueDao.trimQueue(Constants.MAX_QUEUED_FRAMES)
        } catch (e: Exception) {
            Timber.e(e, "Error saving pending state to Room database")
        }
    }

    private suspend fun flushOfflineQueue() {
        try {
            val queued = offlineQueueDao.getOldest(10)
            for (entity in queued) {
                val payload = Json.decodeFromString<ScreenStatePayload>(entity.payloadJson)
                val flushResult = rpc.submitScreenState(payload)
                if (flushResult.isSuccess) {
                    offlineQueueDao.delete(entity.id)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error flushing offline state queue")
        }
    }
}
