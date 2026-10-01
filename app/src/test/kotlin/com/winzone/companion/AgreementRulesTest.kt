package com.winzone.companion

import com.winzone.companion.agreement.AgreementRules
import com.winzone.companion.agreement.LocalStateSnapshot
import com.winzone.companion.agreement.RemoteStateSnapshot
import com.winzone.companion.data.match.Side
import com.winzone.companion.data.state.MatchLayout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgreementRulesTest {

    @Test
    fun `canStartMatch passes when both sides satisfy in_play kickoff requirements`() {
        val local = LocalStateSnapshot(
            layout = MatchLayout.IN_PLAY,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 0,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 15,
            ocrConfidence = 0.85f
        )
        val remote = RemoteStateSnapshot(
            layout = MatchLayout.IN_PLAY,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 0,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 18
        )

        assertTrue(AgreementRules.canStartMatch(local, remote, Side.A))
    }

    @Test
    fun `canStartMatch fails when local confidence is below kickoff threshold`() {
        val local = LocalStateSnapshot(
            layout = MatchLayout.IN_PLAY,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 0,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 15,
            ocrConfidence = 0.55f // < 0.70 threshold
        )
        val remote = RemoteStateSnapshot(
            layout = MatchLayout.IN_PLAY,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 0,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 18
        )

        assertFalse(AgreementRules.canStartMatch(local, remote, Side.A))
    }

    @Test
    fun `canStartMatch fails when clock is past 120 seconds`() {
        val local = LocalStateSnapshot(
            layout = MatchLayout.IN_PLAY,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 0,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 200, // > 120s
            ocrConfidence = 0.85f
        )
        val remote = RemoteStateSnapshot(
            layout = MatchLayout.IN_PLAY,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 0,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 18
        )

        assertFalse(AgreementRules.canStartMatch(local, remote, Side.A))
    }

    @Test
    fun `canFinalize succeeds when both sides agree on full_time with matching scores after kickoff`() {
        val local = LocalStateSnapshot(
            layout = MatchLayout.FULL_TIME,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 2,
            scoreB = 1,
            penA = null,
            penB = null,
            clockSeconds = 5400,
            ocrConfidence = 0.80f
        )
        val remote = RemoteStateSnapshot(
            layout = MatchLayout.FULL_TIME,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 2,
            scoreB = 1,
            penA = null,
            penB = null,
            clockSeconds = 5400
        )

        assertTrue(AgreementRules.canFinalize(local, remote, Side.A, kickoffConfirmed = true))
    }

    @Test
    fun `canFinalize fails if kickoff was not previously confirmed`() {
        val local = LocalStateSnapshot(
            layout = MatchLayout.FULL_TIME,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 2,
            scoreB = 1,
            penA = null,
            penB = null,
            clockSeconds = 5400,
            ocrConfidence = 0.80f
        )
        val remote = RemoteStateSnapshot(
            layout = MatchLayout.FULL_TIME,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 2,
            scoreB = 1,
            penA = null,
            penB = null,
            clockSeconds = 5400
        )

        assertFalse(AgreementRules.canFinalize(local, remote, Side.A, kickoffConfirmed = false))
    }

    @Test
    fun `canFinalize fails if scores disagree at full_time`() {
        val local = LocalStateSnapshot(
            layout = MatchLayout.FULL_TIME,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 2,
            scoreB = 1,
            penA = null,
            penB = null,
            clockSeconds = 5400,
            ocrConfidence = 0.80f
        )
        val remote = RemoteStateSnapshot(
            layout = MatchLayout.FULL_TIME,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 2,
            scoreB = 2, // Mismatch
            penA = null,
            penB = null,
            clockSeconds = 5400
        )

        assertFalse(AgreementRules.canFinalize(local, remote, Side.A, kickoffConfirmed = true))
    }

    @Test
    fun `canFinalize succeeds on opponent disconnected or conceded`() {
        val local = LocalStateSnapshot(
            layout = MatchLayout.FULL_TIME,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 1,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 3000,
            ocrConfidence = 0.80f
        )
        val remote = RemoteStateSnapshot(
            layout = MatchLayout.OPPONENT_DISCONNECTED,
            teamA = "Red Tigers",
            teamB = "Blue Sharks",
            scoreA = 1,
            scoreB = 0,
            penA = null,
            penB = null,
            clockSeconds = 3000
        )

        assertTrue(AgreementRules.canFinalize(local, remote, Side.A, kickoffConfirmed = true))
    }
}
