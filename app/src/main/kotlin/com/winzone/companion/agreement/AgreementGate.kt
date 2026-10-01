package com.winzone.companion.agreement

import com.winzone.companion.data.match.Side
import com.winzone.companion.data.remote.SupabaseRpcClient
import com.winzone.companion.data.state.MatchLayout
import com.winzone.companion.data.state.ScreenStatePayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

interface AgreementGate {
    val isKickoffTriggered: StateFlow<Boolean>
    val isFinalizeTriggered: StateFlow<Boolean>
    suspend fun onLocalState(payload: ScreenStatePayload, isSubmitSuccess: Boolean)
    suspend fun onRemoteState(remote: RemoteStateSnapshot)
    fun initMatch(matchId: String, side: Side)
    fun reset()
}

@Singleton
class LocalAgreementGate @Inject constructor(
    private val rpcClient: SupabaseRpcClient
) : AgreementGate {

    private var currentMatchId: String = ""
    private var mySide: Side = Side.A

    private val kickoffFired = AtomicBoolean(false)
    private val finalizeFired = AtomicBoolean(false)

    private val _isKickoffTriggered = MutableStateFlow(false)
    override val isKickoffTriggered: StateFlow<Boolean> = _isKickoffTriggered.asStateFlow()

    private val _isFinalizeTriggered = MutableStateFlow(false)
    override val isFinalizeTriggered: StateFlow<Boolean> = _isFinalizeTriggered.asStateFlow()

    private var latestLocal: LocalStateSnapshot? = null
    private var latestRemote: RemoteStateSnapshot? = null

    override fun initMatch(matchId: String, side: Side) {
        currentMatchId = matchId
        mySide = side
        reset()
    }

    override fun reset() {
        kickoffFired.set(false)
        finalizeFired.set(false)
        _isKickoffTriggered.value = false
        _isFinalizeTriggered.value = false
        latestLocal = null
        latestRemote = null
    }

    override suspend fun onLocalState(payload: ScreenStatePayload, isSubmitSuccess: Boolean) {
        val layout = try {
            MatchLayout.values().firstOrNull { it.wireName == payload.pLayout } ?: MatchLayout.UNKNOWN
        } catch (_: Exception) {
            MatchLayout.UNKNOWN
        }

        latestLocal = LocalStateSnapshot(
            layout = layout,
            teamA = payload.pTeamA,
            teamB = payload.pTeamB,
            scoreA = payload.pScoreA,
            scoreB = payload.pScoreB,
            penA = payload.pPenA,
            penB = payload.pPenB,
            clockSeconds = payload.pClockSeconds,
            ocrConfidence = payload.pRawJson.ocrConfidence
        )

        evaluateAgreement()
    }

    override suspend fun onRemoteState(remote: RemoteStateSnapshot) {
        latestRemote = remote
        evaluateAgreement()
    }

    private suspend fun evaluateAgreement() {
        val local = latestLocal ?: return
        val remote = latestRemote ?: return
        if (currentMatchId.isEmpty()) return

        // Check Kickoff
        if (!kickoffFired.get()) {
            if (AgreementRules.canStartMatch(local, remote, mySide)) {
                if (kickoffFired.compareAndSet(false, true)) {
                    Timber.i("Agreement reached on kickoff! Calling start_match(%s)", currentMatchId)
                    val result = rpcClient.startMatch(currentMatchId)
                    if (result.isSuccess) {
                        _isKickoffTriggered.value = true
                    } else {
                        kickoffFired.set(false) // Allow retry
                    }
                }
            }
        }

        // Check Finalize
        if (kickoffFired.get() && !finalizeFired.get()) {
            if (AgreementRules.canFinalize(local, remote, mySide, kickoffFired.get())) {
                if (finalizeFired.compareAndSet(false, true)) {
                    Timber.i("Agreement reached on full-time! Calling app_finalize_result(%s)", currentMatchId)
                    val result = rpcClient.finalizeResult(currentMatchId)
                    if (result.isSuccess) {
                        _isFinalizeTriggered.value = true
                    } else {
                        finalizeFired.set(false) // Allow retry
                    }
                }
            }
        }
    }
}
