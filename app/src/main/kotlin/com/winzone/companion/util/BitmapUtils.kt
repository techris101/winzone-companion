package com.winzone.companion.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapUtils {

    fun imageToBitmap(image: Image, targetBitmap: Bitmap? = null): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height

        val rowPadding = rowStride - pixelStride * width
        val bitmapWidth = width + rowPadding / pixelStride

        val tempBitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
        tempBitmap.copyPixelsFromBuffer(buffer)

        val cleanBitmap = if (rowPadding == 0) {
            tempBitmap
        } else {
            val cropped = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
            tempBitmap.recycle()
            cropped
        }

        return cleanBitmap
    }

    fun downscaleIfNeeded(bitmap: Bitmap, maxEdge: Int = 1280): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = max(width, height)
        if (longest <= maxEdge) {
            return bitmap
        }
        val scale = maxEdge.toFloat() / longest
        val newWidth = (width * scale).roundToInt()
        val newHeight = (height * scale).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun cropRegion(bitmap: Bitmap, normalizedRect: RectF): Bitmap {
        val left = (normalizedRect.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (normalizedRect.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (normalizedRect.right * bitmap.width).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (normalizedRect.bottom * bitmap.height).roundToInt().coerceIn(top + 1, bitmap.height)

        val cropWidth = right - left
        val cropHeight = bottom - top

        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    fun isRectInsideRegion(box: Rect, region: RectF, frameWidth: Int, frameHeight: Int): Boolean {
        val normLeft = box.left.toFloat() / frameWidth
        val normTop = box.top.toFloat() / frameHeight
        val normRight = box.right.toFloat() / frameWidth
        val normBottom = box.bottom.toFloat() / frameHeight

        val centerX = (normLeft + normRight) / 2f
        val centerY = (normTop + normBottom) / 2f

        return region.contains(centerX, centerY)
    }
}
