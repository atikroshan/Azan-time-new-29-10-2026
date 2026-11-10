package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
 * Cinematic macOS Cloth-Type Genie Fold Animation:
 * 1. Tap language button (English, Hindi, or Urdu):
 *    - Unselected buttons smoothly merge (520ms) into the tapped button with vertical motion blur.
 *    - Tapped button pulses up to 1.12x absorbing them, then settles.
 * 2. Settle Pause (140ms):
 *    - User clearly registers the single golden pill button.
 * 3. Exact macOS Cloth-Type Genie Effect (GenieFoldOverlay with 56 horizontal strips):
 *    - Screen snapshot is captured via graphicsLayer.
 *    - The entire language screen folds like silk/cloth into the tapped button using the exact 56-strip
 *      2D affine matrix S-curve cloth transform from mobile-genie-nav.
 *    - As the button moves to the right, the cloth funnel follows it and sucks into the button.
 *    - FirstTimeSetupScreen (rendered directly underneath in MainActivity) is revealed in the background!
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // GraphicsLayer to capture screenshot for the 56-strip cloth fold
    val pageGraphicsLayer = rememberGraphicsLayer()
    var outgoingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Merge animation progress for unselected buttons (0f -> 1f)
    val mergeProgress = remember { Animatable(0f) }
    val mergeAlpha = remember { Animatable(1f) }
    val mergeScale = remember { Animatable(1f) }

    // Tapped button absorption pulse scale
    val tappedPulseScale = remember { Animatable(1f) }

    // Exit animation: Tapped button rush to the right
    val buttonExitX = remember { Animatable(0f) }

    // macOS Genie Fold progress (0f -> 1f) over GENIE_DUR
    val genieProgress = remember { Animatable(0f) }

    // Screen and button positioning measurements
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val buttonStepPx = with(density) { 58.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val buttonWidthPx = with(density) { 172.dp.toPx() }
    val buttonHeightPx = with(density) { 44.dp.toPx() }

    // Track button Y centers
    val buttonCenterYs = remember { mutableStateMapOf<Int, Float>() }

    val otherButtonBlurY = (mergeProgress.value * 16f).dp
    val selectedButtonBlurX = ((buttonExitX.value / (screenWidthPx * 1.35f)) * 20f).coerceIn(0f, 20f).dp

    // Measured or calculated pivot Y for the tapped button
    val targetCenterY = remember(selectedIndex, buttonCenterYs.size, screenHeightPx) {
        val measuredY = selectedIndex?.let { buttonCenterYs[it] }
        if (measuredY != null && measuredY > 0f) {
            measuredY
        } else {
            val fraction = when (selectedIndex) {
                0 -> 0.48f
                1 -> 0.56f
                else -> 0.64f
            }
            screenHeightPx * fraction
        }
    }

    val buttonCenterX = screenWidthPx / 2f

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
            delay(140L) // Settle pause so user clearly registers the single merged button!

            // PHASE 2: CAPTURE SCREENSHOT FOR THE MACOS CLOTH-TYPE GENIE EFFECT
            val snapshot = try {
                pageGraphicsLayer.toImageBitmap().asAndroidBitmap()
            } catch (_: Exception) {
                null
            }
            outgoingBitmap = snapshot

            // PHASE 3: MACOS CLOTH GENIE FOLD (56-strip cloth matrix fold)
            launch {
                buttonExitX.animateTo(
                    screenWidthPx * 1.30f,
                    tween(GENIE_DUR.toInt(), easing = FastOutSlowInEasing)
                )
            }

            launch {
                genieProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(GENIE_DUR.toInt(), easing = LinearEasing)
                )
            }

            delay(GENIE_DUR + 40L) // Wait for macOS cloth fold to finish

            // PHASE 4: Transition directly to setup screen in chosen language
            onLanguageSelect(code)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("initial_language_screen"),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = outgoingBitmap
        if (currentBitmap != null && genieProgress.value > 0f) {
            // Layer A: EXACT macOS Cloth-Type Genie Fold (56 horizontal strips S-curve matrix deformation)
            val currentTx = buttonCenterX + buttonExitX.value
            GenieFoldOverlay(
                bitmap = currentBitmap,
                progress = genieProgress.value,
                target = TabTarget(
                    centerX = currentTx,
                    centerY = targetCenterY,
                    width = buttonWidthPx,
                    height = buttonHeightPx
                ),
                containerWidth = screenWidthPx,
                containerHeight = screenHeightPx,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Layer B: Interactive Language Selection UI, captured into pageGraphicsLayer
            val fallbackAlpha = if (genieProgress.value > 0f && outgoingBitmap == null) {
                (1f - genieProgress.value).coerceIn(0f, 1f)
            } else 1f

            val fallbackScale = if (genieProgress.value > 0f && outgoingBitmap == null) {
                (1f - 0.95f * genieProgress.value).coerceAtLeast(0.04f)
            } else 1f

            val fallbackPivotY = (targetCenterY / screenHeightPx).coerceIn(0.2f, 0.8f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = fallbackAlpha
                        this.scaleX = fallbackScale
                        this.scaleY = fallbackScale
                        this.transformOrigin = TransformOrigin(1.0f, fallbackPivotY)
                    }
                    .drawWithContent {
                        pageGraphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawContent()
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(DarkEmeraldTop, DarkEmeraldMid, DarkEmeraldBottom)
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding(),
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
