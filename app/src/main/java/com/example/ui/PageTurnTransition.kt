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
import kotlin.math.max

/**
 * Clean Book Page Turn / Page Curl Transition
 * Replaces the cloth/genie fold with a realistic 3D book page-turning curl effect.
 */
const val PAGE_TURN_DURATION: Long = 850L

@Composable
fun PageTurnOverlay(
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
    val shadowPaint = remember {
        Paint().apply {
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val W = if (containerWidth > 0f) containerWidth else size.width
        val H = if (containerHeight > 0f) containerHeight else size.height
        if (W <= 0f || H <= 0f || bitmap.isRecycled) return@Canvas

        val u = progress.coerceIn(0f, 1f)
        // Smooth sine ease-in-out for natural page turning momentum
        val p = (0.5f - 0.5f * cos(PI.toFloat() * u)).coerceIn(0f, 1f)

        // Number of horizontal strips for curl deformation (32 strips for smooth cylindrical curl)
        val N = 32
        val stripW = W / N

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas

            // Draw underlying background / static page portion if any, or render curling page strips from right to left
            for (i in 0 until N) {
                val t = i.toFloat() / N
                val nextT = (i + 1).toFloat() / N

                // Page curl calculation: as p progresses, strips on the right curl over to the left
                // Curl angle based on strip position relative to curl progress p
                val curlProgress = (p * 1.2f - t * 0.2f).coerceIn(0f, 1f)
                val angle = curlProgress * PI.toFloat()

                val stripWidth = stripW * max(0.1f, cos(angle))
                val offsetX = t * W + (1f - cos(angle)) * stripW * 0.5f
                val elevation = sin(angle) * (W * 0.15f)

                matrix.reset()
                // Apply 3D perspective / scale and translation for the curling strip
                val scaleX = max(0.05f, cos(angle))
                val skewY = sin(angle) * 0.1f
                matrix.setValues(
                    floatArrayOf(
                        scaleX, skewY, offsetX,
                        0f, 1f - (elevation / H) * 0.1f, elevation * 0.2f,
                        0f, 0f, 1f
                    )
                )

                nativeCanvas.save()
                nativeCanvas.concat(matrix)
                nativeCanvas.clipRect(0f, 0f, stripW, H)
                nativeCanvas.translate(-i * stripW, 0f)
                nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint)

                // Add page turn shadow / crease highlight on curling edge
                if (angle > 0.05f && angle < PI.toFloat() - 0.05f) {
                    val shadowAlpha = (sin(angle) * 140f).toInt().coerceIn(0, 255)
                    shadowPaint.color = 0xFF000000.toInt()
                    shadowPaint.alpha = shadowAlpha
                    nativeCanvas.drawRect(0f, 0f, stripW, H, shadowPaint)
                }

                nativeCanvas.restore()
            }
        }
    }
}
