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
    var hasEntered by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    // Smooth entrance animations
    val contentScale by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0.86f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "splashEntranceScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "splashEntranceAlpha"
    )

    // Dissolve exit animation
    val screenAlpha by animateFloatAsState(
        targetValue = if (isDismissing) 0f else 1f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
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
        hasEntered = true // Start entrance animation immediately
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
