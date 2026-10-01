package com.winzone.companion.ocr

data class RectRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

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
    val teamA: RectRegion,
    val teamB: RectRegion,
    val scoreA: RectRegion,
    val scoreB: RectRegion,
    val penA: RectRegion? = null,
    val penB: RectRegion? = null,
    val clock: RectRegion? = null
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
