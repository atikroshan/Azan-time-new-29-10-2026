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
import kotlin.math.sin

/**
 * Liquid Spill / Fluid Flow Transition
 * Organic wave sweeping from right side upwards across the screen.
 */
const val LIQUID_SPILL_DURATION: Long = 750L

@Composable
fun LiquidSpillOverlay(
    bitmap: Bitmap,
    progress: Float, // 0f to 1f
    containerWidth: Float,
    containerHeight: Float,
    modifier: Modifier = Modifier
) {
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    }
    val liquidPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = 0xFF0D281C.toInt() // Rich deep Islamic green liquid background matching app theme
        }
    }
    val spillEdgePaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = 0xFF34D399.toInt() // Bright emerald spill edge highlight
            strokeWidth = 12f
            style = Paint.Style.STROKE
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val W = if (containerWidth > 0f) containerWidth else size.width
        val H = if (containerHeight > 0f) containerHeight else size.height
        if (W <= 0f || H <= 0f || bitmap.isRecycled) return@Canvas

        val u = progress.coerceIn(0f, 1f)
        val p = (0.5f - 0.5f * cos(PI.toFloat() * u)).coerceIn(0f, 1f)

        // Fluid wave sweeping from right (W * 1.5) towards left (-0.5 * W) with upward rotation
        val waveX = W * (1.3f - 1.8f * p)
        val waveY = H * (0.5f - 0.4f * sin(p * PI.toFloat()))

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas

            // Draw outgoing bitmap
            nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint)

            // Draw organic liquid wave sweeping from right upwards
            val androidPath = android.graphics.Path().apply {
                moveTo(W, 0f)
                lineTo(W, H)
                lineTo(waveX - W * 0.2f, H)
                // Organic cubic bezier curve flowing upwards and rightwards
                cubicTo(
                    waveX + W * 0.3f, H * 0.7f,
                    waveX - W * 0.4f, H * 0.3f,
                    waveX, 0f
                )
                close()
            }

            nativeCanvas.drawPath(androidPath, liquidPaint)
            nativeCanvas.drawPath(androidPath, spillEdgePaint)
        }
    }
}
