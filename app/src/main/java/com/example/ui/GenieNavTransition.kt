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
 * Ultra-clean macOS/iOS Genie Fold & Fabric Curtain Animation
 * Inspired by high-end fluid accordion folding shorts & smooth physics transitions.
 *
 * Features:
 * - High-density 96 horizontal satin strips for sub-pixel buttery smooth curves
 * - Two-phase S-curve funnel physics with refined ease-in-out and zero jagged edges
 * - Hyper-realistic glossy satin pleat shader with dynamic wrinkle displacement waves
 * - Flawless convergence into bottom navigation tab or initial language button
 */
const val GENIE_N: Int = 96
const val GENIE_DUR: Long = 1250L

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
    val f = 1f + (minS - 1f) * genieSS((t - 0.08f) / 0.92f) * n
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
 * Renders the outgoing page snapshot into 96 horizontal satin strips, each transformed with a 2D affine matrix
 * following the two-phase S-curve funnel physics, with ultra-clean fabric wrinkle shading & specular highlights.
 */
@Composable
fun GenieFoldOverlay(
    bitmap: Bitmap,
    progress: Float, // u in 0f..1f (linear time progress)
    target: TabTarget,
    containerWidth: Float,
    containerHeight: Float,
    modifier: Modifier = Modifier,
    fabricFold: Boolean = true
) {
    val matrix = remember { Matrix() }
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    }
    val pleatPaint = remember {
        Paint().apply {
            isAntiAlias = true
        }
    }
    val creasePaint = remember {
        Paint().apply {
            isAntiAlias = true
        }
    }
    val shaderMatrix = remember { Matrix() }
    val pleatSpanW = remember(containerWidth) {
        if (containerWidth > 0f) containerWidth else 1080f
    }
    val pleatShader = remember(fabricFold, pleatSpanW) {
        if (!fabricFold) {
            null
        } else {
            // Ultra-clean glossy satin pleats: luminous white-gold specular streaks flanked by deep fold shadows
            android.graphics.LinearGradient(
                0f, 0f, pleatSpanW, 0f,
                intArrayOf(
                    0x82000000.toInt(), // left hem deep shadow
                    0x75FFFFFF.toInt(), // satin gloss ridge 1
                    0x24000000.toInt(), // smooth transition
                    0x7A000000.toInt(), // fold 1 deep valley
                    0x6FFFFAEE.toInt(), // satin warm gold-white ridge 2
                    0x76000000.toInt(), // center deep valley
                    0x85FFFFFF.toInt(), // center brilliant satin specular gleam
                    0x7A000000.toInt(), // fold 3 deep valley
                    0x6FFFFAEE.toInt(), // satin warm ridge 4
                    0x7A000000.toInt(), // fold 4 deep valley
                    0x75FFFFFF.toInt(), // satin gloss ridge 5
                    0x82000000.toInt()  // right hem deep shadow
                ),
                floatArrayOf(0f, 0.08f, 0.14f, 0.22f, 0.34f, 0.44f, 0.52f, 0.60f, 0.70f, 0.81f, 0.92f, 1f),
                android.graphics.Shader.TileMode.MIRROR
            )
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
        val minS = max(tabW * 0.48f / W, 0.025f)
        val sh = H / GENIE_N
        val span = if (ty > 0f) ty else H

        // Sine ease-in-out: buttery smooth departure and velvet deceleration
        val u = progress.coerceIn(0f, 1f)
        val p = (0.5f - 0.5f * cos(PI.toFloat() * u)).coerceIn(0f, 1f)

        // Phase 1: Bottom edge pulled down to destination icon first, creating the clean accordion funnel
        val n = genieSS(p / 0.52f)
        // Phase 2: Top edge slides down while page flows into funnel
        val yt = ty * genieSS((p - 0.25f) / 0.75f)
        val yb = H + (ty - H) * n

        // Fade out during last 5% of progress for absolute clean finish
        val opacity = if (p > 0.95f) {
            max(0f, (1f - p) / 0.05f)
        } else {
            1f
        }
        val baseAlpha = opacity.coerceIn(0f, 1f)
        paint.alpha = (baseAlpha * 255).toInt()
        if (paint.alpha <= 0) return@Canvas

        if (pleatShader != null) {
            pleatPaint.shader = pleatShader
        }

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas
            var y0 = yt
            var pr = genieProfile(y0, n, span, minS, cx, tx)

            for (j in 0 until GENIE_N) {
                val y1 = yt + (yb - yt) * ((j + 1).toFloat() / GENIE_N)
                val cu = genieProfile(y1, n, span, minS, cx, tx)
                val baseA = (pr.f + cu.f) / 2f
                val dc = cu.c - pr.c
                // 1.0px overlap prevents any subpixel gaps between strips during accordion folding
                val sy = max(0.001f, (y1 - y0 + 1.0f) / sh)

                val tStrip = (j + 0.5f) / GENIE_N
                val envelope = kotlin.math.sin(tStrip * PI.toFloat())
                val primaryWave = kotlin.math.sin((tStrip * 5.6f - p * 2.8f) * PI.toFloat())
                val fineWrinkle = kotlin.math.sin((tStrip * 11.2f - p * 4.2f) * PI.toFloat())
                val combinedWrinkle = 0.7f * primaryWave + 0.3f * fineWrinkle

                val wrinkleScale = if (fabricFold && n > 0.015f) {
                    1f + (0.035f * combinedWrinkle * envelope * n)
                } else 1f
                val a = (baseA * wrinkleScale).coerceAtLeast(0.015f)

                val rippleOffset = if (fabricFold && n > 0.01f) {
                    combinedWrinkle * envelope * n * (1f - p * 0.25f) * (W * 0.02f)
                } else 0f

                val transX = (pr.c + cu.c) / 2f + rippleOffset - a * W / 2f - dc / 2f
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

                if (fabricFold && pleatShader != null) {
                    // Glossy satin pleats & specular wrinkle luster intensify cleanly as the cloth gathers
                    val gather = (1f - baseA).coerceIn(0f, 1f)
                    val foldIntensity = (0.32f * n + 0.68f * gather).coerceIn(0f, 1f)
                    val pAlpha = (foldIntensity * baseAlpha * 248f).toInt().coerceIn(0, 255)
                    if (pAlpha > 2) {
                        nativeCanvas.translate(0f, j * sh)
                        shaderMatrix.setTranslate(combinedWrinkle * W * 0.05f * n, 0f)
                        pleatShader.setLocalMatrix(shaderMatrix)
                        pleatPaint.alpha = pAlpha
                        nativeCanvas.drawRect(0f, 0f, W, sh, pleatPaint)

                        // High-contrast clean satin gleam on wrinkle crests & velvety shadows in folds
                        val waveIntensity = kotlin.math.abs(combinedWrinkle) * foldIntensity * baseAlpha
                        if (waveIntensity > 0.012f) {
                            creasePaint.color = if (combinedWrinkle >= 0f) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                            val maxCrease = if (combinedWrinkle >= 0f) 82f else 98f
                            creasePaint.alpha = (waveIntensity * maxCrease).toInt().coerceIn(0, 255)
                            nativeCanvas.drawRect(0f, 0f, W, sh, creasePaint)
                        }
                    }
                }

                nativeCanvas.restore()

                y0 = y1
                pr = cu
            }
        }
    }
}
