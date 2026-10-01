package com.winzone.companion.capture

import com.winzone.companion.data.state.MatchLayout
import com.winzone.companion.ocr.GameProfile
import com.winzone.companion.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class FrameSampler(
    private val scope: CoroutineScope,
    private val session: MediaProjectionSession,
    private val analyzer: FrameAnalyzer,
    private val reporter: StateReporter,
    private val controller: CaptureController,
    private val profile: GameProfile
) {
    private val inFlight = AtomicBoolean(false)
    private var samplerJob: Job? = null

    fun start() {
        if (samplerJob != null) return
        samplerJob = scope.launch {
            Timber.i("FrameSampler started with %d ms interval", Constants.SUBMIT_INTERVAL_MS)
            while (isActive) {
                sample()
                delay(Constants.SUBMIT_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        samplerJob?.cancel()
        samplerJob = null
        inFlight.set(false)
        Timber.i("FrameSampler stopped")
    }

    private suspend fun sample() {
        if (controller.isSamplerPaused.value) {
            return
        }

        if (!inFlight.compareAndSet(false, true)) {
            Timber.d("Previous frame sample still in flight, skipping tick")
            return
        }

        try {
            val bitmap = session.acquireLatestBitmap()
            if (bitmap == null) {
                return
            }

            val analysis = analyzer.analyze(bitmap, profile)
            bitmap.recycle()

            controller.updateAnalysis(analysis)

            // §10.5: Low confidence team name fallback trigger
            val manualA = controller.manualTeamA.value
            val manualB = controller.manualTeamB.value
            if (manualA.isNullOrBlank() && manualB.isNullOrBlank()) {
                if (analysis.ocrConfidence < Constants.OCR_CONFIDENCE_FALLBACK_THRESHOLD &&
                    analysis.layout == MatchLayout.IN_PLAY &&
                    (analysis.teamA.isNullOrBlank() || analysis.teamB.isNullOrBlank())
                ) {
                    Timber.w("Low OCR confidence at kickoff, triggering team name fallback dialog")
                    controller.showFallbackDialog()
                }
            }

            reporter.submit(
                analysis = analysis,
                manualTeamA = controller.manualTeamA.value,
                manualTeamB = controller.manualTeamB.value
            )
        } catch (e: Exception) {
            Timber.e(e, "Error during frame sampling cycle")
        } finally {
            inFlight.set(false)
        }
    }
}
