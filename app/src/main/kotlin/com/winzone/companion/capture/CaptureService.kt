package com.winzone.companion.capture

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.winzone.companion.agreement.AgreementGate
import com.winzone.companion.data.auth.AuthRepository
import com.winzone.companion.data.match.Side
import com.winzone.companion.data.remote.SupabaseRpcClient
import com.winzone.companion.data.state.OfflineQueueDao
import com.winzone.companion.integrity.DeviceIntegrityManager
import com.winzone.companion.ocr.profiles.DefaultGameProfile
import com.winzone.companion.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CaptureService : Service() {

    @Inject lateinit var rpcClient: SupabaseRpcClient
    @Inject lateinit var agreementGate: AgreementGate
    @Inject lateinit var offlineQueueDao: OfflineQueueDao
    @Inject lateinit var integrityManager: DeviceIntegrityManager
    @Inject lateinit var frameAnalyzer: FrameAnalyzer
    @Inject lateinit var captureController: CaptureController
    @Inject lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var projectionSession: MediaProjectionSession? = null
    private var frameSampler: FrameSampler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == Constants.ACTION_STOP_CAPTURE) {
            Timber.i("Received stop capture action")
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(Constants.EXTRA_RESULT_CODE, 0)
        val resultData = intent.getParcelableExtra<Intent>(Constants.EXTRA_RESULT_DATA)
        val matchId = intent.getStringExtra(Constants.EXTRA_MATCH_ID).orEmpty()
        val sideWire = intent.getStringExtra(Constants.EXTRA_MY_SIDE) ?: "a"
        val mySide = if (sideWire.equals("b", ignoreCase = true)) Side.B else Side.A

        if (resultCode == 0 || resultData == null || matchId.isEmpty()) {
            Timber.e("Missing required extras for CaptureService")
            stopSelf()
            return START_NOT_STICKY
        }

        startCaptureForeground(matchId)
        acquireWakeLock()

        agreementGate.initMatch(matchId, mySide)

        projectionSession = MediaProjectionSession(this) {
            Timber.w("MediaProjection was revoked, stopping CaptureService")
            stopSelf()
        }
        projectionSession?.start(resultCode, resultData)

        val reporter = StateReporter(
            rpc = rpcClient,
            agreementGate = agreementGate,
            offlineQueueDao = offlineQueueDao,
            integrityManager = integrityManager,
            matchId = matchId,
            mySide = mySide
        )

        frameSampler = FrameSampler(
            scope = serviceScope,
            session = projectionSession!!,
            analyzer = frameAnalyzer,
            reporter = reporter,
            controller = captureController,
            profile = DefaultGameProfile.instance
        )
        frameSampler?.start()

        startSessionRefresher()
        captureController.updateCapturing(true, matchId)

        return START_NOT_STICKY
    }

    private fun startCaptureForeground(matchId: String) {
        val shortId = if (matchId.length >= 8) matchId.take(8) else matchId
        val notification = NotificationFactory.buildCaptureNotification(this, shortId)

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            Constants.CAPTURE_NOTIFICATION_ID,
            notification,
            foregroundType
        )
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKE_LOCK_TAG).apply {
                acquire(Constants.WAKE_LOCK_TIMEOUT_MS)
            }
            Timber.i("Acquired Partial WakeLock for screen capture")
        } catch (e: Exception) {
            Timber.e(e, "Error acquiring WakeLock")
        }
    }

    private fun startSessionRefresher() {
        serviceScope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                delay(Constants.SESSION_REFRESH_INTERVAL_MS)
                val refreshResult = authRepository.refreshIfNeeded()
                if (refreshResult.isSuccess) {
                    consecutiveFailures = 0
                } else {
                    consecutiveFailures++
                    Timber.w("Auth refresh failed (%d consecutive)", consecutiveFailures)
                    if (consecutiveFailures >= 3) {
                        Timber.e("Auth refresh failed 3 times in a row, stopping capture service")
                        stopSelf()
                        break
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("CaptureService onDestroy called")

        frameSampler?.stop()
        frameSampler = null

        projectionSession?.release()
        projectionSession = null

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error releasing WakeLock")
        }
        wakeLock = null

        captureController.updateCapturing(false)
        agreementGate.reset()
        serviceScope.cancel()
    }
}
