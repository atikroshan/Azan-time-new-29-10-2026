package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
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
 * One-Time Initial Language Selection Screen:
 * 1. Tapping a language pill immediately updates selection state & preselects language underneath.
 * 2. Step 1 (540ms): The 2 unselected pills squeeze horizontally into 44dp circles with ground shadow & text blur.
 * 3. Step 2 (1050ms): The 2 unselected circles roll left & right off-screen (720° rotation + Trim-Path speed lines).
 * 4. Step 3: Launches the same Tap-Origin Liquid Portal Reveal transition (1150ms) from the selected pill's center.
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLanguagePreselected: (String) -> Unit = {},
    onStartPortalReveal: ((code: String, centerX: Float, centerY: Float, startRadius: Float) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val prefersReducedMotion = rememberPrefersReducedMotion()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // Unselected pill squeeze (1f -> 0f) and roll-off (0f -> 1f) progress
    val otherWidthFraction = remember { Animatable(1f) }
    val otherTextBlurProgress = remember { Animatable(0f) }
    val otherShadowAlpha = remember { Animatable(0f) }
    val rollProgress = remember { Animatable(0f) }

    // Coordinates in root for exact Tap-Origin Liquid Portal Reveal
    var rootOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var screenWidthPx by remember { mutableFloatStateOf(0f) }
    val buttonCenterXs = remember { mutableStateListOf(0f, 0f, 0f) }
    val buttonCenterYs = remember { mutableStateListOf(0f, 0f, 0f) }

    val pillFullWidth = 172.dp
    val pillCircleDiameter = 44.dp

    fun handleLanguageTap(index: Int, code: String) {
        if (isTransitioning) return
        isTransitioning = true
        selectedIndex = index
        onLanguagePreselected(code)

        val fallbackCx = if (screenWidthPx > 0f) screenWidthPx / 2f else with(density) { 180.dp.toPx() }
        val fallbackCy = with(density) { (380 + index * 58).dp.toPx() }
        val startCx = if (buttonCenterXs[index] > 0f) {
            (buttonCenterXs[index] - rootOriginInRoot.x).coerceAtLeast(0f)
        } else fallbackCx
        val startCy = if (buttonCenterYs[index] > 0f) {
            (buttonCenterYs[index] - rootOriginInRoot.y).coerceAtLeast(0f)
        } else fallbackCy
        val startRadius = with(density) { (pillCircleDiameter / 2f).toPx() }

        if (prefersReducedMotion) {
            if (onStartPortalReveal != null) {
                onStartPortalReveal(code, startCx, startCy, startRadius)
            } else {
                onLanguageSelect(code)
            }
            return
        }

        coroutineScope.launch {
            // Step 1 (540ms): Squeeze unselected pills into 44.dp circles with subtle shadow & text blur
            launch {
                otherTextBlurProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            launch {
                otherShadowAlpha.animateTo(
                    targetValue = 0.50f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                )
            }
            otherWidthFraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 540,
                    easing = CubicBezierEasing(0.35f, 0f, 0.2f, 1f)
                )
            )

            // Step 2 (1050ms): Roll both unselected circles off-screen (Right & Left) with 720° spin + speed lines
            launch {
                delay(720)
                otherShadowAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 280)
                )
            }
            rollProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1050,
                    easing = CubicBezierEasing(0.45f, 0.05f, 0.85f, 0.45f)
                )
            )

            // Brief beat so the selected pill cleanly anchors the portal origin
            delay(160)

            if (onStartPortalReveal != null) {
                onStartPortalReveal(code, startCx, startCy, startRadius)
            } else {
                onLanguageSelect(code)
            }
        }
    }

    // Track which unselected button rolls Right (+1f) and which rolls Left (-1f)
    val unselectedIndices = remember(selectedIndex) {
        if (selectedIndex == null) emptyList()
        else languages.indices.filter { it != selectedIndex }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkEmeraldTop, DarkEmeraldMid, DarkEmeraldBottom)
                )
            )
            .onGloballyPositioned { coords ->
                rootOriginInRoot = coords.positionInRoot()
                screenWidthPx = coords.size.width.toFloat()
            }
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

            Spacer(modifier = Modifier.height(44.dp))

            // 4. VERTICAL STACK OF LANGUAGE PILL BUTTONS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                languages.forEachIndexed { i, option ->
                    val isSelected = (selectedIndex == i) || (selectedIndex == null && option.code == "en")
                    val isUnselectedDuringTransition = selectedIndex != null && selectedIndex != i

                    // Alternate rolling direction: first unselected rolls Right (+1f), second rolls Left (-1f)
                    val rollDirection = if (isUnselectedDuringTransition) {
                        val orderIdx = unselectedIndices.indexOf(i)
                        if (orderIdx == 0) 1f else -1f
                    } else 0f

                    val currentWidth = if (isUnselectedDuringTransition) {
                        lerp(pillCircleDiameter, pillFullWidth, otherWidthFraction.value)
                    } else {
                        pillFullWidth
                    }

                    val textBlurProg = if (isUnselectedDuringTransition) otherTextBlurProgress.value else 0f
                    val circleShadowAlpha = if (isUnselectedDuringTransition) otherShadowAlpha.value else 0f
                    val currentRoll = if (isUnselectedDuringTransition) rollProgress.value else 0f

                    val screenWidth = if (screenWidthPx > 0f) screenWidthPx else with(density) { 400.dp.toPx() }
                    val rollTranslationX = if (isUnselectedDuringTransition) {
                        rollDirection * currentRoll * (screenWidth * 0.72f + with(density) { 120.dp.toPx() })
                    } else 0f
                    val rollRotationZ = if (isUnselectedDuringTransition) {
                        rollDirection * currentRoll * 720f
                    } else 0f
                    val rollFadeAlpha = if (isUnselectedDuringTransition && currentRoll > 0.72f) {
                        (1f - ((currentRoll - 0.72f) / 0.28f)).coerceIn(0f, 1f)
                    } else 1f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                buttonCenterXs[i] = pos.x + coords.size.width / 2f
                                buttonCenterYs[i] = pos.y + coords.size.height / 2f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Trim-Path trailing speed lines behind rolling circle
                        if (isUnselectedDuringTransition && currentRoll > 0.02f && currentRoll < 0.99f) {
                            RollingSpeedLinesOverlay(
                                direction = rollDirection,
                                rollProgress = currentRoll,
                                circleTranslationX = rollTranslationX,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Subtle elliptical ground shadow while circle is squeezing & starting to roll
                        if (isUnselectedDuringTransition && circleShadowAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .offset(y = 22.dp)
                                    .graphicsLayer {
                                        translationX = rollTranslationX
                                        alpha = circleShadowAlpha * rollFadeAlpha
                                    }
                                    .width(34.dp)
                                    .height(5.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.55f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }

                        LanguagePillButton(
                            option = option,
                            isSelected = isSelected,
                            pillWidth = currentWidth,
                            textBlurProgress = textBlurProg,
                            rollBlurDp = if (isUnselectedDuringTransition) (currentRoll * 2.5f).dp else 0.dp,
                            enabled = !isTransitioning,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = rollTranslationX
                                    rotationZ = rollRotationZ
                                    alpha = rollFadeAlpha
                                    if (isUnselectedDuringTransition && currentRoll > 0f) {
                                        scaleX = 1f + currentRoll * 0.08f
                                        scaleY = 1f - currentRoll * 0.03f
                                    }
                                },
                            onClick = {
                                handleLanguageTap(i, option.code)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 5. FOOTER: Version and Powered By
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
 * Draws crisp Trim-Path aerodynamic speed lines trailing opposite to the rolling circle's direction.
 */
@Composable
private fun RollingSpeedLinesOverlay(
    direction: Float,
    rollProgress: Float,
    circleTranslationX: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f + circleTranslationX
        val centerY = size.height / 2f
        val radius = 22.dp.toPx()

        val lineFade = when {
            rollProgress < 0.14f -> rollProgress / 0.14f
            rollProgress > 0.78f -> (1f - rollProgress) / 0.22f
            else -> 1f
        }.coerceIn(0f, 1f)
        if (lineFade <= 0.01f) return@Canvas

        // 5 dynamic trim-path wind streaks trailing on the back edge
        val offsetsY = listOf(-14f, -7f, 0f, 7f, 14f)
        val baseLengths = listOf(22f, 36f, 44f, 32f, 20f)
        val alphas = listOf(0.35f, 0.55f, 0.70f, 0.50f, 0.32f)

        offsetsY.forEachIndexed { idx, yDp ->
            val y = centerY + yDp.dp.toPx()
            val maxLen = baseLengths[idx].dp.toPx() * (0.45f + 0.75f * rollProgress)
            val tailEdgeX = centerX - direction * (radius + 5.dp.toPx() + (idx % 2) * 4.dp.toPx())
            val startX = tailEdgeX
            val endX = tailEdgeX - direction * maxLen

            drawLine(
                color = Color(0xFFFAF7EE).copy(alpha = alphas[idx] * lineFade),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = (1.6f - (idx % 2) * 0.4f).dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Compact Capsule Pill button:
 * - Selected: Warm Gold vertical gradient (#F3DE8E -> #D49B37) + Gold border (#FFD700) + Dark text (#0D131F)
 * - Unselected: Warm Off-White ivory vertical gradient (#FAF7EE -> #EBE5D5) + Soft Ivory border (#D6CEB8) + Dark Slate text (#141C28)
 * - Shape: Pure capsule pill (RoundedCornerShape(percent = 50))
 */
@Composable
private fun LanguagePillButton(
    option: LanguageOption,
    isSelected: Boolean,
    pillWidth: Dp = 172.dp,
    textBlurProgress: Float = 0f,
    rollBlurDp: Dp = 0.dp,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "lang_pill_press_scale"
    )

    val shape = RoundedCornerShape(percent = 50)
    val textAlpha = (1f - textBlurProgress * 1.15f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(pillWidth)
            .height(44.dp)
            .scale(pressScale)
            .let { base ->
                if (rollBlurDp > 0.2.dp) {
                    base.blur(rollBlurDp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                } else base
            }
            .clip(shape)
            .background(
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(Color(0xFFF3DE8E), Color(0xFFD49B37))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFFFAF7EE), Color(0xFFEBE5D5))
                    )
                }
            )
            .border(
                width = if (isSelected) 1.6.dp else 1.2.dp,
                color = if (isSelected) Color(0xFFFFD700) else Color(0xFFD6CEB8),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = if (isSelected) Color(0xFF0D131F) else Color(0xFF141C28)),
                onClick = onClick
            )
            .testTag("lang_pill_${option.code}"),
        contentAlignment = Alignment.Center
    ) {
        if (textAlpha > 0.01f) {
            Text(
                text = option.label,
                fontSize = if (option.code == "ur") 17.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF0D131F) else Color(0xFF141C28),
                letterSpacing = if (option.code == "en") 1.2.sp else 0.4.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .alpha(textAlpha)
                    .let { base ->
                        if (textBlurProgress > 0.05f) {
                            base.blur((textBlurProgress * 4f).dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        } else base
                    }
            )
        }
    }
}
