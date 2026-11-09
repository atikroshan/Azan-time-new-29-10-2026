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
 * Cinematic Animation Sequence ("Phele button jana hai phir baki screen aur background mai setup screen dekhni chahiye"):
 * 1. When a button is tapped (English, Hindi, or Urdu):
 *    - The unselected 2 buttons smoothly glide vertically (550ms) into the tapped button.
 *    - As they glide, they scale down and fade out with vertical motion blur.
 *    - Tapped button pulses up to 1.12x absorbing them, then settles.
 * 2. Settle Pause (160ms):
 *    - User clearly registers the single golden pill button.
 * 3. Tapped Button Leads the Exit ("Phele button jana hai"):
 *    - Tapped button accelerates and flies off to the right screen edge with horizontal motion blur.
 * 4. Screen Follows Behind ("phir baki screen"):
 *    - After a 160ms head-start for the button, the entire screen (content + dark emerald background)
 *      sweeps smoothly to the right edge with horizontal motion blur.
 * 5. Background Reveal ("background mai setup screen dekhni chahiye"):
 *    - Because FirstTimeSetupScreen is rendered directly underneath in MainActivity,
 *      sliding this screen off to the right physically uncovers FirstTimeSetupScreen right in the background!
 * 6. Finishes and transitions seamlessly into setup in the chosen language.
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // Merge animation progress for unselected buttons (0f -> 1f) - slower, visible glide
    val mergeProgress = remember { Animatable(0f) }
    val mergeAlpha = remember { Animatable(1f) }
    val mergeScale = remember { Animatable(1f) }

    // Tapped button absorption pulse scale
    val tappedPulseScale = remember { Animatable(1f) }

    // Exit animation: Button exits first, then screen follows
    val buttonExitX = remember { Animatable(0f) }
    val screenExitX = remember { Animatable(0f) }

    // Spacing calculations: button height 44dp + gap 14dp = 58dp center-to-center
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val buttonStepPx = with(density) { 58.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    val otherButtonBlurY = (mergeProgress.value * 16f).dp
    val selectedButtonBlurX = ((buttonExitX.value / (screenWidthPx * 1.35f)) * 20f).coerceIn(0f, 20f).dp
    val screenBlurX = ((screenExitX.value / screenWidthPx) * 14f).coerceIn(0f, 14f).dp

    fun handleLanguageTap(index: Int, code: String) {
        if (isTransitioning) return
        isTransitioning = true
        selectedIndex = index

        coroutineScope.launch {
            // PHASE 1: SLOW SMOOTH MERGE (550ms) - unselected buttons glide smoothly into tapped button
            launch {
                mergeProgress.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
            }
            launch {
                mergeAlpha.animateTo(0f, tween(480, delayMillis = 60, easing = FastOutLinearInEasing))
            }
            launch {
                mergeScale.animateTo(0.35f, tween(550, easing = FastOutSlowInEasing))
            }
            // Tapped button expands slightly to absorb them, then settles
            launch {
                tappedPulseScale.animateTo(1.12f, tween(280, easing = FastOutSlowInEasing))
                tappedPulseScale.animateTo(1.0f, tween(270, easing = FastOutSlowInEasing))
            }

            delay(560L) // Wait for merge animation to complete
            delay(160L) // Settle pause so user clearly registers the single merged button!

            // PHASE 2: TAPPED BUTTON EXITS FIRST TO THE RIGHT ("Phele button jana hai")
            launch {
                buttonExitX.animateTo(
                    screenWidthPx * 1.35f,
                    tween(420, easing = FastOutSlowInEasing)
                )
            }

            // PHASE 3: SCREEN FOLLOWS BEHIND ("phir baki screen aur background mai setup screen dekhni chahiye")
            delay(160L) // Give the button a visible head start!

            launch {
                screenExitX.animateTo(
                    screenWidthPx * 1.05f,
                    tween(500, easing = FastOutSlowInEasing)
                )
            }

            delay(520L) // Wait for screen slide to finish

            // PHASE 4: Navigate directly to setup screen in selected language
            onLanguageSelect(code)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.translationX = screenExitX.value
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
