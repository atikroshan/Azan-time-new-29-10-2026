package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 * Direct selection without delay or exit transition, as requested.
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLanguagePreselected: (String) -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    fun handleLanguageTap(index: Int, code: String) {
        selectedIndex = index
        onLanguagePreselected(code)
        onLanguageSelect(code)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkEmeraldTop, DarkEmeraldMid, DarkEmeraldBottom)
                )
            )
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

                    LanguagePillButton(
                        option = option,
                        isSelected = isSelected,
                        pillWidth = 172.dp,
                        onClick = {
                            handleLanguageTap(i, option.code)
                        }
                    )
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
            overflow = TextOverflow.Clip
        )
    }
}
