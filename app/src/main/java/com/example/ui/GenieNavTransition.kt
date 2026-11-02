package com.example.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/**
 * macOS Genie Fold Page Transition for Bottom Tab Navigation
 * Faithful conversion of mobile-genie-nav.html
 *
 * Requirements:
 * - Easily editable constants at top: N=56 horizontal strips, DUR=780ms duration
 * - Outgoing page folds into ITS OWN nav button
 * - Incoming page is rendered immediately underneath
 * - S-curve funnel profile with 2 phases (bottom pulled first, top slides down)
 * - Sine ease-in-out and fade out in the last 6%
 * - Respects prefers-reduced-motion with simple fade fallback
 */
const val GENIE_N: Int = 56
const val GENIE_DUR: Long = 780L

data class TabTarget(
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

data class GenieProfileResult(val f: Float, val c: Float)

fun genieClamp(x: Float): Float = when {
    x < 0f -> 0f
    x > 1f -> 1f
    else -> x
}

/**
 * Smoothstep S-curve: ss(x) = x * x * (3 - 2 * x)
 */
fun genieSS(x: Float): Float {
    val cx = genieClamp(x)
    return cx * cx * (3f - 2f * cx)
}

/**
 * Funnel profile: Screen fixed S-curve. Width factor f and horizontal center c change based on y.
 */
fun genieProfile(
    y: Float,
    n: Float,
    span: Float,
    minS: Float,
    cx: Float,
    tx: Float
): GenieProfileResult {
    val t = genieClamp(y / span)
    val f = 1f + (minS - 1f) * genieSS((t - 0.1f) / 0.9f) * n
    val c = cx + (tx - cx) * genieSS(t) * n
    return GenieProfileResult(f, c)
}

/**
 * Checks system-level setting for reduced motion / disabled animations.
 */
@Composable
fun rememberPrefersReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Genie Fold Overlay Canvas
 * Renders the outgoing page snapshot into N horizontal strips, each transformed with a 2D affine matrix
 * exactly following the two-phase S-curve funnel physics from mobile-genie-nav.html.
 */
@Composable
fun GenieFoldOverlay(
    bitmap: Bitmap,
    progress: Float, // u in 0f..1f (linear time progress)
    target: TabTarget,
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

        val tx = if (target.centerX > 0f) target.centerX else (W / 2f)
        val ty = if (target.centerY > 0f) target.centerY else H
        val cx = W / 2f
        val tabW = if (target.width > 0f) target.width else (W / 3f)
        val minS = max(tabW * 0.55f / W, 0.03f)
        val sh = H / GENIE_N
        val span = if (ty > 0f) ty else H

        // Sine ease-in-out: smooth departure and deceleration
        val u = progress.coerceIn(0f, 1f)
        val p = (0.5f - 0.5f * cos(PI.toFloat() * u)).coerceIn(0f, 1f)

        // Phase 1: Bottom edge pulled down to icon first, creating the funnel
        val n = genieSS(p / 0.55f)
        // Phase 2: Top edge slides down while page flows into funnel
        val yt = ty * genieSS((p - 0.28f) / 0.72f)
        val yb = H + (ty - H) * n

        // Fade out during last 6% of progress
        val opacity = if (p > 0.94f) {
            max(0f, (1f - p) / 0.06f)
        } else {
            1f
        }
        paint.alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
        if (paint.alpha <= 0) return@Canvas

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas
            var y0 = yt
            var pr = genieProfile(y0, n, span, minS, cx, tx)

            for (j in 0 until GENIE_N) {
                val y1 = yt + (yb - yt) * ((j + 1).toFloat() / GENIE_N)
                val cu = genieProfile(y1, n, span, minS, cx, tx)
                val a = (pr.f + cu.f) / 2f
                val dc = cu.c - pr.c
                // 0.8px overlap prevents subpixel gaps between strips during deformation
                val sy = max(0.001f, (y1 - y0 + 0.8f) / sh)
                val transX = (pr.c + cu.c) / 2f - a * W / 2f - dc / 2f
                val transY = y0

                matrix.reset()
                matrix.setValues(
                    floatArrayOf(
                        a, dc / sh, transX,
                        0f, sy, transY,
                        0f, 0f, 1f
                    )
                )

                nativeCanvas.save()
                nativeCanvas.concat(matrix)
                nativeCanvas.clipRect(0f, 0f, W, sh)
                nativeCanvas.translate(0f, -j * sh)
                nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint)
                nativeCanvas.restore()

                y0 = y1
                pr = cu
            }
        }
    }
}
