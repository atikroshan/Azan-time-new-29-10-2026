package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.nativeCanvas
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.LanguageCirclesRow
import com.example.R
import com.example.appBackground
import com.example.data.GoogleSheetMasjidSync
import com.example.data.MasjidItem
import com.example.data.displayId
import com.example.data.getLocalizedName
import com.example.data.getLocalizedFullLocation
import com.example.islamicStarBackground
import com.example.ui.theme.LocalAppStrings
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SupportScreen(
    uiState: UIState,
    onBack: () -> Unit,
    onLangSelect: (String) -> Unit = {},
    onMasjidSubmitted: () -> Unit = {},
    onChangeMasjid: () -> Unit = {},
    onSelectMasjid: (MasjidItem) -> Unit = {},
    onOpenAdminLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFrontScreen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        SupportBackScreen(
            uiState = uiState,
            onBack = onBack,
            onLangSelect = onLangSelect,
            onMasjidSubmitted = onMasjidSubmitted,
            onChangeMasjid = onChangeMasjid,
            onSelectMasjid = onSelectMasjid,
            onOpenAdminLogin = onOpenAdminLogin,
            onOpenFrontScreen = { showFrontScreen = true },
            modifier = Modifier
                .fillMaxSize()
                .blur(if (showFrontScreen) 8.dp else 0.dp)
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = showFrontScreen,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { showFrontScreen = false }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showFrontScreen,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val sheetOffsetY = remember { Animatable(0f) }
            val coroutineScope = rememberCoroutineScope()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.77f)
                    .offset(y = (-6).dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .offset { IntOffset(0, sheetOffsetY.value.toInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (sheetOffsetY.value > 120f) {
                                    showFrontScreen = false
                                } else {
                                    coroutineScope.launch {
                                        sheetOffsetY.animateTo(0f, animationSpec = tween(200))
                                    }
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = (sheetOffsetY.value + dragAmount).coerceAtLeast(0f)
                                    sheetOffsetY.snapTo(newOffset)
                                }
                            }
                        )
                    },
                color = Color(0xFF040E0A),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showFrontScreen = false }
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(5.dp)
                                .background(Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }

                    SupportFrontScreen(
                        uiState = uiState,
                        onClose = { showFrontScreen = false },
                        onLangSelect = onLangSelect,
                        onMasjidSubmitted = onMasjidSubmitted,
                        onChangeMasjid = onChangeMasjid,
                        onSelectMasjid = onSelectMasjid,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private val MinarDomeShape = object : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val w = size.width
        val h = size.height
        val cH = (w * 0.44324f).coerceAtMost(h * 0.42f)
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, cH)
            cubicTo(-0.0004f * w, 0.9866f * cH, -0.0031f * w, 0.8497f * cH, 0.0900f * w, 0.8238f * cH)
            lineTo(0.0914f * w, 0.8234f * cH)
            lineTo(0.0913f * w, 0.8209f * cH)
            cubicTo(0.0913f * w, 0.8204f * cH, 0.0907f * w, 0.7791f * cH, 0.1074f * w, 0.7483f * cH)
            cubicTo(0.1196f * w, 0.7256f * cH, 0.1380f * w, 0.7141f * cH, 0.1620f * w, 0.7141f * cH)
            lineTo(0.1643f * w, 0.7141f * cH)
            lineTo(0.1635f * w, 0.7102f * cH)
            cubicTo(0.1632f * w, 0.7088f * cH, 0.1330f * w, 0.5653f * cH, 0.1722f * w, 0.4134f * cH)
            cubicTo(0.2038f * w, 0.2909f * cH, 0.2705f * w, 0.1970f * cH, 0.3705f * w, 0.1343f * cH)
            cubicTo(0.3713f * w, 0.1337f * cH, 0.4469f * w, 0.0816f * cH, 0.4876f * w, 0.0091f * cH)
            cubicTo(0.4944f * w, -0.0030f * cH, 0.5055f * w, -0.0030f * cH, 0.5123f * w, 0.0091f * cH)
            cubicTo(0.5531f * w, 0.0816f * cH, 0.6287f * w, 0.1337f * cH, 0.6295f * w, 0.1343f * cH)
            cubicTo(0.7295f * w, 0.1970f * cH, 0.7962f * w, 0.2909f * cH, 0.8277f * w, 0.4134f * cH)
            cubicTo(0.8669f * w, 0.5653f * cH, 0.8368f * w, 0.7088f * cH, 0.8365f * w, 0.7102f * cH)
            lineTo(0.8357f * w, 0.7141f * cH)
            lineTo(0.8380f * w, 0.7141f * cH)
            cubicTo(0.8620f * w, 0.7141f * cH, 0.8804f * w, 0.7256f * cH, 0.8926f * w, 0.7483f * cH)
            cubicTo(0.9092f * w, 0.7791f * cH, 0.9086f * w, 0.8204f * cH, 0.9086f * w, 0.8209f * cH)
            lineTo(0.9086f * w, 0.8234f * cH)
            lineTo(0.9100f * w, 0.8238f * cH)
            cubicTo(1.0031f * w, 0.8497f * cH, 1.0004f * w, 0.9866f * cH, w, cH)
            lineTo(w, h)
            close()
        }
        return Outline.Generic(path)
    }
}

private fun Modifier.cardIslamicStarOverlay(alpha: Float = 0.055f): Modifier = this.drawBehind {
    val step = 42.dp.toPx()
    val patternColor = Color(0xFFF3DE8E).copy(alpha = alpha)
    val r = 12.dp.toPx()
    val rIn = r * 0.52f
    var y = 0f
    while (y < size.height + step) {
        var x = 0f
        while (x < size.width + step) {
            val starPath = Path()
            for (i in 0 until 16) {
                val angle = (i * Math.PI / 8.0).toFloat()
                val radius = if (i % 2 == 0) r else rIn
                val px = x + radius * kotlin.math.cos(angle)
                val py = y + radius * kotlin.math.sin(angle)
                if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
            }
            starPath.close()
            drawPath(path = starPath, color = patternColor, style = Stroke(width = 1.dp.toPx()))
            x += step
        }
        y += step
    }
}

@Composable
fun SupportBackScreen(
    uiState: UIState,
    onBack: () -> Unit,
    onLangSelect: (String) -> Unit = {},
    onMasjidSubmitted: () -> Unit = {},
    onChangeMasjid: () -> Unit = {},
    onSelectMasjid: (MasjidItem) -> Unit = {},
    onOpenFrontScreen: () -> Unit = {},
    onOpenAdminLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showThanksDialog by remember { mutableStateOf(false) }
    var showAddMasjidDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val appStrings = LocalAppStrings.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF040E0A), Color(0xFF06140D), Color(0xFF040C08), Color(0xFF020704))))
    ) {
        val screenHeight = maxHeight
        val isCompact = screenHeight < 680.dp
        val isUltraCompact = screenHeight < 580.dp
        val itemSpacing = if (isCompact) 5.dp else 8.dp
        val logoSize = if (isUltraCompact) 46.dp else if (isCompact) 52.dp else 64.dp
        val wordmarkHeight = if (isUltraCompact) 18.dp else if (isCompact) 22.dp else 26.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 6.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LanguageCirclesRow(currentLang = uiState.language, onLangSelect = onLangSelect)
                Box(modifier = Modifier.size(38.dp).background(color = Color(0xFFF3DE8E), shape = RoundedCornerShape(10.dp)).clickable { onOpenAdminLogin() }, contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = "Admin Login", tint = Color(0xFF040E0A), modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AzanLogoCard(size = logoSize)
                Spacer(modifier = Modifier.height(itemSpacing))
                AzanTimeWordmark(height = wordmarkHeight, topPadding = 2.dp, bottomPadding = 2.dp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            val pagerState = rememberPagerState(initialPage = 300, pageCount = { 1000 })
            val settingCards = listOf(
                Triple(when (uiState.language) { "ur" -> "اپنی مسجد"; "hi" -> "अपनी मस्जिद"; else -> "CURRENT MASJID" }, "", Icons.Outlined.Mosque),
                Triple(when (uiState.language) { "ur" -> "نئی مسجد"; "hi" -> "नई मस्जिद"; else -> "ADD NEW MASJID" }, "", Icons.Outlined.AddLocationAlt),
                Triple(when (uiState.language) { "ur" -> "ہماری حمایت کریں"; "hi" -> "ہمارا समर्थन करें"; else -> "SUPPORT US" }, when (uiState.language) { "ur" -> "فیڈبک کے ساتھ ایپ کو سپورٹ کریں، 5 اسٹار ریٹنگ دیں"; "hi" -> "फीडबैक کے ساتھ ایپ کو سپورٹ کریں, 5 स्टार रेटिंग दें"; else -> "Support our app with feedback and 5-star rating" }, Icons.Outlined.HeadsetMic)
            )
            val actionTexts = listOf(when (uiState.language) { "ur" -> "تبدیل"; "hi" -> "बदलें"; else -> "CHANGE" }, when (uiState.language) { "ur" -> "درخواست"; "hi" -> "जोड़ें"; else -> "ADD" }, when (uiState.language) { "ur" -> "کھولیں"; "hi" -> "खोलें"; else -> "OPEN" })
            val actions = listOf({ onChangeMasjid() }, { showAddMasjidDialog = true }, { onOpenFrontScreen() })

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                // Bottom edge transparency fade (covers only the bottom 24dp so cards blend smoothly without dimming the button)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            val fadeHeight = 24.dp.toPx()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF040E0A).copy(alpha = 0.72f),
                                        Color(0xFF040E0A)
                                    ),
                                    startY = (size.height - fadeHeight).coerceAtLeast(0f),
                                    endY = size.height
                                )
                            )
                        }
                ) {
                    HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 70.dp), pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth().fillMaxHeight()) { page ->
                        val realIndex = page % 3
                        val (title, subtitle, icon) = settingCards[realIndex]
                        val cardAnimOffsetY = remember { Animatable(0f) }

                        val distance = kotlin.math.abs(pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                        val isSelected = (distance < 0.1f)
                        
                        fun handleCardClick() {
                            if (isSelected) {
                                coroutineScope.launch {
                                    cardAnimOffsetY.animateTo(-40f, tween(130))
                                    cardAnimOffsetY.animateTo(2500f, tween(350))
                                    actions[realIndex]()
                                    delay(600L)
                                    cardAnimOffsetY.snapTo(0f)
                                }
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(page) }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { 
                                    scaleX = 1f - 0.12f * distance.coerceIn(0f, 1f)
                                    scaleY = 1f - 0.15f * distance.coerceIn(0f, 1f)
                                    translationY = cardAnimOffsetY.value + (distance * 35.dp.toPx()).coerceIn(0f, 35.dp.toPx())
                                }
                                .blur(if (distance < 0.05f) 0.dp else (distance * 6f).dp)
                                .alpha(1f - 0.25f * distance.coerceIn(0f, 1f))
                                .clip(MinarDomeShape)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleCardClick() },
                            shape = MinarDomeShape,
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color.Transparent else Color(0xFF0D281C)),
                            border = BorderStroke(width = if (isSelected) 2.dp else 1.2.dp, color = Color(0xFFE5B842).copy(alpha = if(isSelected) 1f else 0.4f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B4E38), Color(0xFF071D13)))).cardIslamicStarOverlay(alpha = 0.04f)) {
                                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Box(modifier = Modifier.size(if (isSelected) 40.dp else 32.dp).background(Color(0xFFF8F9FA), CircleShape), contentAlignment = Alignment.Center) { 
                                        Icon(imageVector = icon, contentDescription = title, tint = Color.Black, modifier = Modifier.size(if (isSelected) 21.dp else 17.dp)) 
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(color = Color(0xFFF3DE8E).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.3f))) { 
                                        Text(text = title, fontSize = if (isSelected) 12.sp else 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF3DE8E), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) 
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF020704)), border = BorderStroke(1.2.dp, Color(0xFFE5B842).copy(alpha = 0.35f))) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            when (realIndex) {
                                                0 -> {
                                                    // Card 0: Change Masjid (Blank container matching Add New Masjid layout, ready for custom animation)
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(if (isSelected) 370.dp else 240.dp)
                                                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                                            .background(Color(0xFF0C1424))
                                                    )
                                                }
                                                1 -> {
                                                    // Card 1: Add New Masjid (Animated Form Filling & Upward Scrolling Preview)
                                                    AnimatedAddMasjidFormPreview(isSelected = isSelected)
                                                }
                                                2 -> {
                                                    // Card 2: Support Us
                                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                        Icon(imageVector = Icons.Outlined.HeadsetMic, contentDescription = null, tint = Color(0xFFF3DE8E), modifier = Modifier.size(36.dp))
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Text(text = subtitle, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 14.sp)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(5) { Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF3DE8E), modifier = Modifier.size(15.dp)) } }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val instr = when (realIndex) { 
                                            0 -> appStrings.changeMasjidInstruction
                                            1 -> "Note: Please use the Add button exclusively to submit the form for the responsible person (Moulana / Mutawalli / Bangi Sahab / All the Trust Member)."
                                            else -> "" 
                                        }
                                        if (instr.isNotEmpty()) { 
                                            Text(
                                                text = instr, 
                                                fontSize = 10.sp, 
                                                color = Color.White, 
                                                textAlign = TextAlign.Center, 
                                                lineHeight = 13.sp, 
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp)) 
                                        }
                                        val activeLangBrush = Brush.verticalGradient(listOf(Color(0xFFF3DE8E), Color(0xFFD49B37)))
                                        Box(
                                            modifier = Modifier
                                                .testTag("setting_action_button_$realIndex")
                                                .width(130.dp)
                                                .height(29.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(brush = activeLangBrush)
                                                .clickable { actions[realIndex]() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = actionTexts[realIndex], 
                                                fontSize = 12.sp, 
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF040E0A)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(26.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMasjidDialog) {
        AddMasjidAutoDialog(
            language = uiState.language,
            onDismiss = { showAddMasjidDialog = false },
            onSubmitted = {
                showAddMasjidDialog = false
                showThanksDialog = true
            }
        )
    }

    if (showThanksDialog) {
        Dialog(onDismissRequest = { showThanksDialog = false; onMasjidSubmitted() }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Card(modifier = Modifier.fillMaxWidth(0.90f), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(modifier = Modifier.size(68.dp).background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape).border(2.dp, Color(0xFF10B981), CircleShape), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(34.dp)) }
                        Text(text = when (uiState.language) { "ur" -> "درخواست موصول ہوگئی!"; "hi" -> "अनुरोध प्राप्त हुआ!"; else -> "Request Received!" }, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF34D399), textAlign = TextAlign.Center)
                        val annotatedText = buildAnnotatedString { when (uiState.language) { "ur" -> { append("براہ کرم "); withStyle(style = SpanStyle(color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 15.sp)) { append("(5 منٹ)") }; append(" انتظار فرمائیں\nآپ کی مسجد لائیو ہو جائے گی۔") }; "hi" -> { append("कृपया "); withStyle(style = SpanStyle(color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 15.sp)) { append("(5 मिनट)") }; append(" प्रतीक्षा करें\nआपकी मस्जिद लाइव हो जाएगी।") }; else -> { append("Please wait "); withStyle(style = SpanStyle(color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 15.sp)) { append("(5 Min)") }; append("\nYour Masjid will be live.") } } }
                        Text(text = annotatedText, fontSize = 14.5.sp, color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), lineHeight = 22.sp)
                        Button(onClick = { showThanksDialog = false; onMasjidSubmitted() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(46.dp)) { Text(text = when (uiState.language) { "ur" -> "ٹھیک ہے"; "hi" -> "ठीक है"; else -> "OK" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White) }
                    }
                }
                ColorfulConfettiOverlay(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun SupportFrontScreen(uiState: UIState, onClose: () -> Unit, onLangSelect: (String) -> Unit = {}, onMasjidSubmitted: () -> Unit = {}, onChangeMasjid: () -> Unit = {}, onSelectMasjid: (MasjidItem) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val primaryGold = Color(0xFFF3DE8E)
    val whatsappTarget = "919960171516"
    var showRateUsDialog by remember { mutableStateOf(false) }
    var showOtherAmountDialog by remember { mutableStateOf(false) }
    var showUpiAppChooserDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var selectedContributionAmount by remember { mutableIntStateOf(250) }
    val prefs = remember { context.getSharedPreferences("azan_user_rating", Context.MODE_PRIVATE) }
    val initialRating = remember(prefs) { try { prefs.getFloat("saved_stars", 0f).toDouble() } catch (e: Throwable) { 0.0 } }
    var userRating by remember { mutableDoubleStateOf(initialRating) }
    var averageRating by remember { mutableDoubleStateOf(0.0) }
    var totalReviews by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { try { val (avg, count) = GoogleSheetMasjidSync.fetchRatingStats(); if (count > 0 && avg > 0.0) { averageRating = avg; totalReviews = count } } catch (t: Throwable) {} }

    fun openWhatsApp() {
        try {
            val message = when (uiState.language) { "ur" -> "السلام علیکم! اذان ایپ کے بارے میں فیڈ بیک / مدد درکار ہے۔"; "hi" -> "अस्सलाम-ओ-अलैकुम! अज़ान ऐप के बारे में सहायता / सुझाव।"; else -> "Assalam-o-Alaikum! I need support / have feedback regarding Azan App." }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsappTarget?text=${Uri.encode(message)}")))
        } catch (e: Exception) { try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$whatsappTarget"))) } catch (e2: Exception) { Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show() } }
    }

    fun launchUpiForApp(packageName: String?, amount: Int) {
        val amStr = if (amount > 0) "&am=$amount" else ""
        val uri = Uri.parse("upi://pay?pa=9960171516@ybl&pn=${Uri.encode("Azan Time App Support")}${amStr}&cu=INR")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (!packageName.isNullOrBlank()) intent.setPackage(packageName)
        try { context.startActivity(intent) } catch (e: Exception) { try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Pay with UPI")) } catch (e2: Exception) { Toast.makeText(context, "No UPI app found", Toast.LENGTH_LONG).show() } }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF040E0A), Color(0xFF06140D), Color(0xFF040C08), Color(0xFF020704))))) {
        val screenHeight = maxHeight
        val isCompact = screenHeight < 680.dp
        val itemSpacing = if (isCompact) 4.dp else 6.dp
        val logoSize = if (isCompact) 70.dp else 80.dp
        val wordmarkHeight = if (isCompact) 36.dp else 42.dp

        Column(modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(itemSpacing), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                LanguageCirclesRow(currentLang = uiState.language, onLangSelect = onLangSelect)
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp).background(Color(0x26FFFFFF), CircleShape)) { Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = primaryGold) }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AzanLogoCard(size = logoSize); Spacer(modifier = Modifier.height(14.dp)); AzanTimeWordmark(height = wordmarkHeight, topPadding = 6.dp, bottomPadding = 6.dp)
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(imageVector = Icons.Outlined.Favorite, contentDescription = null, tint = primaryGold, modifier = Modifier.size(20.dp)); Text(text = when (uiState.language) { "ur" -> "ایپ میں تعاون کریں"; "hi" -> "ऐप को सपोर्ट करें"; else -> "Support the App" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryGold) }
                        Text(text = "UPI Pay / QR", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF34D399))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100, 250, 500).forEach { amt -> Button(onClick = { selectedContributionAmount = amt; showUpiAppChooserDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White), border = BorderStroke(1.dp, Color(0xFF424242)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(38.dp)) { Text(text = "₹$amt", fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                        Button(onClick = { showOtherAmountDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = primaryGold), border = BorderStroke(1.dp, Color(0xFF424242)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(38.dp)) { Text(text = when (uiState.language) { "ur" -> "دیگر"; "hi" -> "अन्य"; else -> "Other" }, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                    Button(onClick = { showQrDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A34D399)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(40.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = primaryGold, modifier = Modifier.size(18.dp)); Text(text = when (uiState.language) { "ur" -> "کیو آر کوڈ اسکین کریں"; "hi" -> "QR کوڈ स्कैन करें"; else -> "Scan QR Code" }, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = primaryGold) } }
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))) {
                Row(modifier = Modifier.fillMaxWidth().clickable { openWhatsApp() }.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Image(painter = painterResource(id = R.drawable.ic_whatsapp_outlined), contentDescription = null, modifier = Modifier.size(28.dp)); Column { Text(text = when (uiState.language) { "ur" -> "سپورٹ اور فیڈبیک"; "hi" -> "सपोर्ट और सुझाव"; else -> "Support & Feedback" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(text = when (uiState.language) { "ur" -> "فوری واٹس ایپ"; "hi" -> "इंस्टेंट व्हाट्सएप"; else -> "Instant WhatsApp" }, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4ADE80)) } }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))) {
                Row(modifier = Modifier.fillMaxWidth().clickable { showRateUsDialog = true }.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    val appRatingDisplay = if (averageRating > 0.0) averageRating else 4.9
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { 
                        Box(modifier = Modifier.size(28.dp).background(Color(0x26F3DE8E), CircleShape), contentAlignment = Alignment.Center) { 
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = primaryGold, modifier = Modifier.size(16.dp)) 
                        }
                        Column { 
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { 
                                Text(text = String.format(Locale.US, "%.1f", appRatingDisplay), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) { for (i in 1..5) { StarIconDisplay(fillRatio = if(appRatingDisplay >= i) 1f else if(appRatingDisplay >= i-0.5) 0.5f else 0f, starSize = 10.dp) } } 
                            }
                            Text(text = when (uiState.language) { "ur" -> "پلے اسٹور پر ریٹنگ دیں"; "hi" -> "प्ले स्टोर पर रेटिंग दें"; else -> "Rate us on Play Store" }, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f)) 
                        } 
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(primaryGold).padding(horizontal = 12.dp, vertical = 5.dp)) { Text(text = when (uiState.language) { "ur" -> "5★ ریٹنگ دیں"; "hi" -> "5★ रेटिंग दें"; else -> "Rate Us 5★" }, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF040E0A)) }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            com.example.ui.AiStudioLoadingFooter(modifier = Modifier.padding(top = 0.dp, bottom = 2.dp))
        }
    }

    if (showOtherAmountDialog) {
        var customAmountText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showOtherAmountDialog = false }, properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false)) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).imePadding()) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = when (uiState.language) { "ur" -> "تعاون کی رقم درج کریں"; "hi" -> "सहयोग राशि दर्ज करें"; else -> "Enter Contribution Amount" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    OutlinedTextField(value = customAmountText, onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() } }, label = { Text(text = when (uiState.language) { "ur" -> "رقم (₹)"; "hi" -> "राशि (₹)"; else -> "Amount (₹)" }, color = Color.White.copy(alpha = 0.7f)) }, placeholder = { Text("1000", color = Color.White.copy(alpha = 0.3f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryGold, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showOtherAmountDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.weight(1f)) { Text(text = when (uiState.language) { "ur" -> "منسوخ"; "hi" -> "रद्द करें"; else -> "Cancel" }, color = Color.White) }
                        Button(onClick = { val entered = customAmountText.toIntOrNull(); if (entered != null && entered > 0) { selectedContributionAmount = entered; showOtherAmountDialog = false; showUpiAppChooserDialog = true } else { Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B842)), modifier = Modifier.weight(1f)) { Text(text = when (uiState.language) { "ur" -> "آگے بڑھیں"; "hi" -> "आगे बढ़ें"; else -> "Proceed" }, color = Color(0xFF0F1713), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
    if (showQrDialog) { 
        Dialog(onDismissRequest = { showQrDialog = false }) { 
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B15)), modifier = Modifier.fillMaxWidth(0.95f).padding(12.dp)) { 
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                        Text(text = when (uiState.language) { "ur" -> "ایپ سپورٹ کے لیے کیو آر اسکین کریں"; "hi" -> "ऐप सपोर्ट के लिए क्यूआर स्कैन करें"; else -> "Scan QR to Support App" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = primaryGold); IconButton(onClick = { showQrDialog = false }, modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), CircleShape)) { Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = primaryGold, modifier = Modifier.size(18.dp)) } 
                    }
                    Box(modifier = Modifier.size(220.dp).background(Color.White, RoundedCornerShape(14.dp)).padding(8.dp), contentAlignment = Alignment.Center) { 
                        AsyncImage(model = R.drawable.qr_support_donate, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) 
                    }
                    Text(text = when (uiState.language) { "ur" -> "کسی بھی UPI ایپ سے اسکین کریں (GPay, PhonePe, Paytm)\nUPI ID: 9960171516@ybl"; "hi" -> "किसी भी UPI ऐप से स्कैन करें (GPay, PhonePe, Paytm)\nUPI ID: 9960171516@ybl"; else -> "Scan using any UPI App (GPay, PhonePe, Paytm)\nUPI ID: 9960171516@ybl" }, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center) 
                } 
            } 
        } 
    }
    if (showRateUsDialog) {
        var tempRating by remember { mutableDoubleStateOf(if (userRating > 0.0) userRating else 5.0) }
        var isSubmitting by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { showRateUsDialog = false }) { 
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)), modifier = Modifier.fillMaxWidth().padding(16.dp)) { 
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { 
                    Box(modifier = Modifier.fillMaxWidth()) { 
                        Text(text = when (uiState.language) { "ur" -> "اذان ٹائم کی درجہ بندی کریں"; "hi" -> "अज़ान टाइम को रेटिंग दें"; else -> "Rate Azan Time" }, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = primaryGold, modifier = Modifier.align(Alignment.Center))
                        IconButton(onClick = { showRateUsDialog = false }, modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), CircleShape).align(Alignment.CenterEnd)) { Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = primaryGold, modifier = Modifier.size(18.dp)) } 
                    }
                    Text(text = when (uiState.language) { "ur" -> "اپنے تجربے کی درجہ بندی کریں۔ جمع کرنے پر آپ کی ریٹنگ محفوظ ہوگی اور پلے اسٹور پر ریویو کے لیے بھیجا جائے گا۔"; "hi" -> "अपने अनुभव को रेट करें। सबमिट करने पर आपकी रेटिंग सुरक्षित होगी और प्ले स्टोर पर रिव्यू के लिए भेजा जाएगा।"; else -> "Rate your experience. Submitting will save your rating and redirect to Google Play Store to review." }, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center)
                    InteractiveHalfStarRatingBar(rating = tempRating, onRatingSelected = { tempRating = it }, starSize = 36.dp, spacing = 8.dp)
                    Text(text = String.format(Locale.US, "%.1f / 5.0 Stars", tempRating), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    Button(onClick = { 
                        isSubmitting = true
                        userRating = tempRating
                        try { prefs.edit().putFloat("saved_stars", tempRating.toFloat()).apply() } catch (e: Throwable) {}
                        coroutineScope.launch { 
                            try { GoogleSheetMasjidSync.submitRating(tempRating) } catch (t: Throwable) {}
                            isSubmitting = false
                            showRateUsDialog = false
                            try { 
                                val pkgName = context.packageName
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkgName")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }) 
                            } catch (e: Exception) { 
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (e2: Exception) {} 
                            } 
                        }
                        Toast.makeText(context, "JazakAllah Khair!", Toast.LENGTH_SHORT).show() 
                    }, enabled = !isSubmitting, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B842)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { 
                        if (isSubmitting) { 
                            CircularProgressIndicator(color = Color(0xFF0F1713), strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) 
                        } else { 
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { 
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFF0F1713), modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = when (uiState.language) { "ur" -> "پلے اسٹور پر جمع اور ریٹ کریں"; "hi" -> "प्ले स्टोर पर सबमिट और रेट करें"; else -> "Submit & Rate on Play Store" }, color = Color(0xFF0F1713), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, textAlign = TextAlign.Center) 
                            } 
                        } 
                    } 
                } 
            } 
        }
    }
    if (showUpiAppChooserDialog) { 
        UpiAppChooserDialog(amount = selectedContributionAmount, language = uiState.language, onDismiss = { showUpiAppChooserDialog = false }, onAppSelected = { pkg -> 
            showUpiAppChooserDialog = false
            launchUpiForApp(pkg, selectedContributionAmount) 
        }) 
    }
}

// Top-level helpers and classes

fun prepareStraightMasjidPhotoBase64(context: Context, uri: Uri, maxDim: Int = 1080): String? {
    return try {
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try { context.contentResolver.openInputStream(uri)?.use { stream -> orientation = ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } } catch (e: Throwable) {}
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val matrix = Matrix()
        when (orientation) { ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f); ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f); ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f) }
        val orientedBitmap = if (!matrix.isIdentity) Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true) else originalBitmap
        val scale = (maxDim.toFloat() / maxOf(orientedBitmap.width, orientedBitmap.height)).coerceAtMost(1f)
        val finalBitmap = if (scale < 1f) Bitmap.createScaledBitmap(orientedBitmap, (orientedBitmap.width * scale).toInt(), (orientedBitmap.height * scale).toInt(), true) else orientedBitmap
        val baos = ByteArrayOutputStream(); finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (e: Throwable) { null }
}

data class StandardizedQrResult(val bitmap: Bitmap, val base64: String, val localFilePath: String, val isCleanRegenerated: Boolean, val decodedPayload: String?)

suspend fun prepareStandardizedMasjidQr(context: Context, uri: Uri): StandardizedQrResult? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
        var decodedContent: String? = null
        try {
            val pixels = IntArray(originalBitmap.width * originalBitmap.height)
            originalBitmap.getPixels(pixels, 0, originalBitmap.width, 0, 0, originalBitmap.width, originalBitmap.height)
            decodedContent = com.google.zxing.qrcode.QRCodeReader().decode(com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(com.google.zxing.RGBLuminanceSource(originalBitmap.width, originalBitmap.height, pixels)))).text
        } catch (e: Exception) {}
        val finalBitmap: Bitmap
        val isReg: Boolean
        if (!decodedContent.isNullOrBlank()) {
            val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(decodedContent, com.google.zxing.BarcodeFormat.QR_CODE, 600, 600, mapOf(com.google.zxing.EncodeHintType.MARGIN to 1))
            finalBitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
            for (x in 0 until 600) for (y in 0 until 600) finalBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            isReg = true
        } else {
            val side = minOf(originalBitmap.width, originalBitmap.height)
            finalBitmap = Bitmap.createScaledBitmap(Bitmap.createBitmap(originalBitmap, (originalBitmap.width - side) / 2, (originalBitmap.height - side) / 2, side, side), 600, 600, true)
            isReg = false
        }
        val destFile = java.io.File(context.filesDir, "custom_masjid_qr.jpg")
        destFile.outputStream().use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        val baos = ByteArrayOutputStream(); finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
        StandardizedQrResult(finalBitmap, Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP), destFile.absolutePath, isReg, decodedContent)
    } catch (e: Throwable) { null }
}

private fun toTitleCaseWords(input: String): String {
    if (input.isEmpty()) return input
    val chars = input.toCharArray(); var cap = true
    for (i in chars.indices) { val c = chars[i]; if (c.isWhitespace() || c == ',' || c == '.' || c == '/') cap = true else if (cap && c.isLetter()) { chars[i] = c.uppercaseChar(); cap = false } else cap = false }
    return String(chars)
}

@Composable
fun AddMasjidAutoDialog(language: String, onDismiss: () -> Unit, onSubmitted: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var masjidName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var adminContact by remember { mutableStateOf("") }
    var adminId by remember { mutableStateOf("admin") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedQrUri by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedPhotoUri = uri
    }
    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedQrUri = uri
    }

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF060E1C)),
            border = BorderStroke(1.2.dp, Color(0xFFF3DE8E).copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Title and Circular Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (language) {
                            "ur" -> "نئی مسجد شامل کریں"
                            "hi" -> "नई मस्जिद जोड़ें"
                            else -> "Add New Masjid"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3DE8E)
                    )
                    IconButton(
                        onClick = { if (!isSending) onDismiss() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF101A2E), CircleShape)
                            .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFF3DE8E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Instruction Banner
                Surface(
                    color = Color(0xFF0F1B2D),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (language) {
                            "ur" -> "براہ کرم ذمہ دار شخص (مولانا / متولی / بنگی صاحب / ٹرسٹ ممبر) یہ فارم جمع کروائیں۔"
                            "hi" -> "जिम्मेदार व्यक्ति (मौलाना / मुतवल्ली / बंगी साहब / ट्रस्ट मेंबर) को ही फॉर्म जमा करना चाहिए।"
                            else -> "The responsible person (Moulana / Mutawalli / Bangi sahab / Trust Member) should submit the form."
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF07101E),
                    unfocusedContainerColor = Color(0xFF07101E),
                    focusedBorderColor = Color(0xFFF3DE8E),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFF3DE8E),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color(0xFFF3DE8E)
                )

                // Field 1: Name of Masjid *
                OutlinedTextField(
                    value = masjidName,
                    onValueChange = { masjidName = toTitleCaseWords(it) },
                    label = { Text("Name of Masjid *", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                // Field 2: Location / City / Area *
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = toTitleCaseWords(it) },
                    label = { Text("Location / City / Area *", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                // Field 3: Admin Contact Number * with trailing checkmark
                OutlinedTextField(
                    value = adminContact,
                    onValueChange = { adminContact = it },
                    label = { Text("Admin Contact Number *", fontSize = 13.sp) },
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (adminContact.filter { ch -> ch.isDigit() }.length >= 10) Color(0xFF2EC878) else Color(0xFFF3DE8E).copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                // Field 4: Admin ID - admin
                OutlinedTextField(
                    value = adminId,
                    onValueChange = { adminId = it },
                    label = { Text("Admin ID - admin", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                // Section 5: Masjid Photo (Optional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Masjid Photo (Optional):",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selectedPhotoUri != null) Color(0xFF2EC878) else Color(0xFF38BDF8).copy(alpha = 0.45f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedPhotoUri != null) Color(0xFF0C241C) else Color(0xFF0A1A30)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedPhotoUri != null) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (selectedPhotoUri != null) Color(0xFF2EC878) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPhotoUri != null) "Masjid Photo Selected ✓ (Change)" else "Browse / Upload Masjid Photo",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedPhotoUri != null) Color(0xFF34D399) else Color(0xFF38BDF8)
                        )
                    }
                }

                // Section 6: Masjid QR (Optional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Masjid QR (Optional):",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedButton(
                        onClick = { qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selectedQrUri != null) Color(0xFF2EC878) else Color(0xFF38BDF8).copy(alpha = 0.45f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedQrUri != null) Color(0xFF0C241C) else Color(0xFF0A1A30)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedQrUri != null) Icons.Default.CheckCircle else Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = if (selectedQrUri != null) Color(0xFF2EC878) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedQrUri != null) "Masjid QR Selected ✓ (Change)" else "Browse / Upload Masjid QR",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedQrUri != null) Color(0xFF34D399) else Color(0xFF38BDF8)
                        )
                    }
                }

                // Submit Button: Emerald Green with Send Icon
                Button(
                    onClick = {
                        when {
                            masjidName.isBlank() -> {
                                Toast.makeText(context, "Please enter Name of Masjid", Toast.LENGTH_SHORT).show()
                            }
                            location.isBlank() -> {
                                Toast.makeText(context, "Please enter Location / City / Area", Toast.LENGTH_SHORT).show()
                            }
                            adminContact.isBlank() -> {
                                Toast.makeText(context, "Please enter Admin Contact Number", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                isSending = true
                                coroutineScope.launch {
                                    val pb = selectedPhotoUri?.let { prepareStraightMasjidPhotoBase64(context, it) }
                                    val qb = selectedQrUri?.let { prepareStandardizedMasjidQr(context, it)?.base64 }
                                    try {
                                        GoogleSheetMasjidSync.submitMasjidRequestAuto(
                                            name = masjidName.trim(),
                                            location = location.trim(),
                                            adminName = adminId.trim().ifBlank { "admin" },
                                            contact = adminContact.trim(),
                                            photoBase64 = pb,
                                            photoName = "${masjidName.trim()}.jpg",
                                            qrBase64 = qb,
                                            qrName = "${masjidName.trim()}_QR.jpg",
                                            webAppUrl = GoogleSheetMasjidSync.APPS_SCRIPT_WEBAPP_URL
                                        )
                                    } catch (e: Exception) {
                                        // Handled gracefully
                                    }
                                    isSending = false
                                    onSubmitted()
                                }
                            }
                        }
                    },
                    enabled = !isSending,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2EC878),
                        disabledContainerColor = Color(0xFF2EC878).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Request",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StarIconDisplay(fillRatio: Float, starSize: Dp = 20.dp, tint: Color = Color(0xFFFFD700)) {
    Canvas(modifier = Modifier.size(starSize)) {
        val path = Path().apply {
            val cx = size.width / 2f; val cy = size.height / 2f; val outerR = size.minDimension / 2f; val innerR = outerR * 0.42f
            for (p in 0 until 10) { 
                val r = if (p % 2 == 0) outerR else innerR
                val angle = Math.toRadians((p * 36 - 90).toDouble())
                val px = cx + (r * kotlin.math.cos(angle)).toFloat()
                val py = cy + (r * kotlin.math.sin(angle)).toFloat()
                if (p == 0) moveTo(px, py) else lineTo(px, py) 
            }
            close()
        }
        if (fillRatio >= 1f) drawPath(path, tint) else if (fillRatio >= 0.5f) { clipRect(right = size.width / 2f) { drawPath(path, tint) }; drawPath(path, tint.copy(alpha = 0.5f), style = Stroke(1.dp.toPx())) } else drawPath(path, tint.copy(alpha = 0.3f), style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun InteractiveHalfStarRatingBar(rating: Double, onRatingSelected: (Double) -> Unit, starSize: Dp = 24.dp, tint: Color = Color(0xFFFFD700), spacing: Dp = 4.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            Box(modifier = Modifier.size(starSize)) {
                StarIconDisplay(fillRatio = if (rating >= i) 1f else if (rating >= i - 0.5) 0.5f else 0f, starSize = starSize, tint = tint)
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRatingSelected(i - 0.5) })
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRatingSelected(i.toDouble()) })
                }
            }
        }
    }
}

@Composable
fun UpiAppChooserDialog(amount: Int, language: String = "en", onDismiss: () -> Unit, onAppSelected: (String?) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B15)), border = BorderStroke(1.5.dp, Color(0xFFE5B842).copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Pay ₹$amount via UPI", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF3DE8E))
                val apps = listOf(Triple("PhonePe", "com.phonepe.app", R.drawable.ic_phonepe), Triple("Google Pay", "com.google.android.apps.nbu.paisa.user", R.drawable.ic_gpay), Triple("Paytm", "net.one97.paytm", R.drawable.ic_paytm), Triple("Other / Default", null, R.drawable.ic_upi))
                apps.forEach { (name, pkg, icon) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onAppSelected(pkg) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(painter = painterResource(id = icon), contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape))
                        Text(text = name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorfulConfettiOverlay(modifier: Modifier = Modifier) {
    var elapsedTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { 
        val start = System.nanoTime()
        while (true) { 
            withFrameNanos { elapsedTime = (it - start) / 1_000_000_000f } 
        } 
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val random = java.util.Random(1337)
        repeat(50) {
            val t = (elapsedTime + random.nextFloat() * 4f) % 4f
            val alpha = if (t < 2.5f) 1f else (1f - (t - 2.5f) / 1.5f).coerceIn(0f, 1f)
            val x = size.width * (0.5f + (random.nextFloat() - 0.5f) * (t * 0.5f))
            val y = size.height * (0.2f + t * 0.6f)
            drawCircle(color = listOf(Color.Yellow, Color.Red, Color.Green, Color.Blue, Color.Magenta).random().copy(alpha = alpha), radius = 6f, center = Offset(x, y))
        }
    }
}

@Composable
fun AnimatedAddMasjidFormPreview(isSelected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "htmlDemoLoop")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "demoProgress"
    )
    val caretBlink by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretBlink"
    )
    val showCaret = caretBlink > 0.45f

    // Timeline phases (0.0 to 1.0 over 11s):
    // 0.05 - 0.20: Typing Name
    val nameProgress = ((progress - 0.05f) / 0.15f).coerceIn(0f, 1f)
    val nameText = "Jama Masjid".take((nameProgress * "Jama Masjid".length).toInt())

    // 0.20 - 0.35: Typing Location
    val locProgress = ((progress - 0.20f) / 0.15f).coerceIn(0f, 1f)
    val locText = "Bijapur Bes, Solapur".take((locProgress * "Bijapur Bes, Solapur".length).toInt())

    // 0.41 - 0.57: Typing Mobile Number + Verified Tick
    val phoneProgress = ((progress - 0.41f) / 0.16f).coerceIn(0f, 1f)
    val phoneText = "9890098900".take((phoneProgress * "9890098900".length).toInt())
    val phoneVerified = progress >= 0.57f

    // 0.63 - 0.73: Photo Upload progress -> Uploaded
    val photoBarProgress = ((progress - 0.63f) / 0.07f).coerceIn(0f, 1f)
    val photoUploading = progress in 0.63f..0.70f
    val photoUploaded = progress >= 0.70f

    // 0.79 - 0.88: QR Upload progress -> Uploaded
    val qrBarProgress = ((progress - 0.79f) / 0.06f).coerceIn(0f, 1f)
    val qrUploading = progress in 0.79f..0.85f
    val qrUploaded = progress >= 0.85f

    // 0.88 - 1.00: Submit / Sending / Success
    val isSending = progress in 0.88f..0.94f
    val isSuccess = progress >= 0.94f

    fun smoothStep(start: Float, end: Float, x: Float): Float {
        val t = ((x - start) / (end - start)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    // Smooth scroll fraction across the entire real scrollable content height
    val scrollFraction = when {
        progress < 0.35f -> 0f
        progress < 0.41f -> 0.42f * smoothStep(0.35f, 0.41f, progress)
        progress < 0.58f -> 0.42f
        progress < 0.63f -> 0.42f + 0.34f * smoothStep(0.58f, 0.63f, progress)
        progress < 0.74f -> 0.76f
        progress < 0.79f -> 0.76f + 0.24f * smoothStep(0.74f, 0.79f, progress)
        else -> 1f
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollFraction, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo((scrollFraction * scrollState.maxValue).toInt())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSelected) 370.dp else 240.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF0C1424))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState, enabled = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Add New Masjid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD166))
                Box(modifier = Modifier.size(20.dp).background(Color(0xFF101A2E), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(11.dp))
                }
            }

            // Instruction Banner
            Surface(
                color = Color(0xFF101A2E),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD166).copy(alpha = 0.25f))
            ) {
                Text(
                    text = "The responsible person (Moulana / Mutawalli / Bangi sahab / Trust Member) should submit the form.",
                    fontSize = 9.5.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(8.dp),
                    lineHeight = 12.sp
                )
            }

            // Field 1: Name of Masjid
            DemoInputField(
                placeholder = "Name of Masjid *",
                value = nameText,
                isFocused = progress in 0.05f..0.20f,
                showCaret = showCaret
            )

            // Field 2: Location
            DemoInputField(
                placeholder = "Location / City / Area *",
                value = locText,
                isFocused = progress in 0.20f..0.35f,
                showCaret = showCaret
            )

            // Field 3: Admin Contact Number
            DemoInputField(
                placeholder = "Admin Contact Number *",
                value = phoneText,
                isFocused = progress in 0.41f..0.57f,
                showCaret = showCaret,
                trailingIcon = if (phoneVerified) {
                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(14.dp)) }
                } else null
            )

            // Field 4: Admin ID - admin
            Surface(
                color = Color(0xFF101A2E),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(text = "Admin ID - admin", fontSize = 8.sp, color = Color.Gray)
                    Text(text = "admin", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // Field 5: Masjid Photo Upload
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Masjid Photo (Optional):", fontSize = 9.sp, color = Color.Gray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (photoUploaded) Color(0xFF0C241C) else Color(0xFF101A2E))
                        .border(
                            1.dp,
                            when {
                                photoUploaded -> Color(0xFF22C55E).copy(alpha = 0.6f)
                                photoUploading -> Color(0xFF38BDF8)
                                else -> Color(0xFF1E293B)
                            },
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    if (photoUploaded) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(22.dp).background(Color(0xFF1E293B), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Mosque, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(13.dp))
                                }
                                Column {
                                    Text(text = "jama_masjid.jpg", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(text = "✓ Uploaded (1.2 MB)", fontSize = 8.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(text = "✕", fontSize = 9.sp, color = Color.Gray)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (photoUploading) "Uploading Photo..." else "Browse / Upload Masjid Photo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        if (photoUploading) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(photoBarProgress)
                                    .height(2.5.dp)
                                    .background(Color(0xFF38BDF8))
                            )
                        }
                    }
                }
            }

            // Field 6: Masjid QR Upload
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Masjid QR (Optional):", fontSize = 9.sp, color = Color.Gray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (qrUploaded) Color(0xFF0C241C) else Color(0xFF101A2E))
                        .border(
                            1.dp,
                            when {
                                qrUploaded -> Color(0xFF22C55E).copy(alpha = 0.6f)
                                qrUploading -> Color(0xFF38BDF8)
                                else -> Color(0xFF1E293B)
                            },
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    if (qrUploaded) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(22.dp).background(Color.White, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text(text = "masjid_qr.png", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(text = "✓ Uploaded (245 KB)", fontSize = 8.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(text = "✕", fontSize = 9.sp, color = Color.Gray)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (qrUploading) "Uploading QR..." else "Browse / Upload Masjid QR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        if (qrUploading) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(qrBarProgress)
                                    .height(2.5.dp)
                                    .background(Color(0xFF38BDF8))
                            )
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSuccess) Color(0xFF22C55E) else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF070B14)
                        )
                    } else if (isSuccess) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF070B14), modifier = Modifier.size(13.dp))
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF070B14), modifier = Modifier.size(12.dp))
                    }
                    Text(
                        text = when {
                            isSuccess -> "Request Sent Successfully! ✓"
                            isSending -> "Sending Request..."
                            else -> "Send Request"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF070B14)
                    )
                }
            }
        }
    }
}

@Composable
fun DemoInputField(
    placeholder: String,
    value: String,
    isFocused: Boolean,
    showCaret: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color(0xFF101A2E), RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (isFocused) Color(0xFF38BDF8) else Color(0xFF1E293B),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayText = if (value.isEmpty() && !isFocused) placeholder else value
                if (displayText.isNotEmpty()) {
                    Text(
                        text = displayText,
                        fontSize = 10.5.sp,
                        color = if (value.isEmpty()) Color.Gray else Color.White,
                        fontWeight = if (value.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (isFocused) {
                    Text(
                        text = placeholder,
                        fontSize = 10.5.sp,
                        color = Color.Gray.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isFocused && showCaret) {
                    Text(
                        text = "|",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}
