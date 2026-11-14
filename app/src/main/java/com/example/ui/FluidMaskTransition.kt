package com.example.ui

import android.graphics.Bitmap
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
 * Fluid Mask Transition (Inspired by attached video mask rotated 90 degrees)
 * An organic fluid wave sweeps from the right across the screen, wiping away the outgoing bitmap
 * to reveal the incoming background image/screen underneath.
 */
const val FLUID_MASK_DURATION: Long = 750L

@Composable
fun FluidMaskOverlay(
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
    val edgeGlowPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = 0xFF34D399.toInt() // Vibrant emerald glowing edge
            strokeWidth = 14f
            style = Paint.Style.STROKE
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val W = if (containerWidth > 0f) containerWidth else size.width
        val H = if (containerHeight > 0f) containerHeight else size.height
        if (W <= 0f || H <= 0f || bitmap.isRecycled) return@Canvas

        val u = progress.coerceIn(0f, 1f)
        val p = (0.5f - 0.5f * cos(PI.toFloat() * u)).coerceIn(0f, 1f)

        // Fluid wave sweeping from right (W * 1.6) to left (-0.6 * W) rotated 90 degrees
        val revealX = W * (1.6f - 2.4f * p)

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas

            nativeCanvas.save()

            // Create organic path for the fluid wipe sweeping from right
            val maskPath = android.graphics.Path().apply {
                moveTo(W, 0f)
                lineTo(W, H)
                lineTo(revealX - W * 0.35f, H)
                // Organic fluid wave curve rotating/sweeping from right
                cubicTo(
                    revealX + W * 0.45f, H * 0.75f,
                    revealX - W * 0.55f, H * 0.25f,
                    revealX, 0f
                )
                close()
            }

            // Clip canvas so the outgoing bitmap is wiped away by the fluid wave, revealing incoming background image
            nativeCanvas.clipPath(maskPath, android.graphics.Region.Op.DIFFERENCE)
            nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint)

            // Draw glowing liquid wave edge along the cut path
            nativeCanvas.drawPath(maskPath, edgeGlowPaint)

            nativeCanvas.restore()
        }
    }
}
