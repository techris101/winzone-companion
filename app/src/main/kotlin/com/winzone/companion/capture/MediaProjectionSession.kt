package com.winzone.companion.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import com.winzone.companion.util.BitmapUtils
import timber.log.Timber

class MediaProjectionSession(
    private val context: Context,
    private val onRevoked: () -> Unit
) {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    val isRunning: Boolean
        get() = projection != null && virtualDisplay != null

    fun start(resultCode: Int, data: Intent) {
        val mpm = context.getSystemService(MediaProjectionManager::class.java)
        projection = mpm.getMediaProjection(resultCode, data)

        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Timber.w("MediaProjection revoked or stopped by system")
                release()
                onRevoked()
            }
        }, Handler(Looper.getMainLooper()))

        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        Timber.i("Starting virtual display: %dx%d, density %d", width, height, density)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection?.createVirtualDisplay(
            "winzone-capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    fun acquireLatestBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        val image = reader.acquireLatestImage() ?: return null
        return try {
            BitmapUtils.imageToBitmap(image)
        } catch (e: Exception) {
            Timber.w(e, "Error converting image to bitmap")
            null
        } finally {
            image.close()
        }
    }

    fun release() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            projection?.stop()
            projection = null
            Timber.i("MediaProjectionSession released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing MediaProjectionSession")
        }
    }
}
