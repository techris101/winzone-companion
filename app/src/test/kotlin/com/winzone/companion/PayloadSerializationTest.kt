package com.winzone.companion

import com.winzone.companion.data.state.MatchLayout
import com.winzone.companion.data.state.RawJson
import com.winzone.companion.data.state.RawOcrBlock
import com.winzone.companion.data.state.ScreenStatePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PayloadSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `ScreenStatePayload serializes and deserializes accurately`() {
        val payload = ScreenStatePayload(
            pMatchId = "d3b07384-d113-4043-9fd9-123456789abc",
            pLayout = MatchLayout.IN_PLAY.wireName,
            pTeamA = "Alpha",
            pTeamB = "Beta",
            pScoreA = 3,
            pScoreB = 2,
            pPenA = null,
            pPenB = null,
            pClockSeconds = 1200,
            pRawJson = RawJson(
                ocrText = "Alpha Beta 3 2 20:00",
                ocrBlocks = listOf(
                    RawOcrBlock(text = "Alpha", left = 10, top = 10, right = 100, bottom = 40, conf = 0.95f)
                ),
                layoutConfidence = 0.85f,
                ocrConfidence = 0.92f,
                frameTsMs = 1700000000000L,
                frameWidth = 1280,
                frameHeight = 720,
                profileId = "default-v1",
                appVersion = "1.0.0",
                integrityLevel = "CLEAN",
                deviceFingerprint = "abcdef0123456789"
            )
        )

        val encoded = json.encodeToString(payload)
        assertTrue(encoded.contains("\"p_match_id\":\"d3b07384-d113-4043-9fd9-123456789abc\""))
        assertTrue(encoded.contains("\"p_layout\":\"in_play\""))
        assertTrue(encoded.contains("\"device_fingerprint\":\"abcdef0123456789\""))

        val decoded = json.decodeFromString<ScreenStatePayload>(encoded)
        assertEquals(payload.pMatchId, decoded.pMatchId)
        assertEquals(payload.pLayout, decoded.pLayout)
        assertEquals(payload.pScoreA, decoded.pScoreA)
        assertEquals(payload.pScoreB, decoded.pScoreB)
        assertEquals(payload.pRawJson.deviceFingerprint, decoded.pRawJson.deviceFingerprint)
    }
}
