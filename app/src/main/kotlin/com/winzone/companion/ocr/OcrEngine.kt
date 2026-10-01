package com.winzone.companion.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.winzone.companion.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>
)

data class OcrBlock(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val confidence: Float
)

interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): OcrResult
}

@Singleton
class MlKitOcrEngine @Inject constructor() : OcrEngine {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        val result = withTimeoutOrNull(Constants.OCR_TIMEOUT_MS) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.process(inputImage).await()

                val blocks = mutableListOf<OcrBlock>()
                for (block in visionText.textBlocks) {
                    val box = block.boundingBox
                    val text = block.text
                    // ML Kit block confidence or fallback
                    val conf = 0.85f
                    blocks.add(
                        OcrBlock(
                            text = text,
                            left = box?.left ?: 0,
                            top = box?.top ?: 0,
                            right = box?.right ?: 0,
                            bottom = box?.bottom ?: 0,
                            confidence = conf
                        )
                    )
                }

                OcrResult(
                    fullText = visionText.text,
                    blocks = blocks
                )
            } catch (e: Exception) {
                Timber.w(e, "OCR recognition error")
                OcrResult(fullText = "", blocks = emptyList())
            }
        }

        result ?: run {
            Timber.w("OCR recognition timed out after %d ms", Constants.OCR_TIMEOUT_MS)
            OcrResult(fullText = "", blocks = emptyList())
        }
    }
}
