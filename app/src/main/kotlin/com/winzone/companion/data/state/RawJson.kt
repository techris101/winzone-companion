package com.winzone.companion.data.state

import com.winzone.companion.util.Constants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawJson(
    @SerialName("ocr_text") val ocrText: String = "",
    @SerialName("ocr_blocks") val ocrBlocks: List<RawOcrBlock> = emptyList(),
    @SerialName("layout_confidence") val layoutConfidence: Float = 0f,
    @SerialName("ocr_confidence") val ocrConfidence: Float = 0f,
    @SerialName("frame_ts_ms") val frameTsMs: Long = 0L,
    @SerialName("frame_width") val frameWidth: Int = 0,
    @SerialName("frame_height") val frameHeight: Int = 0,
    @SerialName("profile_id") val profileId: String = Constants.PROFILE_ID_DEFAULT,
    @SerialName("app_version") val appVersion: String = Constants.CLIENT_VERSION,
    @SerialName("integrity_level") val integrityLevel: String = "CLEAN",
    @SerialName("device_fingerprint") val deviceFingerprint: String = ""
)

@Serializable
data class RawOcrBlock(
    @SerialName("text") val text: String,
    @SerialName("left") val left: Int = 0,
    @SerialName("top") val top: Int = 0,
    @SerialName("right") val right: Int = 0,
    @SerialName("bottom") val bottom: Int = 0,
    @SerialName("conf") val conf: Float = 0f
)
