package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
    val title: String,
    val nativeScript: String,
    val subtext: String,
    val badgeSymbol: String
)

private val languages = listOf(
    LanguageOption(
        code = "en",
        title = "ENGLISH",
        nativeScript = "English",
        subtext = "Continue in English",
        badgeSymbol = "EN"
    ),
    LanguageOption(
        code = "hi",
        title = "हिंदी",
        nativeScript = "Hindi",
        subtext = "हिंदी में आगे बढ़ें",
        badgeSymbol = "ह"
    ),
    LanguageOption(
        code = "ur",
        title = "اردو",
        nativeScript = "Urdu",
        subtext = "اردو میں آگے بڑھیں",
        badgeSymbol = "ا"
    )
)

/**
 * One-Time Initial Language Selection Screen.
 * Displayed only on first launch BEFORE FirstTimeSetupScreen.
 *
 * Structure:
 * - Top: Squircle AzanLogoCard + CorelDRAW AzanTimeWordmark ("AZAN TIME" + "AAO ALLAH KI RAAH MEIN CHALE")
 * - Subtitle: Multilingual prompt
 * - Vertically stacked ("ek ke niche ek") large, touch-friendly language cards:
 *   1. ENGLISH
 *   2. हिंदी
 *   3. اردو
 *
 * On selection:
 * - Saves selected language
 * - Marks initial language chosen (persisted in DataStore & SharedPreferences)
 * - Proceeds immediately to FirstTimeSetupScreen rendered in that language.
 */
@Composable
fun InitialLanguageScreen(
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCode by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
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
            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(28.dp))

            // 4. VERTICAL STACK OF LANGUAGE BUTTONS ("ek ke niche ek")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                languages.forEach { option ->
                    val isCurrentSelected = selectedCode == option.code

                    LanguageVerticalButton(
                        option = option,
                        isSelected = isCurrentSelected,
                        onClick = {
                            if (selectedCode == null) {
                                selectedCode = option.code
                                coroutineScope.launch {
                                    delay(160L) // Subtle ripple/select animation
                                    onLanguageSelect(option.code)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. SUBTLE BRANDING FOOTER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(GoldAccent.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline Azan Time • Solapur",
                    fontSize = 11.sp,
                    color = Color(0xFF8BAA9B),
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(GoldAccent.copy(alpha = 0.6f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun LanguageVerticalButton(
    option: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.97f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "lang_btn_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = GoldAccent),
                onClick = onClick
            )
            .testTag("lang_button_${option.code}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isSelected) 2.2.dp else 1.3.dp,
            color = if (isSelected) GoldLight else GoldAccent.copy(alpha = 0.75f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSelected) {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF235C3F), Color(0xFF133E2B))
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF133B29), Color(0xFF0A2419))
                        )
                    }
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Badge Symbol
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) GoldAccent else Color(0xFF092015)
                            )
                            .border(
                                width = 1.2.dp,
                                color = GoldAccent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.badgeSymbol,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF081C15) else GoldAccent
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Center: Language Name and native prompt
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = option.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) GoldLight else Color(0xFFFFF9E6),
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = option.subtext,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) GoldLight.copy(alpha = 0.9f) else GoldAccent.copy(alpha = 0.8f)
                        )
                    }
                }

                // Right: Arrow / Selected Check Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) GoldAccent else Color(0xFF0A2216)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF081C15),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Select ${option.title}",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
