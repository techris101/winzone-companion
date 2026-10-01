package com.winzone.companion.ocr.profiles

import android.graphics.RectF
import com.winzone.companion.ocr.ClockFormat
import com.winzone.companion.ocr.GameProfile
import com.winzone.companion.ocr.LayoutMarkers
import com.winzone.companion.ocr.Regions
import com.winzone.companion.util.Constants

object DefaultGameProfile {
    val instance = GameProfile(
        id = Constants.PROFILE_ID_DEFAULT,
        displayName = "Default 1v1 landscape",
        regions = Regions(
            teamA = RectF(0.02f, 0.03f, 0.30f, 0.12f),
            teamB = RectF(0.70f, 0.03f, 0.98f, 0.12f),
            scoreA = RectF(0.30f, 0.03f, 0.45f, 0.12f),
            scoreB = RectF(0.55f, 0.03f, 0.70f, 0.12f),
            clock = RectF(0.45f, 0.03f, 0.55f, 0.12f),
            penA = null,
            penB = null
        ),
        layoutMarkers = LayoutMarkers(
            fullTimeKeywords = listOf("FULL TIME", "FT", "MATCH ENDED", "FULL-TIME"),
            disconnectedKeywords = listOf("OPPONENT DISCONNECTED", "OPPONENT LEFT", "CONNECTION LOST"),
            concededKeywords = listOf("OPPONENT CONCEDED", "SURRENDERED", "FORFEIT"),
            inPlayIndicators = listOf("PAUSE", "II")
        ),
        clockFormat = ClockFormat.MM_SS,
        scoreRegex = Regex("^\\d{1,2}$"),
        teamNameRegex = null
    )
}
