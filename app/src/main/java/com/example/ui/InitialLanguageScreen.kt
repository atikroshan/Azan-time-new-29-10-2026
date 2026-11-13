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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
 * - Selected button: Warm Gold gradient + dark text
 * - Unselected buttons: Off-White ivory gradient + dark charcoal text
 *
 * Choreography on tapping any language button:
 * 1. The 2 UNSELECTED off-white buttons run the elastic squash/lift + pill-to-circle squeeze with text blur.
 * 2. Both unselected circles roll slowly RIGHT & LEFT off-screen with horizontal motion blur.
 * 3. Wait 2 seconds with only the selected button visible.
 * 4. Selected button glides smoothly straight down off the bottom edge (moderate pace, no bounce/parachute),
 *    while GenieFoldOverlay with fabric fold follows it downward to reveal FirstTimeSetupScreen.
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLanguagePreselected: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // GraphicsLayer to capture screenshot (without the selected button) for the Genie fabric fold
    val pageGraphicsLayer = rememberGraphicsLayer()
    var outgoingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFloatingSelected by remember { mutableStateOf(false) }

    // Phase 1 & 2: Smooth pill-to-circle squeeze + Right/Left roll-out with trim-path speed lines (no bounce)
    val otherShadowAlpha = remember { Animatable(0f) }
    val otherWidthFraction = remember { Animatable(1f) } // 1f (172.dp pill) -> 0f (44.dp circle)
    val otherTextBlurProgress = remember { Animatable(0f) }
    val otherRollProgress = remember { Animatable(0f) } // 0f -> 1f roll Right & Left with trim-path speed lines

    // Phase 4: Slow smooth downward glide + Genie fabric fold tracking the descending selected button
    val exitProgress = remember { Animatable(0f) }

    // Screen and button positioning measurements
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val fallbackScreenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val fallbackScreenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val buttonWidthPx = with(density) { 172.dp.toPx() }
    val buttonHeightPx = with(density) { 44.dp.toPx() }

    var rootOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var rootWidthPx by remember { mutableFloatStateOf(0f) }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }

    val containerWidthPx = if (rootWidthPx > 0f) rootWidthPx else fallbackScreenWidthPx
    val containerHeightPx = if (rootHeightPx > 0f) rootHeightPx else fallbackScreenHeightPx

    val buttonRootXs = remember { mutableStateMapOf<Int, Float>() }
    val buttonRootYs = remember { mutableStateMapOf<Int, Float>() }

    val startLeftLocal = remember(selectedIndex, buttonRootXs.size, rootOriginInRoot, containerWidthPx) {
        val rawX = selectedIndex?.let { buttonRootXs[it] }
        if (rawX != null) rawX - rootOriginInRoot.x else (containerWidthPx - buttonWidthPx) / 2f
    }
    val startTopLocal = remember(selectedIndex, buttonRootYs.size, rootOriginInRoot, containerHeightPx) {
        val rawY = selectedIndex?.let { buttonRootYs[it] }
        if (rawY != null) {
            rawY - rootOriginInRoot.y
        } else {
            val fraction = when (selectedIndex) {
                0 -> 0.48f
                1 -> 0.56f
                else -> 0.64f
            }
            (containerHeightPx * fraction) - (buttonHeightPx / 2f)
        }
    }

    val startCenterX = startLeftLocal + (buttonWidthPx / 2f)
    val startCenterY = startTopLocal + (buttonHeightPx / 2f)

    // Smooth moderate-speed vertical descent (no bounce, no parachute sway/tilt)
    val u = exitProgress.value.coerceIn(0f, 1f)
    val dropCurve = genieSS(u) // smooth ease-in-out matching Genie fold cadence
    val totalDropPx = (containerHeightPx - startCenterY) + buttonHeightPx * 2.0f
    val buttonDropY = totalDropPx * dropCurve
    val buttonBlurY: Dp = (((u - 0.35f) / 0.65f).coerceIn(0f, 1f) * 6f).dp
    val buttonExitAlpha = 1f - ((u - 0.86f) / 0.14f).coerceIn(0f, 1f)

    val currentCenterX = startCenterX
    val currentCenterY = startCenterY + buttonDropY

    fun handleLanguageTap(index: Int, code: String) {
        if (isTransitioning) return
        isTransitioning = true
        selectedIndex = index
        // Pre-localize FirstTimeSetupScreen underneath so the revealed screen already matches
        onLanguagePreselected(code)

        coroutineScope.launch {
            // STEP 1: Unselected 2 off-white pills squeeze smoothly into 44.dp circles (NO bounce)
            launch {
                otherShadowAlpha.animateTo(0.36f, tween(220, easing = FastOutSlowInEasing))
            }
            launch {
                otherTextBlurProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            }
            otherWidthFraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing)
            )

            // STEP 2: Both off-white circles roll Right & Left off-screen with Trim-Path speed lines
            launch {
                otherShadowAlpha.animateTo(0f, tween(360, easing = FastOutLinearInEasing))
            }
            otherRollProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing)
            )

            // STEP 3: 2 second wait with only the selected button visible
            delay(2000L)

            // STEP 4: Swap selected button to floating layer cleanly, capture screen without it,
            // then glide selected button + cloth fabric fold slowly downward off-screen.
            showFloatingSelected = true
            withFrameNanos { }

            val snapshot = try {
                pageGraphicsLayer.toImageBitmap().asAndroidBitmap()
            } catch (_: Exception) {
                null
            }
            outgoingBitmap = snapshot

            exitProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2300, easing = LinearEasing)
            )

            delay(40L)

            onLanguageSelect(code)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootOriginInRoot = coords.positionInRoot()
                rootWidthPx = coords.size.width.toFloat()
                rootHeightPx = coords.size.height.toFloat()
            }
            .testTag("initial_language_screen"),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = outgoingBitmap
        if (currentBitmap != null && u > 0f) {
            // Layer A: 56-strip Cloth Genie Fold with fabric pleat shading following the descending button
            GenieFoldOverlay(
                bitmap = currentBitmap,
                progress = u,
                target = TabTarget(
                    centerX = currentCenterX,
                    centerY = currentCenterY,
                    width = buttonWidthPx,
                    height = buttonHeightPx
                ),
                containerWidth = containerWidthPx,
                containerHeight = containerHeightPx,
                fabricFold = true,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val fallbackAlpha = if (u > 0f && outgoingBitmap == null) {
                (1f - u).coerceIn(0f, 1f)
            } else 1f
            val fallbackScale = if (u > 0f && outgoingBitmap == null) {
                (1f - 0.95f * u).coerceAtLeast(0.04f)
            } else 1f
            val fallbackPivotY = (currentCenterY / containerHeightPx).coerceIn(0.2f, 0.95f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = fallbackAlpha
                        this.scaleX = fallbackScale
                        this.scaleY = fallbackScale
                        this.transformOrigin = TransformOrigin(0.5f, fallbackPivotY)
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
                    ),
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
                            val isTapped = selectedIndex == i
                            val isOther = selectedIndex != null && !isTapped
                            val isInitiallySelected = selectedIndex == null && option.code == "en"

                            val otherDirection = if (isOther) {
                                val sel = selectedIndex!!
                                val otherOrder = if (i < sel) i else i - 1
                                // 1st unselected circle rolls RIGHT (+1f), 2nd unselected circle rolls LEFT (-1f)
                                if (otherOrder == 0) 1f else -1f
                            } else 0f

                            val transX = if (isOther) {
                                otherDirection * containerWidthPx * 1.15f * otherRollProgress.value
                            } else 0f

                            val rollRotationZ = if (isOther) {
                                otherDirection * 720f * otherRollProgress.value
                            } else 0f

                            val rollBlurX: Dp = if (isOther) {
                                (otherRollProgress.value * 10f).dp
                            } else 0.dp

                            val alphaVal = when {
                                isTapped && showFloatingSelected -> 0f
                                isOther -> {
                                    val rollFade = ((otherRollProgress.value - 0.65f) / 0.35f).coerceIn(0f, 1f)
                                    1f - rollFade
                                }
                                else -> 1f
                            }

                            val pillWidthDp: Dp = if (isOther) {
                                44.dp + (128.dp * otherWidthFraction.value)
                            } else {
                                172.dp
                            }

                            val textMotionBlurX: Dp = if (isOther) {
                                (otherTextBlurProgress.value * 8f).dp
                            } else 0.dp

                            val textAlphaVal: Float = if (isOther) {
                                (1f - ((1f - otherWidthFraction.value) * 1.45f)).coerceIn(0f, 1f)
                            } else 1f

                            val textScaleXVal: Float = if (isOther) {
                                (0.35f + 0.65f * otherWidthFraction.value).coerceIn(0.35f, 1f)
                            } else 1f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isTapped) 2f else 1f),
                                contentAlignment = Alignment.Center
                            ) {
                                // Trim-Path speed lines trailing behind rolling circles (Right & Left)
                                val r = otherRollProgress.value
                                if (isOther && r > 0.005f && r < 0.995f) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                    ) {
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val circleRadiusPx = 22.dp.toPx()
                                        val circleCenterX = centerX + transX
                                        val backEdgeX = circleCenterX - otherDirection * (circleRadiusPx + 6.dp.toPx())
                                        val trackStartX = centerX - otherDirection * 12.dp.toPx()
                                        val trackEndX = centerX + otherDirection * (containerWidthPx * 1.05f)

                                        val yOffsetsDp = floatArrayOf(-15f, -6f, 1f, 9f, 16f)
                                        val strokesDp = floatArrayOf(2.0f, 2.8f, 1.8f, 2.5f, 1.6f)
                                        val staggers = floatArrayOf(0.00f, 0.05f, 0.02f, 0.08f, 0.04f)
                                        val speedMultipliers = floatArrayOf(1.0f, 1.12f, 0.94f, 1.06f, 0.88f)
                                        val isGoldLine = booleanArrayOf(false, true, false, true, false)

                                        for (idx in 0 until 5) {
                                            val localT = ((r - staggers[idx]) / (1f - staggers[idx])).coerceIn(0f, 1f)
                                            if (localT <= 0.01f || localT >= 0.99f) continue

                                            val trimEnd = (1f - (1f - localT) * (1f - localT)) * speedMultipliers[idx]
                                            val trimStart = (localT * localT * 0.95f) * speedMultipliers[idx]

                                            val rawHeadX = trackStartX + (trackEndX - trackStartX) * trimEnd.coerceIn(0f, 1f)
                                            val tailX = trackStartX + (trackEndX - trackStartX) * trimStart.coerceIn(0f, 1f)

                                            val headX = if (otherDirection > 0f) {
                                                minOf(rawHeadX, backEdgeX)
                                            } else {
                                                maxOf(rawHeadX, backEdgeX)
                                            }

                                            val validSpan = if (otherDirection > 0f) {
                                                headX > tailX + 4f
                                            } else {
                                                headX < tailX - 4f
                                            }
                                            if (!validSpan) continue

                                            val yPos = centerY + yOffsetsDp[idx].dp.toPx()
                                            val fadeOut = (1f - ((localT - 0.65f) / 0.35f).coerceIn(0f, 1f))
                                            val fadeIn = (localT / 0.12f).coerceIn(0f, 1f)
                                            val lineAlpha = fadeIn * fadeOut * 0.90f
                                            val baseColor = if (isGoldLine[idx]) {
                                                Color(0xFFF3DE8E)
                                            } else {
                                                Color(0xFFFAF7EE)
                                            }

                                            drawLine(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        baseColor.copy(alpha = lineAlpha * 0.65f),
                                                        baseColor.copy(alpha = lineAlpha)
                                                    ),
                                                    start = Offset(tailX, yPos),
                                                    end = Offset(headX, yPos)
                                                ),
                                                start = Offset(tailX, yPos),
                                                end = Offset(headX, yPos),
                                                strokeWidth = strokesDp[idx].dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }

                                // Subtle thin elliptical ground shadow underneath the 2 unselected pills during Step 1
                                if (isOther && otherShadowAlpha.value > 0.01f) {
                                    val shadowWidthDp = (pillWidthDp * 0.82f).coerceAtLeast(32.dp)
                                    Canvas(
                                        modifier = Modifier
                                            .offset(y = 29.dp)
                                            .width(shadowWidthDp)
                                            .height(6.dp)
                                            .alpha(otherShadowAlpha.value)
                                    ) {
                                        drawOval(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.75f),
                                                    Color.Black.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(size.width / 2f, size.height / 2f),
                                                radius = (size.width / 2f).coerceAtLeast(1f)
                                            ),
                                            topLeft = Offset.Zero,
                                            size = Size(size.width, size.height)
                                        )
                                    }
                                }

                                LanguagePillButton(
                                    option = option,
                                    isSelected = isTapped || isInitiallySelected,
                                    pillWidth = pillWidthDp,
                                    textBlurX = textMotionBlurX,
                                    textAlpha = textAlphaVal,
                                    textScaleX = textScaleXVal,
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            val pos = coords.positionInRoot()
                                            buttonRootXs[i] = pos.x
                                            buttonRootYs[i] = pos.y
                                        }
                                        .graphicsLayer {
                                            this.translationX = transX
                                            this.rotationZ = rollRotationZ
                                            this.alpha = alphaVal
                                        }
                                        .blur(
                                            radiusX = rollBlurX,
                                            radiusY = 0.dp,
                                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                                        ),
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

        // Layer B: Floating selected button gliding smoothly straight down off-screen with the Genie fabric fold
        val tappedIdx = selectedIndex
        if (showFloatingSelected && tappedIdx != null && buttonExitAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                contentAlignment = Alignment.TopStart
            ) {
                LanguagePillButton(
                    option = languages[tappedIdx],
                    isSelected = true,
                    pillWidth = 172.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            this.translationX = startLeftLocal
                            this.translationY = startTopLocal + buttonDropY
                            this.alpha = buttonExitAlpha
                        }
                        .blur(
                            radiusX = 0.dp,
                            radiusY = buttonBlurY,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        ),
                    onClick = {}
                )
            }
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
    textBlurX: Dp = 0.dp,
    textAlpha: Float = 1f,
    textScaleX: Float = 1f,
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
            .width(pillWidth)
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
                interactionSource = interactionSource,
                indication = ripple(color = if (isSelected) Color(0xFF0D131F) else Color(0xFF141C28)),
                onClick = onClick
            )
            .testTag("lang_pill_${option.code}"),
        contentAlignment = Alignment.Center
    ) {
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
                .graphicsLayer {
                    this.scaleX = textScaleX
                    this.alpha = textAlpha
                }
                .blur(radiusX = textBlurX, radiusY = 0.dp)
        )
    }
}
