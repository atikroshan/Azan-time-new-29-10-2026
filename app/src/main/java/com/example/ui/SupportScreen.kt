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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.LanguageCirclesRow
import com.example.R
import com.example.appBackground
import com.example.data.GoogleSheetMasjidSync
import com.example.data.MasjidItem
import com.example.islamicStarBackground
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
    onOpenSupportFrontScreen: () -> Unit = {},
    onOpenAdminLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFrontScreen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // SupportBackScreen rendered in background with conditional blur
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

        // Dimmed backdrop overlay when front screen is open
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

        // SupportFrontScreen sliding up as dual overlay sheet (Half Height)
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
            val sheetOffsetY = remember { androidx.compose.animation.core.Animatable(0f) }
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
                    // Top Drag Indicator Bar (Fully working: tap or drag to close)
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
    var showAddMasjidDialog by remember { mutableStateOf(false) }
    var showThanksDialog by remember { mutableStateOf(false) }
    var showMasjidSelectorDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040E0A),
                        Color(0xFF06140D),
                        Color(0xFF040C08),
                        Color(0xFF020704)
                    )
                )
            )
    ) {
        val screenHeight = maxHeight
        val isCompact = screenHeight < 680.dp
        val isUltraCompact = screenHeight < 580.dp

        val itemSpacing = when {
            screenHeight >= 720.dp -> 8.dp
            isCompact -> 5.dp
            else -> 6.dp
        }
        val slotVerticalPadding = when {
            screenHeight >= 720.dp -> 10.dp
            isCompact -> 6.dp
            else -> 8.dp
        }
        val logoSize = when {
            screenHeight >= 720.dp -> 82.dp
            isUltraCompact -> 56.dp
            isCompact -> 62.dp
            else -> 72.dp
        }
        val wordmarkHeight = when {
            screenHeight >= 720.dp -> 34.dp
            isUltraCompact -> 22.dp
            isCompact -> 26.dp
            else -> 30.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Language Buttons at top left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                LanguageCirclesRow(
                    currentLang = uiState.language,
                    onLangSelect = onLangSelect
                )
            }

            // 2. Center: Logo & AZAN TIME Wordmark with Punchline
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AzanLogoCard(size = logoSize)
                Spacer(modifier = Modifier.height(itemSpacing))
                AzanTimeWordmark(
                    height = wordmarkHeight,
                    topPadding = 2.dp,
                    bottomPadding = 2.dp
                )
            }

            // 3. 2-Row 2-Column Vertical Rectangular Cards Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Admin side | Change
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SupportGridItem(
                        title = when (uiState.language) {
                            "ur" -> "ایڈمن لاگ ان"
                            "hi" -> "एडमिन लॉगिन"
                            else -> "Admin Login"
                        },
                        subtitle = "Manage Timings",
                        icon = Icons.Filled.Person,
                        onClick = onOpenAdminLogin,
                        modifier = Modifier.weight(1f)
                    )
                    SupportGridItem(
                        title = if (uiState.selectedMasjid.id.equals("guest", ignoreCase = true) || uiState.selectedMasjid.name.isBlank()) {
                            when (uiState.language) {
                                "ur" -> "گیسٹ مسجد"
                                "hi" -> "गेस्ट मस्जिद"
                                else -> "Guest Masjid"
                            }
                        } else {
                            uiState.selectedMasjid.name
                        },
                        subtitle = uiState.selectedMasjid.city.ifBlank { "Solapur City" },
                        icon = Icons.Outlined.Mosque,
                        onClick = onChangeMasjid,
                        modifier = Modifier.weight(1f),
                        actionText = when (uiState.language) {
                            "ur" -> "تبدیل ⌵"
                            "hi" -> "बदलें ⌵"
                            else -> "Change ⌵"
                        }
                    )
                }

                // Row 2: Request | Support
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SupportGridItem(
                        title = when (uiState.language) {
                            "ur" -> "نئی مسجد"
                            "hi" -> "नई मस्जिद"
                            else -> "Request Add"
                        },
                        subtitle = "Submit Timings",
                        icon = Icons.Outlined.AddLocationAlt,
                        onClick = { showAddMasjidDialog = true },
                        modifier = Modifier.weight(1f),
                        actionText = when (uiState.language) {
                            "ur" -> "درخواست +"
                            "hi" -> "जोड़ें +"
                            else -> "Request +"
                        }
                    )
                    SupportGridItem(
                        title = when (uiState.language) {
                            "ur" -> "سپورٹ"
                            "hi" -> "सपोर्ट"
                            else -> "Support"
                        },
                        subtitle = "Rate & Follow",
                        icon = Icons.Outlined.HeadsetMic,
                        onClick = onOpenFrontScreen,
                        modifier = Modifier.weight(1f),
                        actionText = when (uiState.language) {
                            "ur" -> "کھولیں >"
                            "hi" -> "खोलें >"
                            else -> "Open >"
                        }
                    )
                }
            }

            // Footer
            AiStudioLoadingFooter(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
    }

    // Add Masjid Form Dialog
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

    // Thanks Popup
    if (showThanksDialog) {
        Dialog(
            onDismissRequest = {
                showThanksDialog = false
                onMasjidSubmitted()
            },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.90f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Thanks",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Text(
                            text = when (uiState.language) {
                                "ur" -> "درخواست موصول ہوگئی!"
                                "hi" -> "अनुरोध प्राप्त हुआ!"
                                else -> "Request Received!"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color(0xFF34D399),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val annotatedText = buildAnnotatedString {
                            append("Please wait ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            ) {
                                append("(5 Min)")
                            }
                            append("\nYour Masjid will be live.")
                        }
                        Text(
                            text = annotatedText,
                            fontSize = 14.5.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                showThanksDialog = false
                                onMasjidSubmitted()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "OK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                ColorfulConfettiOverlay(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun SupportFrontScreen(
    uiState: UIState,
    onClose: () -> Unit,
    onLangSelect: (String) -> Unit = {},
    onMasjidSubmitted: () -> Unit = {},
    onChangeMasjid: () -> Unit = {},
    onSelectMasjid: (MasjidItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
    val initialRating = remember(prefs) {
        try {
            prefs.getFloat("saved_stars", 0f).toDouble()
        } catch (e1: Throwable) {
            try {
                prefs.getInt("saved_stars", 0).toDouble()
            } catch (e2: Throwable) {
                try {
                    prefs.getString("saved_stars", "0")?.toDoubleOrNull() ?: 0.0
                } catch (e3: Throwable) {
                    0.0
                }
            }
        }
    }
    var userRating by remember { mutableDoubleStateOf(initialRating) }
    var averageRating by remember { mutableDoubleStateOf(0.0) }
    var totalReviews by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val (avg, count) = GoogleSheetMasjidSync.fetchRatingStats()
            if (count > 0 && avg > 0.0) {
                averageRating = avg
                totalReviews = count
            }
        } catch (t: Throwable) {
            // Never crash
        }
    }

    fun openWhatsApp() {
        try {
            val message = when (uiState.language) {
                "ur" -> "السلام علیکم! اذان ایپ کے بارے میں فیڈ بیک / مدد درکار ہے۔"
                "hi" -> "अस्सلام-ओ-अलैकुम! अज़ान ऐप के बारे में सहायता / सुझाव।"
                else -> "Assalam-o-Alaikum! I need support / have feedback regarding Azan App."
            }
            val uri = Uri.parse("https://wa.me/$whatsappTarget?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$whatsappTarget"))
                context.startActivity(dial)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchUpiForApp(packageName: String?, amount: Int) {
        val amStr = if (amount > 0) "&am=$amount" else ""
        val targetVpa = "9960171516@ybl"
        val targetName = Uri.encode("Azan Time App Support")
        val uri: Uri = Uri.parse("upi://pay?pa=$targetVpa&pn=$targetName${amStr}&cu=INR")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (!packageName.isNullOrBlank()) {
            intent.setPackage(packageName)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(Intent.createChooser(fallbackIntent, "Pay with UPI"))
            } catch (e2: Exception) {
                Toast.makeText(context, "No UPI app found. Please scan the QR code.", Toast.LENGTH_LONG).show()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040E0A),
                        Color(0xFF06140D),
                        Color(0xFF040C08),
                        Color(0xFF020704)
                    )
                )
            )
    ) {
        val screenHeight = maxHeight
        val isCompact = screenHeight < 680.dp
        val isUltraCompact = screenHeight < 580.dp

        val itemSpacing = when {
            screenHeight >= 720.dp -> 8.dp
            isCompact -> 4.dp
            else -> 6.dp
        }
        val slotVerticalPadding = when {
            screenHeight >= 720.dp -> 8.dp
            isCompact -> 4.dp
            else -> 6.dp
        }
        val logoSize = when {
            screenHeight >= 720.dp -> 86.dp
            isUltraCompact -> 60.dp
            isCompact -> 70.dp
            else -> 80.dp
        }
        val wordmarkHeight = when {
            screenHeight >= 720.dp -> 46.dp
            isUltraCompact -> 30.dp
            isCompact -> 36.dp
            else -> 42.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Language Circles on Left + Close Button (X) on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LanguageCirclesRow(
                    currentLang = uiState.language,
                    onLangSelect = onLangSelect
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x26FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = primaryGold
                    )
                }
            }

            // 2. Center: Logo & AZAN TIME Wordmark
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AzanLogoCard(size = logoSize)
                Spacer(modifier = Modifier.height(14.dp))
                AzanTimeWordmark(
                    height = wordmarkHeight,
                    topPadding = 6.dp,
                    bottomPadding = 6.dp
                )
            }

            // Slot 1: App Support UPI Card (₹100, ₹250, ₹500, Other, Scan QR)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = slotVerticalPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = "Support App",
                                tint = Color(0xFFF3DE8E),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "ایپ میں تعاون کریں"
                                    "hi" -> "ऐप को सपोर्ट करें"
                                    else -> "Support the App"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3DE8E)
                            )
                        }
                        Text(
                            text = "UPI Pay / QR",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34D399)
                        )
                    }

                    // Preset Amount Buttons (₹100, ₹250, ₹500, Other)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val amounts = listOf(100, 250, 500)
                        amounts.forEach { amt ->
                            Button(
                                onClick = {
                                    selectedContributionAmount = amt
                                    showUpiAppChooserDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFF424242)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Text(
                                    text = "₹$amt",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { showOtherAmountDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = primaryGold
                            ),
                            border = BorderStroke(1.dp, Color(0xFF424242)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = "Other",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Scan QR Button
                    Button(
                        onClick = { showQrDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A34D399)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = primaryGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Scan QR Code",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryGold
                            )
                        }
                    }
                }
            }

            // Slot 2: Support & Feedback (Full Width)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openWhatsApp() }
                        .padding(horizontal = 14.dp, vertical = slotVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_whatsapp_outlined),
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "سپورٹ اور فیڈبیک"
                                    "hi" -> "सपोर्ट और सुझाव"
                                    else -> "Support & Feedback"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "فوری واٹس ایپ"
                                    "hi" -> "इंस्टेंट व्हाट्सएप"
                                    else -> "Instant WhatsApp"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4ADE80),
                                maxLines = 1
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Slot 3: Rate Us (Full Width)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRateUsDialog = true }
                        .padding(horizontal = 14.dp, vertical = slotVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val appRatingDisplay = if (averageRating > 0.0) averageRating else 4.9
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0x26F3DE8E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = primaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", appRatingDisplay),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryGold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                    for (i in 1..5) {
                                        val fillRatio = when {
                                            appRatingDisplay >= i -> 1f
                                            appRatingDisplay >= i - 0.5 -> 0.5f
                                            else -> 0f
                                        }
                                        StarIconDisplay(fillRatio = fillRatio, size = 10.dp)
                                    }
                                }
                            }
                            Text(
                                text = "Rate us on Play Store",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryGold)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Rate Us 5★",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF040E0A)
                        )
                    }
                }
            }

            // App Version Line Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = slotVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (uiState.language) {
                            "ur" -> "ایپ ورژن"
                            "hi" -> "ऐप वर्ज़न"
                            else -> "App Version"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "v3.1.66",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            AiStudioLoadingFooter(
                modifier = Modifier.padding(top = 0.dp, bottom = 2.dp)
            )
        }
    }

    // Custom Amount Dialog
    if (showOtherAmountDialog) {
        val amountFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val amountKeyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        var customAmountText by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = {
                amountFocusManager.clearFocus()
                amountKeyboardController?.hide()
                showOtherAmountDialog = false
            },
            properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter Contribution Amount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGold
                    )
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (₹)", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("e.g. 1000", color = Color.White.copy(alpha = 0.3f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showOtherAmountDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val entered = customAmountText.toIntOrNull()
                                if (entered != null && entered > 0) {
                                    selectedContributionAmount = entered
                                    showOtherAmountDialog = false
                                    showUpiAppChooserDialog = true
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B842)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Proceed", color = Color(0xFF0F1713), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        val dialogQrModel = R.drawable.qr_support_donate

        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B15)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scan QR to Support App",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold
                        )
                        IconButton(
                            onClick = { showQrDialog = false },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = primaryGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = dialogQrModel,
                            contentDescription = "App Support Donation QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Text(
                        text = "Scan using any UPI App (GPay, PhonePe, Paytm)\nUPI ID: 9960171516@ybl",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Rate Us Dialog
    if (showRateUsDialog) {
        var tempRating by remember { mutableDoubleStateOf(if (userRating > 0.0) userRating else 5.0) }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showRateUsDialog = false }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Rate Azan Time",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = { showRateUsDialog = false },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = primaryGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Rate your experience. Submitting will save your rating and redirect to Google Play Store to review.",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )

                    InteractiveHalfStarRatingBar(
                        rating = tempRating,
                        onRatingSelected = { selected ->
                            tempRating = selected
                        },
                        starSize = 36.dp,
                        spacing = 8.dp
                    )

                    Text(
                        text = String.format(Locale.US, "%.1f / 5.0 Stars", tempRating),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGold
                    )

                    Button(
                        onClick = {
                            isSubmitting = true
                            userRating = tempRating
                            try {
                                prefs.edit().putFloat("saved_stars", tempRating.toFloat()).apply()
                            } catch (e: Throwable) {
                                // Ignore
                            }
                            coroutineScope.launch {
                                try {
                                    GoogleSheetMasjidSync.submitRating(tempRating)
                                } catch (t: Throwable) {
                                    // Ignore
                                }
                                isSubmitting = false
                                showRateUsDialog = false

                                try {
                                    val pkgName = context.packageName
                                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkgName")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                    context.startActivity(marketIntent)
                                } catch (e: Exception) {
                                    try {
                                        val pkgName = context.packageName
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(webIntent)
                                    } catch (e2: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                            Toast.makeText(
                                context,
                                "⭐⭐⭐⭐⭐ JazakAllah Khair! Opening Play Store...",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B842)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color(0xFF0F1713),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", color = Color(0xFF0F1713), fontWeight = FontWeight.Bold)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF0F1713),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Submit & Rate on Play Store",
                                    color = Color(0xFF0F1713),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUpiAppChooserDialog) {
        UpiAppChooserDialog(
            amount = selectedContributionAmount,
            onDismiss = { showUpiAppChooserDialog = false },
            onAppSelected = { pkg ->
                showUpiAppChooserDialog = false
                launchUpiForApp(pkg, selectedContributionAmount)
            }
        )
    }
}

fun prepareStraightMasjidPhotoBase64(context: Context, uri: Uri, maxDim: Int = 1080): String? {
    return try {
        // 1. Read EXIF Orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // 2. Decode raw bitmap
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        // 3. Build rotation matrix to preserve natural upright / straight angle (do not flip to landscape)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val correctlyOrientedBitmap = if (!matrix.isIdentity) {
            Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )
        } else {
            originalBitmap
        }

        // 4. Scale down while strictly preserving aspect ratio
        val w = correctlyOrientedBitmap.width
        val h = correctlyOrientedBitmap.height
        val scale = (maxDim.toFloat() / maxOf(w, h)).coerceAtMost(1f)
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                correctlyOrientedBitmap,
                (w * scale).toInt(),
                (h * scale).toInt(),
                true
            )
        } else {
            correctlyOrientedBitmap
        }

        val baos = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

data class StandardizedQrResult(
    val bitmap: Bitmap,
    val base64: String,
    val localFilePath: String,
    val isCleanRegenerated: Boolean,
    val decodedPayload: String?
)

suspend fun prepareStandardizedMasjidQr(
    context: Context,
    uri: Uri
): StandardizedQrResult? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        // 1. Read EXIF Orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Throwable) {}

        // 2. Decode raw bitmap
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@withContext null

        // 3. Straighten rotation
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        val orientedBitmap = if (!matrix.isIdentity) {
            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        } else {
            originalBitmap
        }

        // 4. Decode QR code content via ZXing QRCodeReader
        var decodedContent: String? = null
        try {
            val width = orientedBitmap.width
            val height = orientedBitmap.height
            val pixels = IntArray(width * height)
            orientedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            val result = com.google.zxing.qrcode.QRCodeReader().decode(binaryBitmap)
            decodedContent = result.text?.trim()
        } catch (_: Throwable) {
            // If full-frame decode failed, try center square crop
            try {
                val side = minOf(orientedBitmap.width, orientedBitmap.height)
                val x = (orientedBitmap.width - side) / 2
                val y = (orientedBitmap.height - side) / 2
                val square = Bitmap.createBitmap(orientedBitmap, x, y, side, side)
                val pixels = IntArray(side * side)
                square.getPixels(pixels, 0, side, 0, 0, side, side)
                val source = com.google.zxing.RGBLuminanceSource(side, side, pixels)
                val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
                val result = com.google.zxing.qrcode.QRCodeReader().decode(binaryBitmap)
                decodedContent = result.text?.trim()
            } catch (_: Throwable) {}
        }

        val finalBitmap: Bitmap
        val isRegenerated: Boolean

        if (!decodedContent.isNullOrBlank()) {
            // Re-generate pixel-perfect standard crisp black & white QR code
            val size = 600
            val hints = mapOf(
                com.google.zxing.EncodeHintType.MARGIN to 1,
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
            )
            val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                decodedContent,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            finalBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (px in 0 until size) {
                for (py in 0 until size) {
                    finalBitmap.setPixel(
                        px,
                        py,
                        if (bitMatrix.get(px, py)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            isRegenerated = true
        } else {
            // Fallback: Crop straight center square and scale to 600x600
            val side = minOf(orientedBitmap.width, orientedBitmap.height)
            val x = (orientedBitmap.width - side) / 2
            val y = (orientedBitmap.height - side) / 2
            val cropped = Bitmap.createBitmap(orientedBitmap, x, y, side, side)
            finalBitmap = Bitmap.createScaledBitmap(cropped, 600, 600, true)
            isRegenerated = false
        }

        // Save straight/clean bitmap to app internal storage
        val destFile = java.io.File(context.filesDir, "custom_masjid_qr.jpg")
        destFile.outputStream().use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        // Generate base64 string
        val baos = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        StandardizedQrResult(
            bitmap = finalBitmap,
            base64 = base64,
            localFilePath = destFile.absolutePath,
            isCleanRegenerated = isRegenerated,
            decodedPayload = decodedContent
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

private suspend fun saveCustomQrToInternalStorage(context: Context, qrUri: Uri): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val destFile = java.io.File(context.filesDir, "custom_masjid_qr.jpg")
        context.contentResolver.openInputStream(qrUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun toTitleCaseWords(input: String): String {
    if (input.isEmpty()) return input
    val chars = input.toCharArray()
    var capitalizeNext = true
    for (i in chars.indices) {
        val c = chars[i]
        if (c.isWhitespace() || c == ',' || c == '-' || c == '/' || c == '(' || c == '.') {
            capitalizeNext = true
        } else if (capitalizeNext && c.isLetter()) {
            chars[i] = c.uppercaseChar()
            capitalizeNext = false
        }
    }
    return String(chars)
}

@Composable
fun AddMasjidAutoDialog(
    language: String,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { com.example.data.PreferencesRepository(context) }
    val savedAppsScriptUrl by prefs.appsScriptUrlFlow.collectAsState(initial = com.example.data.GoogleSheetMasjidSync.APPS_SCRIPT_WEBAPP_URL)
    var masjidName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("admin") }
    var adminContact by remember { mutableStateOf("") }
    val masjidNameFocusRequester = remember { FocusRequester() }
    val locationFocusRequester = remember { FocusRequester() }
    val contactFocusRequester = remember { FocusRequester() }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedQrUri by remember { mutableStateOf<Uri?>(null) }
    var standardizedQr by remember { mutableStateOf<StandardizedQrResult?>(null) }
    var isProcessingQr by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    var dismissKeyboard by remember { mutableStateOf<() -> Unit>({}) }

    val performFormSubmit: () -> Unit = {
        dismissKeyboard()
        if (masjidName.isBlank()) {
            errorMessage = "Please enter Masjid Name"
        } else if (location.isBlank()) {
            errorMessage = "Please enter Location"
        } else if (adminName.isBlank()) {
            errorMessage = "Please enter Admin Name"
        } else if (adminContact.isBlank()) {
            errorMessage = "Please enter Admin Contact Number"
        } else if (!isSending) {
            isSending = true
            val cleanMasjidName = masjidName.trim()
            val targetUrl = savedAppsScriptUrl.trim()
            val photoUri = selectedPhotoUri
            val qrUri = selectedQrUri

            coroutineScope.launch {
                var photoBase64: String? = null
                if (photoUri != null) {
                    try {
                        photoBase64 = prepareStraightMasjidPhotoBase64(context, photoUri)
                    } catch (e: Throwable) {
                        // Proceed without photo if encoding fails
                    }
                }

                var qrBase64: String? = standardizedQr?.base64
                if (qrBase64 == null && qrUri != null) {
                    try {
                        val res = prepareStandardizedMasjidQr(context, qrUri)
                        qrBase64 = res?.base64 ?: prepareStraightMasjidPhotoBase64(context, qrUri)
                        if (res != null) {
                            prefs.setCustomMasjidQrPath(res.localFilePath)
                        }
                    } catch (e: Throwable) {
                        // Proceed without QR if encoding fails
                    }
                }

                try {
                    com.example.data.GoogleSheetMasjidSync.submitMasjidRequestAuto(
                        name = cleanMasjidName,
                        location = location.trim(),
                        adminName = adminName.trim(),
                        contact = adminContact.trim(),
                        photoBase64 = photoBase64,
                        photoName = "$cleanMasjidName.jpg",
                        qrBase64 = qrBase64,
                        qrName = "${cleanMasjidName}_QR.jpg",
                        webAppUrl = targetUrl
                    )
                } catch (e: Exception) {
                    // Handled gracefully in background
                }
                isSending = false
                onSubmitted()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedQrUri = uri
        if (uri != null) {
            isProcessingQr = true
            coroutineScope.launch {
                val res = prepareStandardizedMasjidQr(context, uri)
                standardizedQr = res
                isProcessingQr = false
                if (res != null) {
                    prefs.setCustomMasjidQrPath(res.localFilePath)
                }
            }
        } else {
            standardizedQr = null
            coroutineScope.launch {
                prefs.setCustomMasjidQrPath(null)
            }
        }
    }

    Dialog(
        onDismissRequest = {
            dismissKeyboard()
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogView = androidx.compose.ui.platform.LocalView.current
        val dialogFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val dialogKeyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        LaunchedEffect(dialogView, dialogFocusManager, dialogKeyboardController) {
            dismissKeyboard = {
                dialogFocusManager.clearFocus(force = true)
                dialogKeyboardController?.hide()
                try {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(dialogView.windowToken, 0)
                    imm?.hideSoftInputFromWindow(dialogView.applicationWindowToken, 0)
                    (context as? Activity)?.currentFocus?.let { focused ->
                        imm?.hideSoftInputFromWindow(focused.windowToken, 0)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .imePadding()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { dismissKeyboard() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { dismissKeyboard() },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Masjid",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3DE8E)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color(0xFFF3DE8E), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFF3DE8E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Responsible person notice in bold white text
                Text(
                    text = "The responsible person (Moulana / Mutawalli / Bangi sahab / Trust Member) should submit the form.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                )

                // Field 1: Name of Masjid
                OutlinedTextField(
                    value = masjidName,
                    onValueChange = { 
                        masjidName = toTitleCaseWords(it)
                        errorMessage = null 
                    },
                    enabled = true,
                    readOnly = false,
                    label = { Text("Name of Masjid *") },
                    placeholder = { 
                        Text("e.g. Markaz Masjid, Jama Masjid", color = Color.White.copy(alpha = 0.4f), fontSize = 13.5.sp) 
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { locationFocusRequester.requestFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFF3DE8E),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF131D2A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(masjidNameFocusRequester)
                )

                // Field 2: Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { 
                        location = toTitleCaseWords(it)
                        errorMessage = null 
                    },
                    enabled = true,
                    readOnly = false,
                    label = { Text("Location / City / Area *") },
                    placeholder = { 
                        Text("e.g. Solapur, Muslim Peth", color = Color.White.copy(alpha = 0.4f), fontSize = 13.5.sp) 
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { contactFocusRequester.requestFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFF3DE8E),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF131D2A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(locationFocusRequester)
                )

                // Field 3: Admin Contact Number
                OutlinedTextField(
                    value = adminContact,
                    onValueChange = { input ->
                        val clean = input.replace("\n", "").replace("\r", "")
                        adminContact = clean
                        errorMessage = null
                        if (input.contains("\n") || input.contains("\r")) {
                            dismissKeyboard()
                        }
                    },
                    enabled = true,
                    readOnly = false,
                    label = { Text("Admin Contact Number *") },
                    placeholder = { 
                        Text("e.g. 9876543210", color = Color.White.copy(alpha = 0.4f), fontSize = 13.5.sp) 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { dismissKeyboard() },
                        onSend = { dismissKeyboard() },
                        onSearch = { dismissKeyboard() },
                        onGo = { dismissKeyboard() },
                        onNext = { dismissKeyboard() }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { dismissKeyboard() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done / Hide Keyboard",
                                tint = Color(0xFFF3DE8E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFF3DE8E),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF131D2A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contactFocusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                                dismissKeyboard()
                                true
                            } else {
                                false
                            }
                        }
                )

                // Field 4: Admin ID (Common for all as requested)
                OutlinedTextField(
                    value = "admin",
                    onValueChange = { },
                    label = { Text("Admin ID - admin") },
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3DE8E).copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White.copy(alpha = 0.7f),
                        unfocusedTextColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color(0xFF141E2E),
                        unfocusedContainerColor = Color(0xFF141E2E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Field 5: Photo Browser Button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Masjid Photo (Optional):",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Photo",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPhotoUri != null) "Photo Selected ✓ Change" else "Browse / Upload Masjid Photo",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    if (selectedPhotoUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1A15), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2E533F), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Masjid Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Masjid Photo Selected",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Saved to Google Drive",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                            IconButton(
                                onClick = { selectedPhotoUri = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Photo",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Field 6: QR Code Browser Button (replaces old Azan Time logo & punchline)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Masjid QR (Optional):",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    Button(
                        onClick = {
                            qrPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Upload QR",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedQrUri != null) "QR Selected ✓ Change" else "Browse / Upload Masjid QR",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    if (isProcessingQr) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "Standardizing & Straightening QR...",
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    } else if (standardizedQr != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1A15), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2E533F), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = standardizedQr!!.bitmap.asImageBitmap(),
                                    contentDescription = "Standard QR Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (standardizedQr!!.isCleanRegenerated) "✓ Standard Clean QR Generated" else "✓ Straight Square QR Cropped",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = if (standardizedQr!!.decodedPayload != null)
                                        "Payload: ${standardizedQr!!.decodedPayload!!.take(28)}..."
                                    else
                                        "Standard high-resolution square QR",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedQrUri = null
                                    standardizedQr = null
                                    coroutineScope.launch {
                                        prefs.setCustomMasjidQrPath(null)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove QR",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Submit Request Button (Uploads photo & QR to Google Drive, sets QR in Support Masjid holder, & inserts into Sheet1)
                Button(
                    onClick = { performFormSubmit() },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Uploading...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Request",
                            fontSize = 13.5.sp,
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
fun StarIconDisplay(
    fillRatio: Float, // 0f = outline, 0.5f = half filled, 1f = fully filled
    size: androidx.compose.ui.unit.Dp = 20.dp,
    tint: Color = Color(0xFFFFD700),
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            val cx = this@Canvas.size.width / 2f
            val cy = this@Canvas.size.height / 2f
            val outerR = this@Canvas.size.minDimension / 2f
            val innerR = outerR * 0.42f
            for (p in 0 until 10) {
                val r = if (p % 2 == 0) outerR else innerR
                val angle = Math.toRadians((p * 36 - 90).toDouble())
                val x = cx + (r * Math.cos(angle)).toFloat()
                val y = cy + (r * Math.sin(angle)).toFloat()
                if (p == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        if (fillRatio >= 1f) {
            drawPath(path, color = tint, style = Fill)
        } else if (fillRatio >= 0.5f) {
            // Half-star: clip left half
            clipRect(right = this.size.width / 2f) {
                drawPath(path, color = tint, style = Fill)
            }
            // Full star outline
            drawPath(path, color = tint.copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx()))
        } else {
            // Empty star outline
            drawPath(path, color = tint.copy(alpha = 0.35f), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
fun InteractiveHalfStarRatingBar(
    rating: Double,
    onRatingSelected: (Double) -> Unit,
    starSize: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = Color(0xFFFFD700),
    spacing: androidx.compose.ui.unit.Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val fillRatio = when {
                rating >= i -> 1f
                rating >= i - 0.5 -> 0.5f
                else -> 0f
            }
            Box(
                modifier = Modifier.size(starSize),
                contentAlignment = Alignment.Center
            ) {
                // Drawn Star vector
                StarIconDisplay(
                    fillRatio = fillRatio,
                    size = starSize,
                    tint = tint
                )
                // Left half clickable for (i - 0.5), right half clickable for (i.toDouble())
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onRatingSelected(i - 0.5)
                            }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onRatingSelected(i.toDouble())
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun UpiAppChooserDialog(
    amount: Int,
    onDismiss: () -> Unit,
    onAppSelected: (String?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B15)),
            border = BorderStroke(1.5.dp, Color(0xFFE5B842).copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pay via UPI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3DE8E)
                        )
                        Text(
                            text = "Amount: ₹$amount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF34D399)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Text(
                    text = "Select your payment app:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.Start)
                )

                // 1. Google Pay
                UpiAppOptionItem(
                    name = "Google Pay",
                    subtitle = "GPay UPI",
                    iconRes = R.drawable.ic_gpay,
                    onClick = { onAppSelected("com.google.android.apps.nbu.paisa.user") }
                )

                // 2. PhonePe
                UpiAppOptionItem(
                    name = "PhonePe",
                    subtitle = "PhonePe UPI",
                    iconRes = R.drawable.ic_phonepe,
                    onClick = { onAppSelected("com.phonepe.app") }
                )

                // 3. Paytm
                UpiAppOptionItem(
                    name = "Paytm",
                    subtitle = "Paytm Payments Bank & UPI",
                    iconRes = R.drawable.ic_paytm,
                    onClick = { onAppSelected("net.one97.paytm") }
                )

                // 4. Other UPI App (System Chooser)
                UpiAppOptionItem(
                    name = "Other UPI App",
                    subtitle = "BHIM, Cred, Amazon Pay, etc.",
                    iconRes = R.drawable.ic_upi,
                    onClick = { onAppSelected(null) }
                )
            }
        }
    }
}

@Composable
fun UpiAppOptionItem(
    name: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF122820)),
        border = BorderStroke(1.dp, Color(0xFF1E4636))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Select",
                tint = Color(0xFFF3DE8E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

data class QrButtonTheme(
    val backgroundColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

/**
 * Animated QR Code Button in Support App section
 * Cycles in a continuous top-to-bottom spin loop across 2 distinct color styles:
 * 1. Theme Golden/Yellow background + Black text & icon
 * 2. White background + Black text & icon
 */
/**
 * QR Code Button in Support App section:
 * Fixed Golden background + Black text & icon, slightly rounded corners.
 */
@Composable
fun AnimatedSupportQrButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(88.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3DE8E))
            .border(1.dp, Color(0xFFE5B842), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = "QR Code",
                tint = Color(0xFF071711),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "QR Code",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF071711)
            )
        }
    }
}

/**
 * Social Media Icon Button with title label
 */
@Composable
private fun SocialIconButton(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        icon()
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/**
 * Circular Social Media Button wrapper
 */
@Composable
private fun SocialCircleButton(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Official Facebook Brand Badge
 */
@Composable
private fun SocialFacebookIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(Color(0xFF1877F2)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.offset(x = 0.5.dp, y = (-0.5).dp)
        )
    }
}

/**
 * Official Instagram Brand Badge with Gradient & Camera glyph
 */
@Composable
private fun SocialInstagramIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF833AB4),
                        Color(0xFFFD1D1D),
                        Color(0xFFF77737)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.5.dp)) {
            val strokePx = 1.5.dp.toPx()
            // Outer rounded rect
            drawRoundRect(
                color = Color.White,
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = strokePx)
            )
            // Inner circle
            drawCircle(
                color = Color.White,
                radius = size.width * 0.23f,
                style = Stroke(width = strokePx)
            )
            // Top-right dot
            drawCircle(
                color = Color.White,
                radius = 1.2.dp.toPx(),
                center = Offset(size.width * 0.73f, size.height * 0.27f)
            )
        }
    }
}

/**
 * Official YouTube Brand Badge
 */
@Composable
private fun SocialYouTubeIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(Color(0xFFFF0000)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.5.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.2f)
                lineTo(size.width * 0.82f, size.height * 0.5f)
                lineTo(size.width * 0.32f, size.height * 0.8f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

/**
 * Official Telegram Brand Badge
 */
@Composable
private fun SocialTelegramIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(Color(0xFF229ED9)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.5.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.5f)
                lineTo(size.width * 0.85f, size.height * 0.18f)
                lineTo(size.width * 0.68f, size.height * 0.82f)
                lineTo(size.width * 0.48f, size.height * 0.62f)
                lineTo(size.width * 0.38f, size.height * 0.75f)
                lineTo(size.width * 0.36f, size.height * 0.58f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

/**
 * Single White Colour Social Media Badges
 */
@Composable
private fun WhiteSocialFacebookIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color(0xFF0F1E17),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.offset(x = 0.5.dp, y = (-0.5).dp)
        )
    }
}

@Composable
private fun WhiteSocialInstagramIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.5.dp)) {
            val strokePx = 1.5.dp.toPx()
            val darkColor = Color(0xFF0F1E17)
            drawRoundRect(
                color = darkColor,
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = strokePx)
            )
            drawCircle(
                color = darkColor,
                radius = size.width * 0.23f,
                style = Stroke(width = strokePx)
            )
            drawCircle(
                color = darkColor,
                radius = 1.2.dp.toPx(),
                center = Offset(size.width * 0.73f, size.height * 0.27f)
            )
        }
    }
}

@Composable
private fun WhiteSocialYouTubeIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.5.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.2f)
                lineTo(size.width * 0.82f, size.height * 0.5f)
                lineTo(size.width * 0.32f, size.height * 0.8f)
                close()
            }
            drawPath(path, Color(0xFF0F1E17))
        }
    }
}

@Composable
private fun WhiteSocialTelegramIcon() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(11.5.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.5f)
                lineTo(size.width * 0.85f, size.height * 0.18f)
                lineTo(size.width * 0.68f, size.height * 0.82f)
                lineTo(size.width * 0.48f, size.height * 0.62f)
                lineTo(size.width * 0.38f, size.height * 0.75f)
                lineTo(size.width * 0.36f, size.height * 0.58f)
                close()
            }
            drawPath(path, Color(0xFF0F1E17))
        }
    }
}

private data class ConfettiParticle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val vx: Float,
    val vy: Float,
    val gravity: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean,
    val swayFrequency: Float,
    val swayPhase: Float
)

@Composable
fun ColorfulConfettiOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 90
) {
    val confettiColors = remember {
        listOf(
            Color(0xFFFFD700), // Bright Gold
            Color(0xFFFF3366), // Festive Pink
            Color(0xFF10B981), // Emerald Green
            Color(0xFF00B0FF), // Sky Blue
            Color(0xFFAA00FF), // Vivid Purple
            Color(0xFFFF9100), // Vibrant Orange
            Color(0xFF00E5FF), // Cyan
            Color(0xFFFF5252), // Coral Red
            Color(0xFF76FF03), // Lime Green
            Color(0xFFFFEB3B)  // Lemon Yellow
        )
    }

    val particles = remember {
        val random = java.util.Random(1337)
        List(particleCount) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 160f + random.nextFloat() * 420f
            ConfettiParticle(
                initialXRatio = 0.5f + (random.nextFloat() - 0.5f) * 0.35f,
                initialYRatio = 0.30f + (random.nextFloat() - 0.5f) * 0.2f,
                vx = kotlin.math.cos(angle) * speed,
                vy = (kotlin.math.sin(angle) * speed * 0.7f) - (180f + random.nextFloat() * 220f),
                gravity = 420f + random.nextFloat() * 300f,
                color = confettiColors[random.nextInt(confettiColors.size)],
                width = 12f + random.nextFloat() * 16f,
                height = 6f + random.nextFloat() * 10f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 720f,
                isCircle = random.nextFloat() < 0.25f,
                swayFrequency = 2f + random.nextFloat() * 3f,
                swayPhase = random.nextFloat() * 2f * Math.PI.toFloat()
            )
        }
    }

    var elapsedTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                elapsedTime = (frameTime - startTime) / 1_000_000_000f
            }
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val loopDuration = 4.0f
        val t = elapsedTime % loopDuration
        val alpha = if (t < 2.8f) 1f else (1f - (t - 2.8f) / 1.2f).coerceIn(0f, 1f)

        for (p in particles) {
            val startX = p.initialXRatio * canvasWidth
            val startY = p.initialYRatio * canvasHeight

            val sway = kotlin.math.sin(t * p.swayFrequency + p.swayPhase) * 24f
            val currentX = startX + p.vx * t * 0.65f + sway
            val currentY = startY + p.vy * t + 0.5f * p.gravity * t * t

            if (currentY > canvasHeight + 50f || currentY < -50f) continue

            val currentRotation = p.rotationSpeed * t
            val flutterScale = kotlin.math.cos(t * 5f + p.swayPhase)
            val particleColor = p.color.copy(alpha = alpha)

            rotate(degrees = currentRotation, pivot = androidx.compose.ui.geometry.Offset(currentX, currentY)) {
                if (p.isCircle) {
                    drawCircle(
                        color = particleColor,
                        radius = (p.width / 2.5f) * kotlin.math.abs(flutterScale).coerceAtLeast(0.3f),
                        center = androidx.compose.ui.geometry.Offset(currentX, currentY)
                    )
                } else {
                    val effectiveHeight = (p.height * flutterScale).coerceIn(-p.height, p.height)
                    drawRect(
                        color = particleColor,
                        topLeft = androidx.compose.ui.geometry.Offset(currentX - p.width / 2f, currentY - effectiveHeight / 2f),
                        size = androidx.compose.ui.geometry.Size(p.width, kotlin.math.abs(effectiveHeight).coerceAtLeast(2.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun SupportGridItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241A)),
        border = BorderStroke(1.dp, Color(0xFFE5B842).copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF143827), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFF3DE8E),
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3DE8E),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF143827))
                    .border(
                        1.2.dp, 
                        if (actionText != null) Color(0xFF10B981).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f), 
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionText ?: "Open",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3DE8E),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



