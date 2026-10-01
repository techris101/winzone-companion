package com.winzone.companion.ocr

import android.graphics.RectF

data class GameProfile(
    val id: String,
    val displayName: String,
    val regions: Regions,
    val layoutMarkers: LayoutMarkers,
    val clockFormat: ClockFormat,
    val scoreRegex: Regex = Regex("^\\d{1,2}$"),
    val teamNameRegex: Regex? = null
)

data class Regions(
    val teamA: RectF,
    val teamB: RectF,
    val scoreA: RectF,
    val scoreB: RectF,
    val penA: RectF? = null,
    val penB: RectF? = null,
    val clock: RectF? = null
)

data class LayoutMarkers(
    val fullTimeKeywords: List<String>,
    val disconnectedKeywords: List<String>,
    val concededKeywords: List<String>,
    val inPlayIndicators: List<String>
)

enum class ClockFormat {
    MM_SS,
    MM_SS_APOSTROPHE,
    SECONDS_ONLY
}
