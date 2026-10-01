package com.winzone.companion.data.state

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MatchLayout(val wireName: String) {
    IN_PLAY("in_play"),
    FULL_TIME("full_time"),
    OPPONENT_DISCONNECTED("opponent_disconnected"),
    OPPONENT_CONCEDED("opponent_conceded"),
    UNKNOWN("unknown")
}

@Serializable
data class ScreenStatePayload(
    @SerialName("p_match_id") val pMatchId: String,
    @SerialName("p_layout") val pLayout: String,
    @SerialName("p_team_a") val pTeamA: String? = null,
    @SerialName("p_team_b") val pTeamB: String? = null,
    @SerialName("p_score_a") val pScoreA: Int? = null,
    @SerialName("p_score_b") val pScoreB: Int? = null,
    @SerialName("p_pen_a") val pPenA: Int? = null,
    @SerialName("p_pen_b") val pPenB: Int? = null,
    @SerialName("p_clock_seconds") val pClockSeconds: Int? = null,
    @SerialName("p_raw_json") val pRawJson: RawJson
)
