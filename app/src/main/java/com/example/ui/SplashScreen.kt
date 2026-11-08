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
 * Flying Golden Bubbles:
 * - Ultra-lightweight, hardware-accelerated Canvas drawing (0 recompositions)
 * - Small golden light color transparent circles
 * - Random sizes and initial positions
 * - Flying very slow from top to bottom
 * - Seamless looping animation
 */
@Composable
fun FlyingGoldenBubbles(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubbleTransition")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleTime"
    )

    val bubbles = remember {
        List(24) { i ->
            val rand = java.util.Random(i * 37L + 11L)
            BubbleSpec(
                xRatio = rand.nextFloat(),
                yOffsetRatio = rand.nextFloat(),
                radiusDp = 2.5f + rand.nextFloat() * 4.5f,
                speedFactor = 0.00007f + rand.nextFloat() * 0.00006f,
                wobbleFreq = 0.0018f + rand.nextFloat() * 0.0012f,
                wobbleAmpDp = 6f + rand.nextFloat() * 8f,
                alpha = 0.20f + rand.nextFloat() * 0.28f
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = time

        bubbles.forEach { b ->
            val progress = (b.yOffsetRatio + t * b.speedFactor) % 1.0f
            val y = progress * (h + 60f) - 30f
            val wobblePx = kotlin.math.sin(t * b.wobbleFreq) * b.wobbleAmpDp.dp.toPx()
            val x = (b.xRatio * w + wobblePx).coerceIn(10f, w - 10f)
            val rPx = b.radiusDp.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF3DE8E).copy(alpha = b.alpha),
                        Color(0xFFE5B842).copy(alpha = b.alpha * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = rPx * 1.6f
                ),
                radius = rPx,
                center = Offset(x, y)
            )
        }
    }
}

private data class BubbleSpec(
    val xRatio: Float,
    val yOffsetRatio: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val wobbleFreq: Float,
    val wobbleAmpDp: Float,
    val alpha: Float
)
