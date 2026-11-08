package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Minimalist, ultra-attractive Splash Screen inspired by Google AI Studio / Gemini:
 * - Obsidian dark cosmic background with breathing ambient emerald-gold aura
 * - Centered squircle AzanLogoCard with continuous sweeping 45-degree shimmer
 * - CorelDRAW vector AzanTimeWordmark ("AZAN TIME" + "AAO ALLAH KI RAAH MEIN CHALE")
 * - Signature Google AI Studio / Gemini glowing shimmer beam progress track
 * - AI Studio minimal shimmering footer ("Powered by @tek")
 * - Butter-smooth entrance easing and 450ms seamless dissolve exit transition
 * - Auto-finishes in 10.0s or instantly on user tap
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var isDismissing by remember { mutableStateOf(false) }

    val contentScale = 1f
    val contentAlpha = 1f

    // Dissolve exit animation
    val screenAlpha by animateFloatAsState(
        targetValue = if (isDismissing) 0f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "splashExitAlpha",
        finishedListener = { finalAlpha ->
            if (finalAlpha == 0f && isDismissing) {
                onFinished()
            }
        }
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splashAuraTransition")

    // Ambient breathing aura pulse behind the logo
    val auraRadius by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraRadius"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    // Google AI Studio signature loading beam translation
    val beamTranslate by infiniteTransition.animateFloat(
        initialValue = -160f,
        targetValue = 340f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aiStudioBeamTranslate"
    )

    val beamBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x33F3DE8E),
            Color(0xFFF3DE8E),
            Color(0xFFFFFFFF),
            Color(0xFF38BDF8),
            Color.Transparent
        ),
        startX = beamTranslate - 110f,
        endX = beamTranslate + 110f
    )

    LaunchedEffect(Unit) {
        delay(10000) // Total 10s duration
        isDismissing = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF020403),
                        Color(0xFF050F0B)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { isDismissing = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Background Breathing Glow (Subtle cosmic gold & emerald halo)
        Canvas(
            modifier = Modifier
                .size(320.dp)
                .alpha(contentAlpha)
        ) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f - 24.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE5B842).copy(alpha = auraAlpha),
                        Color(0xFF0E2F23).copy(alpha = auraAlpha * 0.75f),
                        Color(0x10061A13),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = auraRadius.dp.toPx()
                ),
                radius = auraRadius.dp.toPx(),
                center = centerOffset
            )
        }

        // 1.5 Flying Golden Bubbles (Seamless drifting from top to bottom)
        FlyingGoldenBubbles(modifier = Modifier.fillMaxSize().alpha(contentAlpha))

        // 2. Central Content: Logo + Typography + Google Studio AI Glow Bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .scale(contentScale)
                .alpha(contentAlpha)
        ) {
            // Squircle Logo Card with sweeping 45-degree shimmer
            AzanLogoCard(
                size = 108.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Vector CorelDRAW Azan Time Wordmark with Punchline
            AzanTimeWordmark(
                height = 46.dp,
                topPadding = 12.dp,
                bottomPadding = 18.dp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Signature Google AI Studio / Gemini Minimal Loading Track
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(Color(0x28F3DE8E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(beamBrush)
                )
            }
        }

        // 3. Bottom Minimal Footer: AI Studio Shimmering Version & Attribution
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .alpha(contentAlpha)
        ) {
            AiStudioLoadingFooter()
        }
    }
}

/**
 * Cinematic Flying Golden Bubbles:
 * - Already distributed across the entire screen from frame 0 (no empty screen on startup)
 * - Continuous, seamless top-to-bottom slow majestic drift
 * - Natural horizontal sinusoidal sway
 * - Multi-layered glowing golden light with soft radial halos
 * - Ultra-smooth 60fps rendering
 */
@Composable
fun FlyingGoldenBubbles(modifier: Modifier = Modifier) {
    val bubbleCount = 28
    val infiniteTransition = rememberInfiniteTransition(label = "cinematicBubbles")

    // Master continuous progress loop (0f to 1f over 24 seconds, perfectly smooth)
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "masterProgress"
    )

    // Pre-calculated deterministic specs for each bubble so they stay stable across recompositions
    val bubbleSpecs = remember {
        val rand = java.util.Random(1337L)
        List(bubbleCount) { i ->
            val sizeDp = when {
                i % 7 == 0 -> 12f + rand.nextFloat() * 3.5f // Large cinematic bokeh orb (up to 15.5dp)
                i % 3 == 0 -> 7f + rand.nextFloat() * 3f   // Medium golden bubble
                else -> 3.5f + rand.nextFloat() * 2.5f     // Small floating particle
            }
            val speedCycles = 1.6f + rand.nextFloat() * 1.8f // how many full screen traversals in 24s (~7s to 15s per cycle)
            val initialYFraction = rand.nextFloat() // 0.0 to 1.0 (already distributed across screen on frame 0!)
            val xRatio = 0.04f + rand.nextFloat() * 0.92f
            val wobbleSpeed = 3f + rand.nextFloat() * 4f // cycles per 24s
            val wobbleAmp = 8f + rand.nextFloat() * 14f // dp amplitude
            val alpha = when {
                sizeDp > 10f -> 0.22f + rand.nextFloat() * 0.16f // softer for large bokeh
                sizeDp > 6f -> 0.32f + rand.nextFloat() * 0.22f
                else -> 0.38f + rand.nextFloat() * 0.32f
            }
            CinematicBubbleSpec(
                xRatio = xRatio,
                initialYFraction = initialYFraction,
                sizeDp = sizeDp,
                speedCycles = speedCycles,
                wobbleSpeed = wobbleSpeed,
                wobbleAmp = wobbleAmp,
                alpha = alpha
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        bubbleSpecs.forEach { b ->
            // Current vertical position with seamless wrap-around from top to bottom
            val rawY = (b.initialYFraction + animationProgress * b.speedCycles) % 1.0f
            // Map 0..1 to -30f..h+30f so they enter gracefully from the top and leave at the bottom
            val y = rawY * (h + 80f) - 40f

            // Gentle horizontal sway
            val wobbleAngle = (animationProgress * b.wobbleSpeed * 2.0 * Math.PI).toFloat()
            val wobblePx = kotlin.math.sin(wobbleAngle) * b.wobbleAmp.dp.toPx()
            val x = (b.xRatio * w + wobblePx).coerceIn(10f, w - 10f)

            val rPx = (b.sizeDp / 2f).dp.toPx()

            // Cinematic warm golden radial glow matching version 3.1.57 aesthetics
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFBEA).copy(alpha = b.alpha),
                        Color(0xFFF3DE8E).copy(alpha = b.alpha * 0.85f),
                        Color(0xFFE5B842).copy(alpha = b.alpha * 0.35f),
                        Color(0x00E5B842)
                    ),
                    center = Offset(x, y),
                    radius = rPx * 1.5f
                ),
                radius = rPx * 1.5f,
                center = Offset(x, y)
            )
        }
    }
}

private data class CinematicBubbleSpec(
    val xRatio: Float,
    val initialYFraction: Float,
    val sizeDp: Float,
    val speedCycles: Float,
    val wobbleSpeed: Float,
    val wobbleAmp: Float,
    val alpha: Float
)
