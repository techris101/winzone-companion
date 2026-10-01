package com.winzone.companion.ocr

import com.winzone.companion.data.state.MatchLayout
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val layout: MatchLayout,
    val confidence: Float
)

@Singleton
class LayoutClassifier @Inject constructor() {

    fun classify(
        ocrResult: OcrResult,
        profile: GameProfile,
        scoreA: Int?,
        scoreB: Int?,
        clockSeconds: Int?
    ): ClassificationResult {
        val upperText = ocrResult.fullText.uppercase()

        // 1. Full-time check
        for (kw in profile.layoutMarkers.fullTimeKeywords) {
            if (upperText.contains(kw.uppercase())) {
                return ClassificationResult(MatchLayout.FULL_TIME, 0.85f)
            }
        }

        // 2. Disconnected check
        for (kw in profile.layoutMarkers.disconnectedKeywords) {
            if (upperText.contains(kw.uppercase())) {
                return ClassificationResult(MatchLayout.OPPONENT_DISCONNECTED, 0.85f)
            }
        }

        // 3. Conceded check
        for (kw in profile.layoutMarkers.concededKeywords) {
            if (upperText.contains(kw.uppercase())) {
                return ClassificationResult(MatchLayout.OPPONENT_CONCEDED, 0.85f)
            }
        }

        // 4. In-play indicator check
        for (indicator in profile.layoutMarkers.inPlayIndicators) {
            if (upperText.contains(indicator.uppercase())) {
                return ClassificationResult(MatchLayout.IN_PLAY, 0.80f)
            }
        }

        // 5. Fallback rule: clock + scores parsed
        if (clockSeconds != null && scoreA != null && scoreB != null) {
            return ClassificationResult(MatchLayout.IN_PLAY, 0.65f)
        }

        return ClassificationResult(MatchLayout.UNKNOWN, 0.0f)
    }
}
