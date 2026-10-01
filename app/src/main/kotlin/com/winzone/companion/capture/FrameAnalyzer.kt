package com.winzone.companion.capture

import android.graphics.Bitmap
import com.winzone.companion.data.match.Side
import com.winzone.companion.data.state.MatchLayout
import com.winzone.companion.data.state.RawJson
import com.winzone.companion.data.state.RawOcrBlock
import com.winzone.companion.data.state.ScreenStatePayload
import com.winzone.companion.ocr.FieldExtractor
import com.winzone.companion.ocr.GameProfile
import com.winzone.companion.ocr.LayoutClassifier
import com.winzone.companion.ocr.OcrBlock
import com.winzone.companion.ocr.OcrEngine
import com.winzone.companion.util.BitmapUtils
import com.winzone.companion.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

data class FrameAnalysis(
    val layout: MatchLayout,
    val teamA: String?,
    val teamB: String?,
    val scoreA: Int?,
    val scoreB: Int?,
    val penA: Int?,
    val penB: Int?,
    val clockSeconds: Int?,
    val ocrConfidence: Float,
    val layoutConfidence: Float,
    val capturedAtMs: Long,
    val rawText: String,
    val rawBlocks: List<OcrBlock>,
    val frameWidth: Int,
    val frameHeight: Int
) {
    fun toPayload(
        matchId: String,
        side: Side,
        integrityLevel: String,
        deviceFingerprint: String
    ): ScreenStatePayload {
        val ocrBlocks = rawBlocks.take(Constants.MAX_RAW_JSON_BLOCKS).map {
            RawOcrBlock(
                text = it.text,
                left = it.left,
                top = it.top,
                right = it.right,
                bottom = it.bottom,
                conf = it.confidence
            )
        }

        return ScreenStatePayload(
            pMatchId = matchId,
            pLayout = layout.wireName,
            pTeamA = teamA,
            pTeamB = teamB,
            pScoreA = scoreA,
            pScoreB = scoreB,
            pPenA = penA,
            pPenB = penB,
            pClockSeconds = clockSeconds,
            pRawJson = RawJson(
                ocrText = rawText,
                ocrBlocks = ocrBlocks,
                layoutConfidence = layoutConfidence,
                ocrConfidence = ocrConfidence,
                frameTsMs = capturedAtMs,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                profileId = Constants.PROFILE_ID_DEFAULT,
                appVersion = Constants.CLIENT_VERSION,
                integrityLevel = integrityLevel,
                deviceFingerprint = deviceFingerprint
            )
        )
    }
}

@Singleton
class FrameAnalyzer @Inject constructor(
    private val ocrEngine: OcrEngine,
    private val layoutClassifier: LayoutClassifier,
    private val fieldExtractor: FieldExtractor
) {

    suspend fun analyze(bitmap: Bitmap, profile: GameProfile): FrameAnalysis {
        val downscaled = BitmapUtils.downscaleIfNeeded(bitmap, 1280)
        val width = downscaled.width
        val height = downscaled.height

        val ocrResult = ocrEngine.recognize(downscaled)
        val fields = fieldExtractor.extract(ocrResult, profile, width, height)
        val classification = layoutClassifier.classify(
            ocrResult = ocrResult,
            profile = profile,
            scoreA = fields.scoreA,
            scoreB = fields.scoreB,
            clockSeconds = fields.clockSeconds
        )

        if (downscaled != bitmap) {
            downscaled.recycle()
        }

        return FrameAnalysis(
            layout = classification.layout,
            teamA = fields.teamA,
            teamB = fields.teamB,
            scoreA = fields.scoreA,
            scoreB = fields.scoreB,
            penA = fields.penA,
            penB = fields.penB,
            clockSeconds = fields.clockSeconds,
            ocrConfidence = fields.ocrConfidence,
            layoutConfidence = classification.confidence,
            capturedAtMs = System.currentTimeMillis(),
            rawText = ocrResult.fullText,
            rawBlocks = ocrResult.blocks,
            frameWidth = width,
            frameHeight = height
        )
    }
}
