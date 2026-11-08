package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlinx.coroutines.delay

/**
 * Minimalist, ultra-attractive Splash Screen inspired by Google AI Studio / Gemini:
 * - Real 720p Dust & Snow Particle Overlay Video Background running in slow cinematic speed
 * - First 2 seconds cinematic camera shake (organic 3D-feel subtle micro-shake & rotation)
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

    // Camera Shake for the first 2 seconds (0 to 2000ms)
    // Decays gracefully from 1.0f at start to 0.0f at 2.0s
    val shakeIntensity = remember { Animatable(1.0f) }
    LaunchedEffect(Unit) {
        // High-energy cinematic impact during first 600ms, then organic decay settling by 2.0s
        delay(600)
        shakeIntensity.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
    }

    // High frequency shake oscillator (approx 16Hz micro camera shake)
    val shakeTransition = rememberInfiniteTransition(label = "cameraShakeTransition")
    val shakeCycle by shakeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 130, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shakeCycle"
    )

    val currentShake = shakeIntensity.value
    // Translation shake: horizontal +/- 5dp, vertical +/- 4dp, rotation +/- 0.7 deg
    val shakeOffsetX = (kotlin.math.sin(shakeCycle * 1.7f) * 5.5f * currentShake)
    val shakeOffsetY = (kotlin.math.cos(shakeCycle * 2.3f) * 4.0f * currentShake)
    val shakeRotation = (kotlin.math.sin(shakeCycle) * 0.75f * currentShake)
    val cameraScale = 1.0f + (0.025f * currentShake)

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
        delay(6000) // Total 6s duration
        isDismissing = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(Color(0xFF020403))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { isDismissing = true }
            )
            .graphicsLayer {
                // Apply Camera Shake & Micro Zoom to entire screen viewport
                translationX = shakeOffsetX.dp.toPx()
                translationY = shakeOffsetY.dp.toPx()
                rotationZ = shakeRotation
                scaleX = cameraScale
                scaleY = cameraScale
            },
        contentAlignment = Alignment.Center
    ) {
        // 0. Slow Cinematic Video Background (Random Dust, Snow & Smoke Particle Overlay)
        SplashVideoBackground(
            modifier = Modifier.fillMaxSize()
        )

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
                        Color(0xFFE5B842).copy(alpha = auraAlpha * 0.70f),
                        Color(0xFF0E2F23).copy(alpha = auraAlpha * 0.50f),
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

        // 1.5 Soft Golden Ambient Light Dust Motes Overlay
        CinematicFallingDust(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha * 0.55f)
        )

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
 * Pure Cinematic Falling Dust Particles:
 * - Natural golden dust motes gently drifting downwards from top to bottom
 * - Ultra-fine dust specks (1.0 - 2.8dp) with soft atmospheric twinkling & air shimmer
 * - No circular bubble rings or heavy orbs; pure cinematic dust / light motes
 * - Distributed across the entire screen from frame 0 for zero startup void
 * - Multi-speed parallax layers with natural horizontal air sway
 */
@Composable
fun CinematicFallingDust(modifier: Modifier = Modifier) {
    val dustCount = 42
    val infiniteTransition = rememberInfiniteTransition(label = "cinematicFallingDust")

    // Master continuous time progression (40s full cycle for slow, graceful downward fall)
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dustFallProgress"
    )

    val dustSpecs = remember {
        val rand = java.util.Random(20261108L)
        List(dustCount) { i ->
            // Dust particle size: tiny glittering specks (1.0 - 2.8dp), rare soft bokeh fleck (3.5dp)
            val sizeDp = when {
                i % 8 == 0 -> 2.8f + rand.nextFloat() * 0.8f   // Subtle soft bokeh dust fleck
                i % 3 == 0 -> 1.8f + rand.nextFloat() * 0.8f   // Medium dust mote
                else -> 1.0f + rand.nextFloat() * 0.7f         // Micro golden dust sparkle
            }
            // Gentle falling speed: 1.0 to 1.8 screen height traversals per 40 seconds (22s to 40s per full screen fall)
            val speedCycles = 1.0f + rand.nextFloat() * 0.8f
            val initialYFraction = rand.nextFloat()
            val xRatio = 0.02f + rand.nextFloat() * 0.96f
            val swaySpeed = 1.5f + rand.nextFloat() * 2.5f
            val swayAmp = 3f + rand.nextFloat() * 6f // gentle drift sway in air
            val twinkleSpeed = 3f + rand.nextFloat() * 5f
            val phase = rand.nextFloat() * 6.28318f
            val baseAlpha = when {
                sizeDp > 2.5f -> 0.35f + rand.nextFloat() * 0.25f
                sizeDp > 1.7f -> 0.45f + rand.nextFloat() * 0.30f
                else -> 0.55f + rand.nextFloat() * 0.35f
            }
            CinematicDustSpec(
                xRatio = xRatio,
                initialYFraction = initialYFraction,
                sizeDp = sizeDp,
                speedCycles = speedCycles,
                swaySpeed = swaySpeed,
                swayAmp = swayAmp,
                twinkleSpeed = twinkleSpeed,
                phase = phase,
                alpha = baseAlpha
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        dustSpecs.forEach { d ->
            // Downward traversal: initial position + progress * speed -> wrapped seamlessly
            val rawY = (d.initialYFraction + animationProgress * d.speedCycles) % 1.0f
            val y = rawY * (h + 40f) - 20f

            // Atmospheric air currents horizontal sway
            val swayAngle = (animationProgress * d.swaySpeed * 2.0 * Math.PI + d.phase).toFloat()
            val swayPx = kotlin.math.sin(swayAngle) * d.swayAmp.dp.toPx()
            val x = (d.xRatio * w + swayPx).coerceIn(4f, w - 4f)

            // Cinematic dust twinkling
            val twinkle = 0.75f + 0.25f * kotlin.math.sin((animationProgress * d.twinkleSpeed * 2.0 * Math.PI + d.phase).toFloat())
            val effectiveAlpha = (d.alpha * twinkle).coerceIn(0.12f, 0.90f)

            val rPx = (d.sizeDp / 2f).dp.toPx()

            // Draw glowing golden dust mote (soft golden center with soft radiant halo)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFBEA).copy(alpha = effectiveAlpha),
                        Color(0xFFF3DE8E).copy(alpha = effectiveAlpha * 0.75f),
                        Color(0xFFE5B842).copy(alpha = effectiveAlpha * 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = rPx * 2.2f
                ),
                radius = rPx * 2.2f,
                center = Offset(x, y)
            )

            // Sharp pinpoint glittering center for micro dust specks
            drawCircle(
                color = Color(0xFFFFFDF5).copy(alpha = effectiveAlpha),
                radius = (rPx * 0.75f).coerceAtLeast(1.0f),
                center = Offset(x, y)
            )
        }
    }
}

private data class CinematicDustSpec(
    val xRatio: Float,
    val initialYFraction: Float,
    val sizeDp: Float,
    val speedCycles: Float,
    val swaySpeed: Float,
    val swayAmp: Float,
    val twinkleSpeed: Float,
    val phase: Float,
    val alpha: Float
)

/**
 * Hardware-accelerated Video Background:
 * Plays the random dust, snow and smoke particle overlay video in slow cinematic speed (0.70x).
 * Runs seamlessly on loop using TextureView without audio or black flicker.
 */
@Composable
fun SplashVideoBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isVideoReady by remember { mutableStateOf(false) }

    val videoAlpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1.0f else 0f,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "splashVideoAlpha"
    )

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .alpha(videoAlpha),
            factory = { ctx ->
                TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        private var mediaPlayer: MediaPlayer? = null

                        override fun onSurfaceTextureAvailable(
                            surface: android.graphics.SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            try {
                                val mp = MediaPlayer().apply {
                                    setSurface(android.view.Surface(surface))
                                    val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash_dust_bg}")
                                    setDataSource(ctx, uri)
                                    isLooping = true
                                    setVolume(0f, 0f)
                                    setOnPreparedListener { player ->
                                        try {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                                player.playbackParams = player.playbackParams.setSpeed(0.70f)
                                            }
                                        } catch (_: Throwable) {
                                        }
                                        player.start()
                                        isVideoReady = true
                                    }
                                    prepareAsync()
                                }
                                mediaPlayer = mp
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: android.graphics.SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                        }

                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                            try {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                            } catch (_: Throwable) {
                            }
                            mediaPlayer = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
                        }
                    }
                }
            }
        )
    }
}

