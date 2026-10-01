package com.winzone.companion

import com.winzone.companion.data.state.MatchLayout
import com.winzone.companion.ocr.LayoutClassifier
import com.winzone.companion.ocr.OcrBlock
import com.winzone.companion.ocr.OcrResult
import com.winzone.companion.ocr.profiles.DefaultGameProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LayoutClassifierTest {

    private val classifier = LayoutClassifier()
    private val profile = DefaultGameProfile.instance

    @Test
    fun `classifies full time layout on keyword match`() {
        val ocr = OcrResult(
            fullText = "MATCH ENDED FULL TIME 2 - 1",
            blocks = listOf(
                OcrBlock("MATCH ENDED", 0, 0, 100, 50, 0.9f),
                OcrBlock("FULL TIME", 0, 50, 100, 100, 0.9f)
            )
        )
        val result = classifier.classify(ocr, profile, scoreA = 2, scoreB = 1, clockSeconds = 5400)
        assertEquals(MatchLayout.FULL_TIME, result.layout)
    }

    @Test
    fun `classifies opponent disconnected layout on keyword match`() {
        val ocr = OcrResult(
            fullText = "OPPONENT DISCONNECTED PLEASE WAIT",
            blocks = listOf(
                OcrBlock("OPPONENT DISCONNECTED", 0, 0, 100, 50, 0.9f)
            )
        )
        val result = classifier.classify(ocr, profile, scoreA = 1, scoreB = 0, clockSeconds = 2000)
        assertEquals(MatchLayout.OPPONENT_DISCONNECTED, result.layout)
    }

    @Test
    fun `classifies opponent conceded layout on keyword match`() {
        val ocr = OcrResult(
            fullText = "MATCH OVER OPPONENT CONCEDED",
            blocks = listOf(
                OcrBlock("OPPONENT CONCEDED", 0, 0, 100, 50, 0.9f)
            )
        )
        val result = classifier.classify(ocr, profile, scoreA = 1, scoreB = 0, clockSeconds = 2000)
        assertEquals(MatchLayout.OPPONENT_CONCEDED, result.layout)
    }

    @Test
    fun `classifies in play on pause indicator or parsed clock and scores`() {
        val ocr = OcrResult(
            fullText = "TEAM A 1 0 TEAM B 23:45 PAUSE",
            blocks = listOf(
                OcrBlock("PAUSE", 0, 0, 100, 50, 0.9f)
            )
        )
        val result = classifier.classify(ocr, profile, scoreA = 1, scoreB = 0, clockSeconds = 1425)
        assertEquals(MatchLayout.IN_PLAY, result.layout)
    }

    @Test
    fun `classifies unknown when no markers or scores match`() {
        val ocr = OcrResult(
            fullText = "LOADING GAME PLEASE WAIT",
            blocks = listOf(
                OcrBlock("LOADING GAME", 0, 0, 100, 50, 0.9f)
            )
        )
        val result = classifier.classify(ocr, profile, scoreA = null, scoreB = null, clockSeconds = null)
        assertEquals(MatchLayout.UNKNOWN, result.layout)
    }
}
