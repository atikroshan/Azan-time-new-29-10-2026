package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext

/**
 * Video Mask Reveal Overlay Transition using WhatsApp transition video (.mp4).
 * Rotated +90 degrees (right side goes up), with white color removed and made transparent via color-keying
 * so that the background image / incoming screen is smoothly revealed through the fluid wave.
 *
 * Uses pre-rendered high-definition alpha-channel WebP frames for instantaneous,
 * 60fps/120fps stutter-free rendering without any MediaPlayer state errors or decoder crashes.
 */
const val VIDEO_MASK_DURATION: Long = 850L

object TransitionFrameCache {
    private val frames = ArrayList<Bitmap>(24)
    private var isLoaded = false

    @Synchronized
    fun getFrames(context: Context): List<Bitmap> {
        if (!isLoaded) {
            try {
                for (i in 1..24) {
                    val name = String.format("transition_frames/frame_%02d.webp", i)
                    context.assets.open(name).use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) frames.add(bmp)
                    }
                }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return frames
    }
}

@Composable
fun VideoMaskOverlay(
    bitmap: Bitmap,
    progress: Float, // 0f to 1f
    containerWidth: Float,
    containerHeight: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val frameList = remember(context) { TransitionFrameCache.getFrames(context) }

    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    }
    val maskPaint = remember {
        Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
    }

    val srcRectBitmap = remember { Rect() }
    val srcRectMask = remember { Rect() }
    val dstRectF = remember { RectF() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val W = if (containerWidth > 0f) containerWidth else size.width
        val H = if (containerHeight > 0f) containerHeight else size.height
        if (W <= 0f || H <= 0f || bitmap.isRecycled || frameList.isEmpty()) return@Canvas

        val frameCount = frameList.size
        val u = progress.coerceIn(0f, 1f)
        val frameIndex = (u * (frameCount - 1)).toInt().coerceIn(0, frameCount - 1)
        val maskBitmap = frameList[frameIndex]

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas
            dstRectF.set(0f, 0f, W, H)

            // Save layer for PorterDuff composition
            val layerId = nativeCanvas.saveLayer(dstRectF, null)

            // 1. Draw outgoing screen snapshot
            srcRectBitmap.set(0, 0, bitmap.width, bitmap.height)
            nativeCanvas.drawBitmap(bitmap, srcRectBitmap, dstRectF, paint)

            // 2. Modulate with fluid transition mask:
            // White color is transparent (Alpha = 0), so DST_IN reveals the background image / incoming screen.
            // Black region is opaque (Alpha = 255), keeping the outgoing screen until swept away.
            srcRectMask.set(0, 0, maskBitmap.width, maskBitmap.height)
            nativeCanvas.drawBitmap(maskBitmap, srcRectMask, dstRectF, maskPaint)

            nativeCanvas.restoreToCount(layerId)
        }
    }
}
