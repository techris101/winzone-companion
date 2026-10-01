package com.winzone.companion.ocr

import android.graphics.Rect
import android.graphics.RectF
import com.winzone.companion.util.BitmapUtils
import com.winzone.companion.util.Time
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedFields(
    val teamA: String?,
    val teamB: String?,
    val scoreA: Int?,
    val scoreB: Int?,
    val penA: Int?,
    val penB: Int?,
    val clockSeconds: Int?,
    val ocrConfidence: Float
)

@Singleton
class FieldExtractor @Inject constructor() {

    fun extract(
        ocrResult: OcrResult,
        profile: GameProfile,
        frameWidth: Int,
        frameHeight: Int
    ): ExtractedFields {
        if (frameWidth <= 0 || frameHeight <= 0 || ocrResult.blocks.isEmpty()) {
            return ExtractedFields(
                teamA = null,
                teamB = null,
                scoreA = null,
                scoreB = null,
                penA = null,
                penB = null,
                clockSeconds = null,
                ocrConfidence = 0.0f
            )
        }

        // 1. Map blocks into regions
        val teamABlocks = filterBlocksInRegion(ocrResult.blocks, profile.regions.teamA, frameWidth, frameHeight)
        val teamBBlocks = filterBlocksInRegion(ocrResult.blocks, profile.regions.teamB, frameWidth, frameHeight)
        val scoreABlocks = filterBlocksInRegion(ocrResult.blocks, profile.regions.scoreA, frameWidth, frameHeight)
        val scoreBBlocks = filterBlocksInRegion(ocrResult.blocks, profile.regions.scoreB, frameWidth, frameHeight)
        val clockBlocks = profile.regions.clock?.let {
            filterBlocksInRegion(ocrResult.blocks, it, frameWidth, frameHeight)
        } ?: emptyList()

        val penABlocks = profile.regions.penA?.let {
            filterBlocksInRegion(ocrResult.blocks, it, frameWidth, frameHeight)
        } ?: emptyList()
        val penBBlocks = profile.regions.penB?.let {
            filterBlocksInRegion(ocrResult.blocks, it, frameWidth, frameHeight)
        } ?: emptyList()

        // 2. Parse fields
        val teamA = parseTeamName(teamABlocks, profile.teamNameRegex)
        val teamB = parseTeamName(teamBBlocks, profile.teamNameRegex)
        val scoreA = parseScore(scoreABlocks, profile.scoreRegex)
        val scoreB = parseScore(scoreBBlocks, profile.scoreRegex)
        val clockSeconds = parseClock(clockBlocks)
        val penA = parseScore(penABlocks, profile.scoreRegex)
        val penB = parseScore(penBBlocks, profile.scoreRegex)

        // Calculate confidence
        val confidences = mutableListOf<Float>()
        if (teamA != null) confidences.add(averageConfidence(teamABlocks))
        if (teamB != null) confidences.add(averageConfidence(teamBBlocks))
        if (scoreA != null) confidences.add(averageConfidence(scoreABlocks))
        if (scoreB != null) confidences.add(averageConfidence(scoreBBlocks))
        if (clockSeconds != null) confidences.add(averageConfidence(clockBlocks))

        val ocrConf = if (confidences.isNotEmpty()) confidences.minOrNull() ?: 0f else 0f

        return ExtractedFields(
            teamA = teamA,
            teamB = teamB,
            scoreA = scoreA,
            scoreB = scoreB,
            penA = penA,
            penB = penB,
            clockSeconds = clockSeconds,
            ocrConfidence = ocrConf
        )
    }

    private fun filterBlocksInRegion(
        blocks: List<OcrBlock>,
        region: RectF,
        frameWidth: Int,
        frameHeight: Int
    ): List<OcrBlock> {
        return blocks.filter { block ->
            val rect = Rect(block.left, block.top, block.right, block.bottom)
            BitmapUtils.isRectInsideRegion(rect, region, frameWidth, frameHeight)
        }
    }

    private fun parseScore(blocks: List<OcrBlock>, regex: Regex): Int? {
        for (block in blocks) {
            val text = block.text.trim()
            if (regex.matches(text)) {
                return text.toIntOrNull()
            }
            // If line contains multiple tokens, search words
            val words = text.split(Regex("\\s+"))
            for (word in words) {
                if (regex.matches(word)) {
                    return word.toIntOrNull()
                }
            }
        }
        return null
    }

    private fun parseClock(blocks: List<OcrBlock>): Int? {
        for (block in blocks) {
            val parsed = Time.parseClockString(block.text)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun parseTeamName(blocks: List<OcrBlock>, customRegex: Regex?): String? {
        if (blocks.isEmpty()) return null
        val full = blocks.joinToString(" ") { it.text.trim() }
            .replace(Regex("[\\r\\n\\t]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (full.isEmpty()) return null

        if (customRegex != null) {
            val match = customRegex.find(full)
            if (match != null) return match.value.take(32)
        }

        // Longest alphabetic run or cleaned name up to 32 chars
        val cleaned = full.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '-' || it == '.' }.take(32)
        return cleaned.ifEmpty { null }
    }

    private fun averageConfidence(blocks: List<OcrBlock>): Float {
        if (blocks.isEmpty()) return 0f
        return blocks.map { it.confidence }.average().toFloat()
    }
}
