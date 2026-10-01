package com.winzone.companion.agreement

import com.winzone.companion.data.match.Side
import com.winzone.companion.data.state.MatchLayout
import com.winzone.companion.util.Constants

data class LocalStateSnapshot(
    val layout: MatchLayout,
    val teamA: String?,
    val teamB: String?,
    val scoreA: Int?,
    val scoreB: Int?,
    val penA: Int?,
    val penB: Int?,
    val clockSeconds: Int?,
    val ocrConfidence: Float
)

data class RemoteStateSnapshot(
    val layout: MatchLayout,
    val teamA: String?,
    val teamB: String?,
    val scoreA: Int?,
    val scoreB: Int?,
    val penA: Int?,
    val penB: Int?,
    val clockSeconds: Int?
)

object AgreementRules {

    fun canStartMatch(
        local: LocalStateSnapshot,
        remote: RemoteStateSnapshot,
        mySide: Side
    ): Boolean {
        // Local requirements
        val localValid = local.layout == MatchLayout.IN_PLAY &&
                local.scoreA != null && local.scoreB != null &&
                !local.teamA.isNullOrBlank() && !local.teamB.isNullOrBlank() &&
                local.clockSeconds != null && local.clockSeconds <= Constants.KICKOFF_CLOCK_MAX_SECONDS &&
                local.ocrConfidence >= Constants.OCR_CONFIDENCE_KICKOFF_THRESHOLD

        if (!localValid) return false

        // Remote requirements
        val remoteValid = remote.layout == MatchLayout.IN_PLAY &&
                !remote.teamA.isNullOrBlank() && !remote.teamB.isNullOrBlank() &&
                (remote.scoreA == 0 && remote.scoreB == 0 || (remote.scoreA == local.scoreA && remote.scoreB == local.scoreB)) &&
                remote.clockSeconds != null && remote.clockSeconds <= Constants.KICKOFF_CLOCK_MAX_SECONDS

        return remoteValid
    }

    fun canFinalize(
        local: LocalStateSnapshot,
        remote: RemoteStateSnapshot,
        mySide: Side,
        kickoffConfirmed: Boolean
    ): Boolean {
        // R2: No winner before kickoff
        if (!kickoffConfirmed) return false

        // Local requirements
        val isTerminalLocal = local.layout in listOf(
            MatchLayout.FULL_TIME,
            MatchLayout.OPPONENT_DISCONNECTED,
            MatchLayout.OPPONENT_CONCEDED
        )
        if (!isTerminalLocal) return false
        if (local.scoreA == null || local.scoreB == null) return false
        if (local.ocrConfidence < Constants.OCR_CONFIDENCE_FINALIZE_THRESHOLD) return false

        // Remote requirements
        val isTerminalRemote = remote.layout in listOf(
            MatchLayout.FULL_TIME,
            MatchLayout.OPPONENT_DISCONNECTED,
            MatchLayout.OPPONENT_CONCEDED
        )
        if (!isTerminalRemote) return false

        // If both full_time, scores must match canonically
        if (local.layout == MatchLayout.FULL_TIME && remote.layout == MatchLayout.FULL_TIME) {
            if (remote.scoreA != null && remote.scoreB != null) {
                if (local.scoreA != remote.scoreA || local.scoreB != remote.scoreB) {
                    return false
                }
            }
        }

        return true
    }

    fun sidesMatch(local: LocalStateSnapshot, remote: RemoteStateSnapshot, mySide: Side): Boolean {
        if (local.teamA.isNullOrBlank() || remote.teamA.isNullOrBlank()) return true
        return true
    }
}
