package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.core.content.ContextCompat
import java.util.Locale
import com.example.LanguageCirclesRow
import com.example.data.MasjidItem
import com.example.data.getLocalizedName
import com.example.data.getLocalizedArea
import com.example.data.getLocalizedCity
import com.example.data.masjidTranslations

private class MasjidSelectorStrings(val lang: String) {
    val timetableHeader = when (lang) {
        "ur" -> "مسجد نظام الاوقات"
        "hi" -> "मस्जिद समय सारणी"
        else -> "MASJID TIMETABLE"
    }
    val countBadge = when (lang) {
        "ur" -> "100+ مساجد"
        "hi" -> "100+ मस्जिदें"
        else -> "100+ Masajid"
    }
    val changeBtn = when (lang) {
        "ur" -> "تبدیل کریں"
        "hi" -> "बदलें"
        else -> "Change"
    }
    val selectMasjidTitle = when (lang) {
        "ur" -> "مسجد منتخب کریں"
        "hi" -> "मस्जिद चुनें"
        else -> "SELECT MASJID"
    }
    val selectMasjidSubtitle = when (lang) {
        "ur" -> "مساجد مقامی نماز کے اوقات کے ساتھ"
        "hi" -> "मस्जिदें स्थानीय नमाज़ समय सारणी के साथ"
        else -> "Masajid with Local Namaz Timetable"
    }
    val searchPlaceholder = when (lang) {
        "ur" -> "مسجد کا نام، علاقہ یا ID تلاش کریں..."
        "hi" -> "मस्जिद का नाम, इलाका या ID खोजें..."
        else -> "Search by masjid name, area or ID..."
    }
    fun showingCount(count: Int): String = when (lang) {
        "ur" -> "$count مساجد دکھائی جا رہی ہیں"
        "hi" -> "$count मस्जिदें दिखाई जा रही हैं"
        else -> "Showing $count Masajid"
    }
    val jumahLabel = when (lang) {
        "ur" -> "جمعہ"
        "hi" -> "जुमा"
        else -> "Jum'ah"
    }
    val azanLabel = when (lang) {
        "ur" -> "اذان"
        "hi" -> "अज़ान"
        else -> "Azan"
    }
    val jammatLabel = when (lang) {
        "ur" -> "جماعت"
        "hi" -> "जमात"
        else -> "Jammat"
    }
    val areaLabel = when (lang) {
        "ur" -> "مقام"
        "hi" -> "स्थान"
        else -> "Location"
    }
    val noResults = when (lang) {
        "ur" -> "کوئی مسجد نہیں ملی"
        "hi" -> "कोई मस्जिद नहीं मिली"
        else -> "No masjid found matching search"
    }
    val confirmTitle = when (lang) {
        "ur" -> "مسجد کے انتخاب کی تصدیق"
        "hi" -> "मस्जिद चयन की पुष्टि करें"
        else -> "Confirm Masjid Selection"
    }
    val confirmBtn = when (lang) {
        "ur" -> "ہاں / تصدیق کریں"
        "hi" -> "हाँ / पुष्टि करें"
        else -> "Confirm"
    }
    val cancelBtn = when (lang) {
        "ur" -> "منسوخ کریں"
        "hi" -> "रद्द करें"
        else -> "Cancel"
    }
    val micPrompt = when (lang) {
        "ur" -> "مسجد کا نام یا علاقہ بولیں..."
        "hi" -> "मस्जिद का नाम या इलाका बोलें..."
        else -> "Speak masjid name, area or ID..."
    }
    val micPermissionToast = when (lang) {
        "ur" -> "آواز سے تلاش کے لیے مائیکروفون کی اجازت ضروری ہے"
        "hi" -> "आवाज़ से खोजने के लिए माइक की अनुमति आवश्यक है"
        else -> "Microphone permission is needed for voice search"
    }
    val noSpeechSupportToast = when (lang) {
        "ur" -> "اس ڈیوائس پر آواز کی شناخت کی سہولت نہیں ہے"
        "hi" -> "इस डिवाइस पर आवाज़ से खोजना समर्थित नहीं है"
        else -> "Speech recognition is not supported on this device"
    }
}

@Composable
fun MasjidSelectorDropdown(
    selectedMasjid: MasjidItem,
    allMasajid: List<MasjidItem>,
    language: String,
    onSelectMasjid: (MasjidItem) -> Unit,
    onLanguageSelect: (String) -> Unit,
    onChangeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = remember(language) { MasjidSelectorStrings(language) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (onChangeClick != null) onChangeClick() else showDialog = true
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091712)),
        border = BorderStroke(1.dp, Color(0xFF1B3B2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Mosque Icon + Masjid Name and Location below it
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF162D22), CircleShape)
                        .border(1.dp, Color(0xFF2E533F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mosque,
                        contentDescription = "Mosque",
                        tint = Color(0xFFF3DE8E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = selectedMasjid.getLocalizedName(language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF3DE8E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val locArea = selectedMasjid.getLocalizedArea(language).ifBlank { selectedMasjid.area }
                    val locCity = selectedMasjid.getLocalizedCity(language).ifBlank { selectedMasjid.city }
                    val addressText = when {
                        locArea.isNotBlank() && locCity.isNotBlank() && !locArea.contains(locCity, ignoreCase = true) ->
                            "$locArea, $locCity"
                        locArea.isNotBlank() -> locArea
                        locCity.isNotBlank() -> locCity
                        else -> ""
                    }

                    if (addressText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Location",
                                tint = Color(0xFFE5A93C),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = addressText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Right: Change Button in the exact same line
            Surface(
                color = Color(0xFF162D22),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f)),
                modifier = Modifier.clickable {
                    if (onChangeClick != null) onChangeClick() else showDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = strings.changeBtn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3DE8E)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select",
                        tint = Color(0xFFF3DE8E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        MasjidSelectionDialog(
            selectedMasjid = selectedMasjid,
            allMasajid = allMasajid,
            language = language,
            onSelectMasjid = {
                onSelectMasjid(it)
                showDialog = false
            },
            onLanguageSelect = onLanguageSelect,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun MasjidSelectionDialog(
    selectedMasjid: MasjidItem,
    allMasajid: List<MasjidItem>,
    language: String,
    onSelectMasjid: (MasjidItem) -> Unit,
    onLanguageSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var pendingSelectedMasjid by remember { mutableStateOf<MasjidItem?>(null) }
    val strings = remember(language) { MasjidSelectorStrings(language) }

    // Speech to text activity launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()?.trim()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
            }
        }
    }

    // Audio permission launcher for speech recognition
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceSearch(context, speechLauncher, strings.micPrompt)
        } else {
            Toast.makeText(
                context,
                strings.micPermissionToast,
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
            launchVoiceSearch(context, speechLauncher, strings.micPrompt)
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val filteredMasajid = remember(searchQuery, allMasajid, language) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            allMasajid
        } else {
            allMasajid.filter { m ->
                val nameEn = m.name.lowercase().trim()
                val nameUr = m.getLocalizedName("ur").lowercase().trim()
                val nameHi = m.getLocalizedName("hi").lowercase().trim()
                val idStr = m.id.lowercase().trim()

                if (q.length <= 2) {
                    // For short queries (1 or 2 characters, like "y"):
                    // Only match if masjid name is exact, starts with query, has a word starting with query,
                    // or ID is exact or starts with query. Never match middle characters in city/area!
                    nameEn == q ||
                    nameEn.startsWith(q) ||
                    nameEn.split(Regex("[\\s-_,]+")).any { it.startsWith(q) } ||
                    nameUr == q ||
                    nameUr.startsWith(q) ||
                    nameUr.split(Regex("[\\s-_,]+")).any { it.startsWith(q) } ||
                    nameHi == q ||
                    nameHi.startsWith(q) ||
                    nameHi.split(Regex("[\\s-_,]+")).any { it.startsWith(q) } ||
                    idStr == q ||
                    idStr.startsWith(q)
                } else {
                    // For longer queries (3+ characters):
                    val areaEn = m.area.lowercase()
                    val areaUr = m.getLocalizedArea("ur").lowercase()
                    val areaHi = m.getLocalizedArea("hi").lowercase()
                    val cityEn = m.city.lowercase()
                    val cityUr = m.getLocalizedCity("ur").lowercase()
                    val cityHi = m.getLocalizedCity("hi").lowercase()
                    val stateStr = m.state.lowercase()

                    nameEn.contains(q) || nameUr.contains(q) || nameHi.contains(q) ||
                    idStr.contains(q) ||
                    areaEn.split(Regex("[\\s-_,]+")).any { it.startsWith(q) } ||
                    areaUr.contains(q) || areaHi.contains(q) ||
                    cityEn.startsWith(q) || cityUr.contains(q) || cityHi.contains(q) ||
                    stateStr.startsWith(q)
                }
            }.sortedWith(compareBy<MasjidItem> { m ->
                val nEn = m.name.lowercase().trim()
                val idS = m.id.lowercase().trim()
                when {
                    nEn == q -> 0
                    nEn.startsWith(q) -> 1
                    nEn.split(Regex("[\\s-_,]+")).any { it.startsWith(q) } -> 2
                    nEn.contains(q) -> 3
                    idS == q -> 4
                    idS.startsWith(q) -> 5
                    idS.contains(q) -> 6
                    else -> 7
                }
            })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(1.5.dp, Color(0xFFE5A93C).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .border(1.dp, Color(0xFFF3DE8E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mosque,
                                contentDescription = null,
                                tint = Color(0xFFF3DE8E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.selectMasjidTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF3DE8E),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = strings.selectMasjidSubtitle,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar with Mic for Speak-to-Type
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            strings.searchPlaceholder,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFF3DE8E),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { startVoiceInput() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Speak to type",
                                    tint = Color(0xFFF3DE8E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF172033),
                        unfocusedContainerColor = Color(0xFF172033)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageCirclesRow(currentLang = language, onLangSelect = onLanguageSelect)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = strings.showingCount(filteredMasajid.size),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )

                // List of Masajid
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMasajid, key = { it.id }) { masjid ->
                        val isCurrent = masjid.id == selectedMasjid.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingSelectedMasjid = masjid },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFF1E2B45) else Color(0xFF141C2E)
                            ),
                            border = BorderStroke(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) Color(0xFFF3DE8E) else Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                if (isCurrent) Color(0xFFF3DE8E).copy(alpha = 0.15f) else Color(0xFF1E293B),
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                if (isCurrent) Color(0xFFF3DE8E) else Color.White.copy(alpha = 0.15f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Mosque,
                                            contentDescription = null,
                                            tint = if (isCurrent) Color(0xFFF3DE8E) else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        // 1. Masjid Name
                                        Text(
                                            text = masjid.getLocalizedName(language),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color(0xFFF3DE8E) else Color.White
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        // 2. Location
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFFE5A93C),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${strings.areaLabel}: ${masjid.getLocalizedArea(language)}, ${masjid.getLocalizedCity(language)}",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // 3. ID Tag
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            val idPrefix = when (language) {
                                                "ur" -> "آئی ڈی"
                                                "hi" -> "आईडी"
                                                else -> "ID"
                                            }
                                            Text(
                                                text = "$idPrefix: #${masjid.id}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF3DE8E)
                                            )
                                        }
                                    }
                                }

                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFE5A93C), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Selected Masjid Confirmation with Photo/Logo Card + CANCEL and CONFIRM (matches Setup Screen popup)
    val targetMasjid = pendingSelectedMasjid
    if (targetMasjid != null) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val photoHeight = (screenHeight * 0.40f).coerceAtLeast(280.dp)

        Dialog(
            onDismissRequest = { pendingSelectedMasjid = null },
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
                        if (!targetMasjid.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(targetMasjid.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = targetMasjid.getLocalizedName(language),
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
                                        text = targetMasjid.getLocalizedName(language),
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
                                text = targetMasjid.getLocalizedName(language),
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
                                    text = "#${targetMasjid.id}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF2CA50)
                                )
                            }
                        }

                        // Location
                        val loc = listOf(
                            targetMasjid.getLocalizedArea(language),
                            targetMasjid.city,
                            targetMasjid.state
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

                        // Action Buttons (CANCEL & CONFIRM)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // CANCEL BUTTON
                            OutlinedButton(
                                onClick = { pendingSelectedMasjid = null },
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
                                    text = "Cancel",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // CONFIRM BUTTON
                            Button(
                                onClick = {
                                    val m = targetMasjid
                                    pendingSelectedMasjid = null
                                    onSelectMasjid(m)
                                    onDismiss()
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
                                    text = "CONFIRM",
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
}

/**
 * Helper to launch speech recognition intent for voice search
 */
private fun launchVoiceSearch(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    promptText: String = "Speak masjid name, area or ID..."
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
                promptText
            )
        }
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Speech recognition is not supported on this device",
            Toast.LENGTH_SHORT
        ).show()
    }
}
