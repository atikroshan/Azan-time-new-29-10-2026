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
import androidx.compose.ui.graphics.graphicsLayer
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
 * Displayed on first launch BEFORE FirstTimeSetupScreen.
 *
 * Cinematic Animation:
 * 1. When a button is tapped (e.g. English):
 *    - The other 2 buttons slide directly into the tapped button with vertical motion blur and disappear (merge).
 * 2. Once merged:
 *    - The tapped button shoots off to the right screen with horizontal motion blur.
 *    - The entire screen follows right behind the button to the right.
 * 3. Immediately transitions into FirstTimeSetupScreen in the chosen language.
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // Merge animation progress for the unselected buttons (0f -> 1f)
    val mergeProgress = remember { Animatable(0f) }
    val mergeAlpha = remember { Animatable(1f) }
    val mergeScale = remember { Animatable(1f) }

    // Exit animation for tapped button and entire screen
    val buttonExitX = remember { Animatable(0f) }
    val screenExitX = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    // Spacing calculations: button height 44dp + gap 14dp = 58dp center-to-center
    val density = LocalDensity.current
    val buttonStepPx = with(density) { 58.dp.toPx() }

    val otherButtonBlurY = (mergeProgress.value * 16f).dp
    val selectedButtonBlurX = ((buttonExitX.value / 1800f) * 26f).coerceIn(0f, 26f).dp
    val screenBlurX = ((screenExitX.value / 2200f) * 16f).coerceIn(0f, 16f).dp

    fun handleLanguageTap(index: Int, code: String) {
        if (isTransitioning) return
        isTransitioning = true
        selectedIndex = index

        coroutineScope.launch {
            // PHASE 1: MERGE (260ms) - unselected buttons slide into tapped button, shrinking & blurring
            launch {
                mergeProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            }
            launch {
                mergeAlpha.animateTo(0f, tween(240, easing = FastOutLinearInEasing))
            }
            launch {
                mergeScale.animateTo(0.6f, tween(260, easing = FastOutSlowInEasing))
            }

            delay(270L) // Wait for merge into the tapped button

            // PHASE 2: LAUNCH RIGHT (340ms) - tapped button shoots right, screen follows 50ms behind
            launch {
                buttonExitX.animateTo(1800f, tween(340, easing = FastOutLinearInEasing))
            }

            delay(50L) // Screen chases behind the button
            launch {
                screenExitX.animateTo(2200f, tween(320, easing = FastOutLinearInEasing))
            }
            launch {
                screenAlpha.animateTo(0f, tween(320))
            }

            delay(330L)

            // PHASE 3: Navigate directly to setup screen in selected language
            onLanguageSelect(code)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.translationX = screenExitX.value
                this.alpha = screenAlpha.value
            }
            .blur(radiusX = screenBlurX, radiusY = 0.dp)
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

            // Moved lower down as requested ("Language button thoda niche loa")
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
                        isTapped -> 1.05f
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

            // 5. FOOTER: Version and Powered By as requested ("last line nikal ke version aur powered by line dalo")
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
