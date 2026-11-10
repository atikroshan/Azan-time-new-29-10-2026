package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

private val GoldAccent = Color(0xFFE5B842)
private val GoldLight = Color(0xFFFFF2B2)
private val DarkEmeraldTop = Color(0xFF04150E)
private val DarkEmeraldMid = Color(0xFF0C2C1D)
private val DarkEmeraldBottom = Color(0xFF030D08)

private data class LanguageOption(
    val code: String,
    val label: String
)

private val languages = listOf(
    LanguageOption(code = "en", label = "ENGLISH"),
    LanguageOption(code = "hi", label = "हिंदी"),
    LanguageOption(code = "ur", label = "اردو")
)

/**
 * One-Time Initial Language Selection Screen.
 * Displayed on first launch as a foreground overlay BEFORE FirstTimeSetupScreen.
 *
 * Cinematic macOS Genie Fold Animation:
 * 1. Tap language button (English, Hindi, or Urdu):
 *    - Unselected buttons smoothly merge (520ms) into the tapped button with vertical motion blur.
 *    - Tapped button pulses up to 1.12x absorbing them, then settles.
 * 2. Settle Pause (150ms):
 *    - User clearly registers the single golden pill button.
 * 3. Tapped Button Leads the Exit:
 *    - Accelerates to the right screen edge with horizontal motion blur.
 * 4. macOS Genie Fold Screen Exit (matching mobile-genie-nav physics):
 *    - Screen DOES NOT simply slide flat. Instead, it funnels and folds into the button's right exit point
 *      using the two-phase S-curve smoothstep physics from macOS Genie:
 *      - Phase 1 (p < 0.25): Funnel formation, pulling the leading edge toward the button vortex.
 *      - Phase 2 (p >= 0.25): S-curve suction where the screen narrows, bends, and flows like silk into the right-edge vortex point.
 *      - 3D perspective fold (rotationY + rotationZ) and directional motion blur.
 *    - As the language page funnels and collapses into the right-edge vortex, FirstTimeSetupScreen (rendered directly underneath)
 *      is dramatically revealed in the background!
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // Merge animation progress for unselected buttons (0f -> 1f)
    val mergeProgress = remember { Animatable(0f) }
    val mergeAlpha = remember { Animatable(1f) }
    val mergeScale = remember { Animatable(1f) }

    // Tapped button absorption pulse scale
    val tappedPulseScale = remember { Animatable(1f) }

    // Exit animation: Tapped button rush to the right
    val buttonExitX = remember { Animatable(0f) }

    // macOS Genie Fold progress (0f -> 1f) over 740ms
    val genieProgress = remember { Animatable(0f) }

    // Screen and button positioning measurements
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val buttonStepPx = with(density) { 58.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Track button Y centers
    val buttonCenterYs = remember { mutableStateMapOf<Int, Float>() }

    val otherButtonBlurY = (mergeProgress.value * 16f).dp
    val selectedButtonBlurX = ((buttonExitX.value / (screenWidthPx * 1.35f)) * 20f).coerceIn(0f, 20f).dp

    // Measured or calculated pivot Y fraction for the tapped button
    val targetPivotY = remember(selectedIndex, buttonCenterYs.size, screenHeightPx) {
        val measuredY = selectedIndex?.let { buttonCenterYs[it] }
        if (measuredY != null && screenHeightPx > 0f) {
            (measuredY / screenHeightPx).coerceIn(0.25f, 0.75f)
        } else {
            when (selectedIndex) {
                0 -> 0.48f
                1 -> 0.56f
                else -> 0.64f
            }
        }
    }

    // macOS Genie two-phase S-curve calculations (from mobile-genie-nav.html)
    val u = genieProgress.value.coerceIn(0f, 1f)
    // Sine ease-in-out: smooth departure and fluid deceleration
    val p = (0.5f - 0.5f * cos(PI.toFloat() * u)).coerceIn(0f, 1f)

    // Smoothstep function
    fun ss(x: Float): Float {
        val cx = x.coerceIn(0f, 1f)
        return cx * cx * (3f - 2f * cx)
    }

    // Phase 1 (p: 0..0.55): Leading edge pulled into funnel vortex
    val n = ss(p / 0.55f)
    // Phase 2 (p: 0.20..1.0): Full page body flows through S-curve funnel into the exit vortex
    val flowProgress = ss((p - 0.20f) / 0.80f)

    // Genie funnel transformations
    val genieScaleX = if (p < 0.20f) {
        1f + 0.08f * n
    } else {
        (1.08f - 1.05f * flowProgress).coerceAtLeast(0.02f)
    }

    val genieScaleY = (1f - 0.96f * flowProgress).coerceAtLeast(0.02f)
    val genieTransX = (screenWidthPx * 0.30f * n) + (screenWidthPx * 0.90f * flowProgress)
    val genieRotationY = 28f * flowProgress
    val genieRotationZ = -5f * (1f - targetPivotY) * flowProgress

    val genieBlurX = (flowProgress * 22f).dp
    val genieBlurY = (flowProgress * 12f).dp

    // Keep page completely visible and opaque during the funnel, fading only in the final 8%
    val genieOpacity = if (p > 0.92f) {
        ((1f - p) / 0.08f).coerceIn(0f, 1f)
    } else {
        1f
    }

    fun handleLanguageTap(index: Int, code: String) {
        if (isTransitioning) return
        isTransitioning = true
        selectedIndex = index

        coroutineScope.launch {
            // PHASE 1: JUICY SMOOTH MERGE (520ms) - unselected buttons glide smoothly into tapped button
            launch {
                mergeProgress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            }
            launch {
                mergeAlpha.animateTo(0f, tween(460, delayMillis = 50, easing = FastOutLinearInEasing))
            }
            launch {
                mergeScale.animateTo(0.35f, tween(520, easing = FastOutSlowInEasing))
            }
            // Tapped button expands slightly to absorb them, then settles
            launch {
                tappedPulseScale.animateTo(1.12f, tween(260, easing = FastOutSlowInEasing))
                tappedPulseScale.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
            }

            delay(530L) // Wait for merge animation to complete
            delay(150L) // Settle pause so user clearly registers the single merged button!

            // PHASE 2: TAPPED BUTTON RUSHES TO THE RIGHT
            launch {
                buttonExitX.animateTo(
                    screenWidthPx * 1.35f,
                    tween(460, easing = FastOutSlowInEasing)
                )
            }

            // PHASE 3: MACOS GENIE FOLD EXIT (740ms) - Screen funnels into the button exit vortex
            delay(50L) // Slight stagger so the button clearly leads into the right edge

            launch {
                genieProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(740, easing = androidx.compose.animation.core.LinearEasing)
                )
            }

            delay(750L) // Complete macOS Genie fold

            // PHASE 4: Transition directly to setup screen in chosen language
            onLanguageSelect(code)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.transformOrigin = TransformOrigin(1.0f, targetPivotY)
                this.scaleX = genieScaleX
                this.scaleY = genieScaleY
                this.translationX = genieTransX
                this.rotationY = genieRotationY
                this.rotationZ = genieRotationZ
                this.cameraDistance = 16f * density.density
                this.alpha = genieOpacity
            }
            .blur(radiusX = genieBlurX, radiusY = genieBlurY)
            .background(
                Brush.verticalGradient(
                    listOf(DarkEmeraldTop, DarkEmeraldMid, DarkEmeraldBottom)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("initial_language_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. BRAND LOGO (Squircle AzanLogoCard)
            AzanLogoCard(size = 92.dp)

            Spacer(modifier = Modifier.height(4.dp))

            // 2. WORDMARK & PUNCHLINE ("AZAN TIME" + "AAO ALLAH KI RAAH MEIN CHALE")
            AzanTimeWordmark(
                height = 46.dp,
                topPadding = 8.dp,
                bottomPadding = 6.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. MULTILINGUAL SELECT LANGUAGE HEADER
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "SELECT LANGUAGE",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    letterSpacing = 1.6.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "भाषा चुनें  •  زبان منتخب کریں",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GoldAccent.copy(alpha = 0.85f),
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Lower down as requested ("Language button thoda niche loa")
            Spacer(modifier = Modifier.height(44.dp))

            // 4. VERTICAL STACK OF LANGUAGE PILL BUTTONS WITH MERGE & LAUNCH ANIMATION
            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                languages.forEachIndexed { i, option ->
                    val isTapped = selectedIndex == i
                    val isOther = selectedIndex != null && !isTapped
                    val isInitiallySelected = selectedIndex == null && option.code == "en"

                    // Calculate translation towards tapped button
                    val targetOffsetY = if (selectedIndex != null) {
                        (selectedIndex!! - i) * buttonStepPx * mergeProgress.value
                    } else {
                        0f
                    }

                    val buttonScale = when {
                        isTapped -> tappedPulseScale.value
                        isOther -> mergeScale.value
                        else -> 1f
                    }

                    val buttonAlpha = if (isOther) mergeAlpha.value else 1f
                    val buttonExitTranslationX = if (isTapped) buttonExitX.value else 0f
                    val buttonBlurX = if (isTapped) selectedButtonBlurX else 0.dp
                    val buttonBlurY = if (isOther) otherButtonBlurY else 0.dp

                    LanguagePillButton(
                        option = option,
                        isSelected = isTapped || isInitiallySelected,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                buttonCenterYs[i] = pos.y + (coords.size.height / 2f)
                            }
                            .graphicsLayer {
                                this.translationY = targetOffsetY
                                this.translationX = buttonExitTranslationX
                                this.scaleX = buttonScale
                                this.scaleY = buttonScale
                                this.alpha = buttonAlpha
                            }
                            .blur(radiusX = buttonBlurX, radiusY = buttonBlurY),
                        onClick = {
                            handleLanguageTap(i, option.code)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 5. FOOTER: Version and Powered By as requested
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(GoldAccent.copy(alpha = 0.7f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "v${com.example.BuildConfig.VERSION_NAME} • Powered by @tek",
                    fontSize = 12.sp,
                    color = Color(0xFF8BAA9B),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(GoldAccent.copy(alpha = 0.7f), CircleShape)
                )
            }
        }
    }
}

/**
 * Compact Capsule Pill button mirroring Home Page language button aesthetic:
 * - Selected: Warm Gold vertical gradient (#F3DE8E -> #D49B37) + Gold border (#FFD700) + Dark text (#0D131F)
 * - Unselected: Deep Navy/Slate vertical gradient (#131926 -> #0F1522) + Navy border (#26334A) + White text
 * - Shape: Pure capsule pill (RoundedCornerShape(percent = 50))
 */
@Composable
private fun LanguagePillButton(
    option: LanguageOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "lang_pill_press_scale"
    )

    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .width(172.dp)
            .height(44.dp)
            .scale(pressScale)
            .clip(shape)
            .background(
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(Color(0xFFF3DE8E), Color(0xFFD49B37))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF131926), Color(0xFF0F1522))
                    )
                }
            )
            .border(
                width = if (isSelected) 1.6.dp else 1.1.dp,
                color = if (isSelected) Color(0xFFFFD700) else Color(0xFF26334A),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (isSelected) Color(0xFF0D131F) else Color(0xFFFFD700)),
                onClick = onClick
            )
            .testTag("lang_pill_${option.code}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.label,
            fontSize = if (option.code == "ur") 17.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF0D131F) else Color.White,
            letterSpacing = if (option.code == "en") 1.2.sp else 0.4.sp,
            textAlign = TextAlign.Center
        )
    }
}
