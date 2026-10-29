package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.MasjidItem
import com.example.data.getLocalizedArea
import com.example.data.getLocalizedName
import com.example.LanguageCirclesRow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale


@Composable
fun FirstTimeSetupScreen(
    uiState: UIState,
    onSelectMasjid: (MasjidItem) -> Unit,
    onFinishSetup: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    onRefresh: () -> Unit = {},
    onLanguageSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = com.example.ui.theme.getAppStrings(uiState.language)
    val isChangeMode = onBack != null

    // Auto-refresh masajid list from Google Sheet whenever Search/Setup screen opens
    LaunchedEffect(Unit) {
        onRefresh()
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    // Starts as null so FINISH button is disabled until a slot is selected from search results
    var userSelectedMasjid by remember {
        mutableStateOf<MasjidItem?>(null)
    }

    var showCantFindMasjidDialog by remember { mutableStateOf(false) }
    var showAddMasjidDialog by remember { mutableStateOf(false) }
    var selectedMasjidToConfirm by remember { mutableStateOf<MasjidItem?>(null) }

    val hasQuery = searchQuery.trim().isNotBlank()

    // System Back handling
    BackHandler {
        if (hasQuery) {
            searchQuery = ""
        } else if (onBack != null) {
            onBack()
        }
    }

    // Speech to text activity launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()?.trim()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                isSearchFocused = true
            }
        }
    }

    // Audio permission launcher for speech recognition
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceSearch(context, speechLauncher)
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed for voice search",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startVoiceInput() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchVoiceSearch(context, speechLauncher)
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Matching masajid list (returns non-guest masajid list when search query is empty, or filtered list when searching)
    val matchedList = remember(searchQuery, uiState.allMasajid, uiState.language, hasQuery) {
        val nonGuestList = uiState.allMasajid.filterNot { m ->
            m.id.equals("guest", ignoreCase = true) ||
            m.name.contains("guest", ignoreCase = true) ||
            m.name.contains("गेस्ट", ignoreCase = true) ||
            m.name.contains("گیسٹ", ignoreCase = true)
        }
        if (!hasQuery) {
            nonGuestList
        } else {
            val q = searchQuery.trim().lowercase()
            nonGuestList.filter { m ->
                m.name.lowercase().contains(q) ||
                m.area.lowercase().contains(q) ||
                m.city.lowercase().contains(q) ||
                m.state.lowercase().contains(q) ||
                m.id.lowercase().contains(q) ||
                m.getLocalizedName(uiState.language).lowercase().contains(q) ||
                m.getLocalizedArea(uiState.language).lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14))
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val logoSectionHeight = (screenHeight * 0.30f).coerceIn(170.dp, 240.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 1. TOP BAR: Language Selection Buttons (Exact same Home Page buttons: E, ह, ر)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageCirclesRow(
                    currentLang = uiState.language,
                    onLangSelect = onLanguageSelect
                )
            }

            // 2. 30% OF SCREEN HEIGHT: AZAN TIME Logo & Punchline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(logoSectionHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Golden Circular Medallion with Mosque Logo
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0F2B1D),
                                        Color(0xFF071710),
                                        Color(0xFF020705)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                BorderStroke(
                                    2.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFF3DE8E),
                                            Color(0xFFF2CA50),
                                            Color(0xFFB37C3C)
                                        )
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.foreground),
                            contentDescription = "Azan Time Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    var headerWidthDp by remember { mutableStateOf<androidx.compose.ui.unit.Dp?>(null) }
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val curStrings = com.example.ui.theme.getAppStrings(uiState.language)

                    Text(
                        text = if (uiState.language == "ur") curStrings.azanTimeHeader else "AZAN TIME",
                        fontFamily = if (uiState.language == "ur") null else GraenMetalFontFamily,
                        fontSize = if (uiState.language == "ur") 26.sp else 32.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            brush = GraenMetalGoldGradient,
                            shadow = GraenMetalGoldShadow
                        ),
                        onTextLayout = { layoutResult ->
                            val w = with(density) { layoutResult.size.width.toDp() }
                            if (headerWidthDp != w) {
                                headerWidthDp = w
                            }
                        }
                    )

                    Box(
                        modifier = if (headerWidthDp != null) Modifier.width(headerWidthDp!!) else Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (curStrings.punchLine == "Come towards the path of Allah") curStrings.punchLine.uppercase() else curStrings.punchLine,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFFF2CA50).copy(alpha = 0.95f),
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. SEARCH BAR
            SearchBarContent(
                searchQuery = searchQuery,
                placeholder = strings.searchPlaceholder,
                onSearchQueryChange = {
                    searchQuery = it
                    isSearchFocused = true
                },
                onSearchFocused = { isSearchFocused = true },
                onVoiceInput = {
                    isSearchFocused = true
                    startVoiceInput()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. SCROLLABLE MIDDLE SECTION (Matching masajid heading & list slide smoothly underneath search bar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                SearchResultsList(
                    matchedList = matchedList,
                    searchQuery = searchQuery,
                    selectedMasjidId = selectedMasjidToConfirm?.id ?: userSelectedMasjid?.id,
                    language = uiState.language,
                    onSelect = { item -> selectedMasjidToConfirm = item }
                )
                // Ensures the very last masjid slot is completely and cleanly visible
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. FIXED BOTTOM FOOTER: "Can't find your Masjid?" Button
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showCantFindMasjidDialog = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, Color(0xFFF2CA50).copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF131D2A).copy(alpha = 0.95f),
                    contentColor = Color(0xFFF2CA50)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mosque,
                    contentDescription = null,
                    tint = Color(0xFFF2CA50),
                    modifier = Modifier
                        .size(19.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = when (uiState.language) {
                        "ur" -> "مسجد نہیں مل رہی؟"
                        "hi" -> "मस्जिद नहीं मिल रही?"
                        else -> "Can't find your Masjid?"
                    },
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF2CA50)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // POPUP DIALOG: Selected Masjid Confirmation with Photo/Logo Card + CANCEL and PROCEED
        val masjidToConfirm = selectedMasjidToConfirm
        if (masjidToConfirm != null) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val photoHeight = (screenHeight * 0.40f).coerceAtLeast(280.dp)

            Dialog(
                onDismissRequest = { selectedMasjidToConfirm = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F151C)),
                    border = BorderStroke(1.5.dp, Color(0xFFF2CA50).copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. TOP: Only Masjid Photo (Taking at least 40% of screen height)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(photoHeight)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(Color(0xFF0A0F14))
                        ) {
                            if (!masjidToConfirm.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(masjidToConfirm.photoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = masjidToConfirm.getLocalizedName(uiState.language),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Fallback Mosque Illustration Background
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Mosque,
                                            contentDescription = null,
                                            tint = Color(0xFFF2CA50).copy(alpha = 0.50f),
                                            modifier = Modifier.size(80.dp)
                                        )
                                        Text(
                                            text = masjidToConfirm.getLocalizedName(uiState.language),
                                            fontSize = 15.sp,
                                            color = Color(0xFFD0C5AF).copy(alpha = 0.70f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. BOTTOM: Details and Buttons
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Masjid Name and ID badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = masjidToConfirm.getLocalizedName(uiState.language),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFFF2CA50).copy(alpha = 0.40f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "#${masjidToConfirm.id}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF2CA50)
                                    )
                                }
                            }

                            // Location
                            val loc = listOf(
                                masjidToConfirm.getLocalizedArea(uiState.language),
                                masjidToConfirm.city,
                                masjidToConfirm.state
                            ).filter { it.isNotBlank() }.joinToString(", ")

                            if (loc.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = loc,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Action Buttons (CANCEL & PROCEED)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // CANCEL BUTTON
                                OutlinedButton(
                                    onClick = { selectedMasjidToConfirm = null },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = when (uiState.language) {
                                            "ur" -> "منسوخ کریں (Cancel)"
                                            "hi" -> "रद्द करें (Cancel)"
                                            else -> "Cancel"
                                        },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // PROCEED BUTTON
                                Button(
                                    onClick = {
                                        val chosen = selectedMasjidToConfirm!!
                                        onSelectMasjid(chosen)
                                        onFinishSetup(chosen.id)
                                        selectedMasjidToConfirm = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF2CA50),
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = strings.finishButton,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // POPUP DIALOG: "Can't find your Masjid?" -> GUEST & REQUEST MASJID
        if (showCantFindMasjidDialog) {
            Dialog(
                onDismissRequest = { showCantFindMasjidDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, Color(0xFFF2CA50).copy(alpha = 0.50f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Top Right Close (X) Button
                        IconButton(
                            onClick = { showCantFindMasjidDialog = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Replace mosque icon with exact same Language Circles Row from home page
                            LanguageCirclesRow(
                                currentLang = uiState.language,
                                onLangSelect = onLanguageSelect
                            )

                            Text(
                                text = when (uiState.language) {
                                    "ur" -> "مسجد نہیں مل رہی؟"
                                    "hi" -> "मस्जिद नहीं मिल रही?"
                                    else -> "Can't find your Masjid?"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            // Numbered lines with bold Guest Masjid and Request us
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        append("1. ")
                                        append(
                                            when (uiState.language) {
                                                "ur" -> "آپ "
                                                "hi" -> "आप "
                                                else -> "You can continue in "
                                            }
                                        )
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFF2CA50))) {
                                            append(
                                                when (uiState.language) {
                                                    "ur" -> "گیسٹ مسجد (Guest Masjid)"
                                                    "hi" -> "गेस्ट मस्जिद (Guest Masjid)"
                                                    else -> "Guest Masjid"
                                                }
                                            )
                                        }
                                        append(
                                            when (uiState.language) {
                                                "ur" -> " میں ڈیفالٹ اوقات کے ساتھ جاری رکھ سکتے ہیں۔"
                                                "hi" -> " में डिफ़ॉल्ट समय के साथ आगे बढ़ सकते हैं।"
                                                else -> " with default timings."
                                            }
                                        )
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFFCBD5E1)
                                )

                                Text(
                                    text = buildAnnotatedString {
                                        append("2. ")
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFF2CA50))) {
                                            append(
                                                when (uiState.language) {
                                                    "ur" -> "ہم سے درخواست کریں"
                                                    "hi" -> "हमसे अनुरोध करें"
                                                    else -> "Request us"
                                                }
                                            )
                                        }
                                        append(
                                            when (uiState.language) {
                                                "ur" -> " کہ آپ کی مسجد فہرست میں شامل کریں۔ ذمہ دار شخص (مولانا / متولی / بنگی صاحب / ٹرسٹ ممبر) کو یہ فارم جمع کرنا چاہیے۔"
                                                "hi" -> " कि आपकी मस्जिद सूची में जोड़ी जाए। जिम्मेदार व्यक्ति (मौलाना / मुतवल्ली / बंगी साहब / ट्रस्ट मेंबर) को ही यह फॉर्म भरना चाहिए।"
                                                else -> " to add your Masjid to the list. The responsible person (Moulana / Mutawalli / Bangi sahab / Trust Member) should submit the form."
                                            }
                                        )
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // VERTICAL STACK OF NEUTRAL BUTTONS: GUEST MASJID (TOP), THEN REQUEST MASJID (BOTTOM)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. GUEST MASJID BUTTON (TOP) - NEUTRAL UNHIGHLIGHTED
                                OutlinedButton(
                                    onClick = {
                                        showCantFindMasjidDialog = false
                                        val guestMasjid = uiState.allMasajid.firstOrNull {
                                            it.id.equals("guest", ignoreCase = true) ||
                                            it.name.contains("guest", ignoreCase = true) ||
                                            it.name.contains("गेस्ट", ignoreCase = true) ||
                                            it.name.contains("گیسٹ", ignoreCase = true)
                                        } ?: userSelectedMasjid ?: uiState.allMasajid.firstOrNull() ?: uiState.selectedMasjid
                                        onSelectMasjid(guestMasjid)
                                        onFinishSetup(guestMasjid.id)
                                    },
                                    border = BorderStroke(1.2.dp, Color(0xFFF2CA50).copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF131D2A).copy(alpha = 0.95f),
                                        contentColor = Color(0xFFF2CA50)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = when (uiState.language) {
                                            "ur" -> "گیسٹ مسجد (Guest Masjid)"
                                            "hi" -> "गेस्ट मस्जिद (Guest Masjid)"
                                            else -> "Guest Masjid"
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // 2. REQUEST MASJID BUTTON (BOTTOM) - NEUTRAL UNHIGHLIGHTED
                                OutlinedButton(
                                    onClick = {
                                        showCantFindMasjidDialog = false
                                        showAddMasjidDialog = true
                                    },
                                    border = BorderStroke(1.2.dp, Color(0xFFF2CA50).copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF131D2A).copy(alpha = 0.95f),
                                        contentColor = Color(0xFFF2CA50)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = when (uiState.language) {
                                            "ur" -> "مسجد شامل کرنے کی درخواست دیں"
                                            "hi" -> "नई मस्जिद जोड़ने की दरखास्त करें"
                                            else -> "Request Add Masjid"
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // REQUEST ADD MASJID FORM DIALOG
        if (showAddMasjidDialog) {
            AddMasjidAutoDialog(
                language = uiState.language,
                onDismiss = { showAddMasjidDialog = false },
                onSubmitted = {
                    showAddMasjidDialog = false
                }
            )
        }
    }
}

/**
 * Reusable Search Bar Component
 */
@Composable
private fun SearchBarContent(
    searchQuery: String,
    placeholder: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchFocused: () -> Unit,
    onVoiceInput: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                Color(0xFF0F172A).copy(alpha = 0.90f),
                RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                Color(0xFFF2CA50).copy(alpha = 0.45f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onSearchFocused() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFFF2CA50),
                modifier = Modifier
                    .padding(start = 8.dp, end = 10.dp)
                    .size(22.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF94A3B8).copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFFF2CA50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                onSearchFocused()
                            }
                        }
                )
            }

            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFFD0C5AF).copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Color(0xFFF2CA50).copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        Color(0xFFF2CA50).copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onVoiceInput() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice search",
                    tint = Color(0xFFF2CA50),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Reusable Search Results List
 */
@Composable
private fun SearchResultsList(
    matchedList: List<MasjidItem>,
    searchQuery: String,
    selectedMasjidId: String?,
    language: String,
    onSelect: (MasjidItem) -> Unit
) {
    val strings = com.example.ui.theme.getAppStrings(language)
    if (matchedList.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.90f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFF2CA50).copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mosque,
                    contentDescription = null,
                    tint = Color(0xFFF2CA50).copy(alpha = 0.65f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = java.lang.String.format(strings.noMasjidFoundTitle, searchQuery.trim()),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.noMasjidFoundHint,
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = java.lang.String.format(strings.matchingMasajid, matchedList.size),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFF2CA50),
                letterSpacing = 0.8.sp
            )
            Text(
                text = strings.tapToSelect,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        matchedList.forEach { item ->
            Card(
                onClick = { onSelect(item) },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F151C).copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFF2CA50).copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.getLocalizedName(language),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        val itemLoc = listOf(
                            item.getLocalizedArea(language),
                            item.city,
                            item.state
                        ).filter { it.isNotBlank() }.joinToString(", ")
                        if (itemLoc.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = itemLoc,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFD0C5AF).copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF1E2836).copy(alpha = 0.8f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                Color(0xFFF2CA50).copy(alpha = 0.35f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${item.id}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFF2CA50)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to launch speech recognition intent
 */
private fun launchVoiceSearch(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak Masjid Name, Location or ID..."
            )
        }
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Speech recognition is not available on this device",
            Toast.LENGTH_SHORT
        ).show()
    }
}
