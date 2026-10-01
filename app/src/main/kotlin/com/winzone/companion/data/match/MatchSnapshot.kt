package com.winzone.companion.data.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Side(val wireName: String) {
    A("a"),
    B("b")
}

@Serializable
data class MatchSnapshot(
    @SerialName("match_id") val matchId: String,
    @SerialName("status") val status: String = "matched",
    @SerialName("player_one_id") val playerOneId: String = "",
    @SerialName("player_two_id") val playerTwoId: String = "",
    @SerialName("my_side") val mySide: String = "a",
    @SerialName("team_a_label") val teamALabel: String? = null,
    @SerialName("team_b_label") val teamBLabel: String? = null,
    @SerialName("stake") val stake: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("kickoff_at") val kickoffAt: String? = null
) {
    val side: Side
        get() = if (mySide.equals("b", ignoreCase = true)) Side.B else Side.A

    val shortId: String
        get() = if (matchId.length >= 8) matchId.take(8) else matchId
}

@Serializable
data class MatchRow(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
    @SerialName("player_one_id") val playerOneId: String,
    @SerialName("player_two_id") val playerTwoId: String,
    @SerialName("stake") val stake: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("kickoff_at") val kickoffAt: String? = null
) {
    fun toSnapshot(currentUserId: String): MatchSnapshot {
        val assignedSide = if (currentUserId == playerTwoId) "b" else "a"
        return MatchSnapshot(
            matchId = id,
            status = status,
            playerOneId = playerOneId,
            playerTwoId = playerTwoId,
            mySide = assignedSide,
            stake = stake,
            createdAt = createdAt,
            kickoffAt = kickoffAt
        )
    }
}
