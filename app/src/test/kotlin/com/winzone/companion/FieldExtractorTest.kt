package com.winzone.companion

import com.winzone.companion.ocr.FieldExtractor
import com.winzone.companion.ocr.OcrBlock
import com.winzone.companion.ocr.OcrResult
import com.winzone.companion.ocr.profiles.DefaultGameProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class FieldExtractorTest {

    private val extractor = FieldExtractor()
    private val profile = DefaultGameProfile.instance

    @Test
    fun `extracts team names scores and clock from mapped bounding box regions`() {
        val width = 1000
        val height = 500

        // Regions for default-v1:
        // teamA: [0.02, 0.03, 0.30, 0.12] -> left=20..300, top=15..60
        // teamB: [0.70, 0.03, 0.98, 0.12] -> left=700..980, top=15..60
        // scoreA: [0.30, 0.03, 0.45, 0.12] -> left=300..450, top=15..60
        // scoreB: [0.55, 0.03, 0.70, 0.12] -> left=550..700, top=15..60
        // clock: [0.45, 0.03, 0.55, 0.12] -> left=450..550, top=15..60

        val blocks = listOf(
            OcrBlock("Lions", left = 30, top = 20, right = 150, bottom = 50, confidence = 0.92f),
            OcrBlock("Tigers", left = 750, top = 20, right = 900, bottom = 50, confidence = 0.91f),
            OcrBlock("2", left = 350, top = 20, right = 400, bottom = 50, confidence = 0.95f),
            OcrBlock("1", left = 600, top = 20, right = 650, bottom = 50, confidence = 0.95f),
            OcrBlock("34:12", left = 470, top = 20, right = 530, bottom = 50, confidence = 0.88f)
        )

        val ocrResult = OcrResult(
            fullText = "Lions Tigers 2 1 34:12",
            blocks = blocks
        )

        val fields = extractor.extract(ocrResult, profile, width, height)

        assertEquals("Lions", fields.teamA)
        assertEquals("Tigers", fields.teamB)
        assertEquals(2, fields.scoreA)
        assertEquals(1, fields.scoreB)
        assertEquals(2052, fields.clockSeconds) // 34*60 + 12 = 2052
        assertNotNull(fields.ocrConfidence)
    }
}
