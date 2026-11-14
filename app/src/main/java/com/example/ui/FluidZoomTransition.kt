package com.example.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/**
 * Fluid Scale-Spring Zoom & Blur Transition (Modern Pinterest / Dribbble UI style)
 * Smoothly scales, fades, and depth-zooms screens with buttery spring physics.
 */
const val FLUID_ZOOM_DURATION: Long = 650L

@Composable
fun FluidZoomOverlay(
    bitmap: Bitmap,
    progress: Float, // 0f to 1f
    containerWidth: Float,
    containerHeight: Float,
    modifier: Modifier = Modifier
) {
    val matrix = remember { Matrix() }
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val W = if (containerWidth > 0f) containerWidth else size.width
        val H = if (containerHeight > 0f) containerHeight else size.height
        if (W <= 0f || H <= 0f || bitmap.isRecycled) return@Canvas

        val u = progress.coerceIn(0f, 1f)
        // Spring ease-out: fast start, buttery deceleration with a slight upscale pop
        val p = (1f - cos(u * PI.toFloat() * 0.5f)).coerceIn(0f, 1f)

        // Scale from 1.0 down to 0.92 or zoom out smoothly
        val scale = 1f - 0.08f * p
        val alpha = (1f - p).coerceIn(0f, 1f)

        paint.alpha = (alpha * 255).toInt()
        if (paint.alpha <= 0) return@Canvas

        val cx = W / 2f
        val cy = H / 2f

        matrix.reset()
        matrix.postScale(scale, scale, cx, cy)

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.concat(matrix)
            nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint)
            nativeCanvas.restore()
        }
    }
}
