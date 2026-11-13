package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.FirstTimeSetupScreen
import com.example.ui.InitialLanguageScreen
import com.example.ui.AiStudioLoadingFooter
import com.example.ui.SplashScreen
import com.example.data.GoogleSheetMasjidSync
import com.example.util.AppUpdateManager
import com.example.util.UpdateInfo
import com.example.ui.UpdateDialog
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.prepareStandardizedMasjidQr
import com.example.ui.prepareStraightMasjidPhotoBase64
import com.example.ui.StandardizedQrResult
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AzanDatabase
import com.example.data.AzanRepository
import com.example.data.PreferencesRepository
import com.example.service.AlarmScheduler
import com.example.ui.AzanViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.core.rememberInfiniteTransition
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloat
import com.example.data.MasjidItem
import com.example.data.getLocalizedName
import com.example.data.getLocalizedArea
import com.example.data.getLocalizedCity
import com.example.data.getLocalizedFullLocation
import com.example.data.getStopLabel
import com.example.data.getMuteLabel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.lifecycleScope
import com.example.ui.MasjidSelectorDropdown
import com.example.ui.MasjidSelectionDialog

class MainActivity : ComponentActivity() {

    private var mainViewModel: AzanViewModel? = null

    override fun onResume() {
        super.onResume()
        ensureSystemScreenTimeout()
        mainViewModel?.refreshDate()
    }

    private fun ensureSystemScreenTimeout() {
        try {
            @Suppress("DEPRECATION")
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureSystemScreenTimeout()
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            try {
                com.example.service.AzanForegroundService.initFromPrefs(this@MainActivity)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            try {
                com.example.worker.PrayerWorkScheduler.scheduleDailySync(applicationContext)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val imageLoader = coil.ImageLoader.Builder(applicationContext)
                    .memoryCache {
                        coil.memory.MemoryCache.Builder(applicationContext)
                            .maxSizePercent(0.30)
                            .build()
                    }
                    .diskCache {
                        coil.disk.DiskCache.Builder()
                            .directory(applicationContext.cacheDir.resolve("image_cache"))
                            .maxSizeBytes(100L * 1024 * 1024)
                            .build()
                    }
                    .respectCacheHeaders(false)
                    .crossfade(true)
                    .build()
                coil.Coil.setImageLoader(imageLoader)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            
            // 1. Asynchronously initialize ViewModel and its dependencies on IO dispatcher in parallel
            val viewModelState = androidx.compose.runtime.produceState<AzanViewModel?>(initialValue = null) {
                val vm = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val db = AzanDatabase.getDatabase(context)
                    val repo = AzanRepository(context, db.azanDao(), db.prayerLogDao())
                    val prefs = PreferencesRepository(context)
                    val scheduler = AlarmScheduler(context)
                    AzanViewModel(context.applicationContext, repo, prefs, scheduler)
                }
                value = vm
                mainViewModel = vm
            }
            val viewModel = viewModelState.value

            MyApplicationTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                // Lock fontScale to 1.0f
                val currentDensity = LocalDensity.current
                val fixedDensity = remember(currentDensity.density) {
                    Density(density = currentDensity.density, fontScale = 1.0f)
                }

                CompositionLocalProvider(LocalDensity provides fixedDensity) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showSplash) {
                            SplashScreen(onFinished = { 
                                showSplash = false 
                            })
                        } else if (viewModel != null) {
                            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                            val strings = when (uiState.language) {
                                "hi" -> HindiStrings
                                "ur" -> UrduStrings
                                else -> EnglishStrings
                            }
                            
                            CompositionLocalProvider(LocalAppStrings provides strings) {
                                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                                        ) { }
                                        LaunchedEffect(Unit) {
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    }
                                    
                                    AzanScreen(
                                        viewModel = viewModel,
                                        uiState = uiState,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ensureSystemScreenTimeout()
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (com.example.service.AzanForegroundService.isPlayingAzan.value) {
            if (keyCode == android.view.KeyEvent.KEYCODE_POWER ||
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                com.example.service.AzanForegroundService.stopService(this)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

}

@Composable
fun AzanScreen(viewModel: AzanViewModel, uiState: com.example.ui.UIState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isRamazanActive = remember {
        val current = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(2027, java.util.Calendar.FEBRUARY, 5, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        current.timeInMillis >= target.timeInMillis
    }

    var selectedTab by remember { mutableStateOf("home") } // "support", "home", or "ramazan"
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var showMasjidSelectorScreen by remember { mutableStateOf(false) }

    var ramazanTapCount by remember { mutableIntStateOf(0) }
    var forcedRamazanActive by remember { mutableStateOf(false) }
    val effectiveRamazanActive = isRamazanActive || forcedRamazanActive

    val prefersReducedMotion = com.example.ui.rememberPrefersReducedMotion()
    val pageGraphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val tabTargets = remember { mutableStateMapOf<String, com.example.ui.TabTarget>() }
    var isGenieBusy by remember { mutableStateOf(false) }
    var outgoingTab by remember { mutableStateOf<String?>(null) }
    var outgoingBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val genieProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val triggerTabSwitch: (String) -> Unit = { targetTab ->
        if (!isGenieBusy && targetTab != selectedTab) {
            val prevTab = selectedTab
            isGenieBusy = true
            coroutineScope.launch {
                val snapshot = try {
                    pageGraphicsLayer.toImageBitmap().asAndroidBitmap()
                } catch (_: Exception) {
                    null
                }

                outgoingTab = prevTab
                outgoingBitmap = snapshot
                selectedTab = targetTab
                if (targetTab == "home") {
                    viewModel.selectToday()
                }

                genieProgress.snapTo(0f)
                genieProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = if (prefersReducedMotion) 240 else com.example.ui.GENIE_DUR.toInt(),
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                )

                outgoingTab = null
                outgoingBitmap = null
                isGenieBusy = false
            }
        }
    }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var availableUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val update = AppUpdateManager.checkForUpdate()
        if (update != null && update.hasUpdate) {
            availableUpdateInfo = update
            showUpdateDialog = true
        }
    }

    LaunchedEffect(effectiveRamazanActive) {
        if (!effectiveRamazanActive && selectedTab == "ramazan") {
            selectedTab = "home"
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.syncGoogleSheet()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val strings = getAppStrings(uiState.language)
    val homeLabel = when (uiState.language) {
        "ur" -> "ہوم"
        "hi" -> "होम"
        else -> "Home"
    }
    val supportLabel = strings.setting
    val ramazanLabel = when (uiState.language) {
        "ur" -> "رمضان"
        "hi" -> "रमज़ान"
        else -> "Ramazan"
    }

    var forceShowSetupAfterLanguage by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!uiState.isInitialLanguageChosen || (!uiState.isSetupCompleted || forceShowSetupAfterLanguage)) {
            // Layer 1: Setup screen rendered directly underneath in the background
            FirstTimeSetupScreen(
                uiState = uiState,
                onSelectMasjid = { masjid -> viewModel.selectMasjid(masjid.id) },
                onFinishSetup = { masjidId ->
                    viewModel.completeSetup(masjidId)
                    forceShowSetupAfterLanguage = false
                },
                onRefresh = { viewModel.syncGoogleSheet() },
                onLanguageSelect = { lang -> viewModel.setLanguage(lang) }
            )

            // Layer 2: Initial Language Screen overlay on top (slides off to reveal setup screen)
            if (!uiState.isInitialLanguageChosen) {
                InitialLanguageScreen(
                    onLanguagePreselected = { lang ->
                        viewModel.setLanguage(lang)
                    },
                    onLanguageSelect = { lang ->
                        viewModel.setLanguage(lang)
                        viewModel.setInitialLanguageChosen(true)
                        forceShowSetupAfterLanguage = true
                    }
                )
            }
        } else if (showAdminPanel) {
            BackHandler { showAdminPanel = false }
            AdminPanelScreen(
                viewModel = viewModel,
                uiState = uiState,
                onBack = { showAdminPanel = false }
            )
        } else {
            if (showUpdateDialog && availableUpdateInfo != null) {
                UpdateDialog(
                    updateInfo = availableUpdateInfo!!,
                    language = uiState.language,
                    onDismiss = { showUpdateDialog = false }
                )
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Black,
            bottomBar = {
                com.example.ui.AnimatedCurvedNavBar(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        triggerTabSwitch(tab)
                    },
                    supportLabel = supportLabel,
                    homeLabel = homeLabel,
                    ramazanLabel = ramazanLabel,
                    isRamazanEnabled = effectiveRamazanActive,
                    isBusy = isGenieBusy,
                    onTabTargetMeasured = { tabKey, target ->
                        tabTargets[tabKey] = target
                    },
                    onRamazanTap = {
                        if (isGenieBusy) return@AnimatedCurvedNavBar
                        ramazanTapCount++
                        if (!effectiveRamazanActive && ramazanTapCount >= 5) {
                            forcedRamazanActive = true
                            val activatedMsg = when (uiState.language) {
                                "ur" -> "رمضان سیکشن فعال ہو گیا!"
                                "hi" -> "रमज़ान सेक्शन सक्रिय हो गया!"
                                else -> "Ramazan section activated!"
                            }
                            android.widget.Toast.makeText(context, activatedMsg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        if (effectiveRamazanActive) {
                            triggerTabSwitch("ramazan")
                        } else {
                            val msg = when (uiState.language) {
                                "ur" -> "رمضان سیکشن 5 فروری 2027 سے دستیاب ہوگا"
                                "hi" -> "रमज़ान सेक्शन 5 फ़रवरी 2027 से शुरू होगा"
                                else -> "Ramazan section will be available from 5 Feb 2027"
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .appBackground()
                    .onSizeChanged { size ->
                        containerWidthPx = size.width.toFloat()
                        containerHeightPx = size.height.toFloat()
                    }
            ) {
                // Layer 1: Newly selected incoming page (rendered immediately underneath)
                val incomingScale = if (isGenieBusy) {
                    0.94f + 0.06f * genieProgress.value
                } else 1f
                val incomingAlpha = if (isGenieBusy) {
                    0.5f + 0.5f * genieProgress.value
                } else 1f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .drawWithContent {
                            pageGraphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawContent()
                        }
                ) {
                    when (selectedTab) {
                        "support" -> {
                            com.example.ui.SupportScreen(
                                uiState = uiState,
                                onBack = { triggerTabSwitch("home") },
                                onLangSelect = { viewModel.setLanguage(it) },
                                onMasjidSubmitted = { viewModel.syncGoogleSheet() },
                                onChangeMasjid = { showMasjidSelectorScreen = true },
                                onSelectMasjid = { masjid ->
                                    viewModel.selectMasjid(masjid.id)
                                    triggerTabSwitch("home")
                                },
                                onOpenAdminLogin = { showAdminLoginDialog = true }
                            )
                        }
                        "ramazan" -> {
                            com.example.ui.RamazanScreenContent(
                                uiState = uiState,
                                onLanguageSelect = { viewModel.setLanguage(it) },
                                onTogglePrayer = { month, day, prayerName ->
                                    viewModel.togglePrayerForDate(month, day, prayerName)
                                }
                            )
                        }
                        else -> {
                            AzanHomeContent(
                                viewModel = viewModel,
                                uiState = uiState,
                                onOpenAdminLogin = { showAdminLoginDialog = true },
                                onChangeMasjid = { showMasjidSelectorScreen = true },
                                onShowAdminPanel = { showAdminPanel = it },
                                onSupportClick = { triggerTabSwitch("support") }
                            )
                        }
                    }
                }

                // Layer 2: macOS Genie Fold cloth effect (56-strip 2D affine matrix transform from mobile-genie-nav)
                val currentOutgoingBitmap = outgoingBitmap
                val currentOutgoingTab = outgoingTab
                val outgoingKey = currentOutgoingTab
                if (isGenieBusy && currentOutgoingBitmap != null && outgoingKey != null) {
                    val measuredTarget = tabTargets[outgoingKey]
                    val fallbackTarget = remember(outgoingKey, containerWidthPx, containerHeightPx) {
                        val w = containerWidthPx
                        val h = containerHeightPx
                        val tabW = w / 3f
                        val cx = when (outgoingKey) {
                            "support" -> tabW * 0.5f
                            "ramazan" -> tabW * 2.5f
                            else -> tabW * 1.5f
                        }
                        val barH = with(density) { 72.dp.toPx() }
                        com.example.ui.TabTarget(
                            centerX = cx,
                            centerY = h - (barH / 2f),
                            width = tabW,
                            height = barH
                        )
                    }
                    val target = measuredTarget ?: fallbackTarget

                    com.example.ui.GenieFoldOverlay(
                        bitmap = currentOutgoingBitmap,
                        progress = genieProgress.value,
                        target = target,
                        containerWidth = containerWidthPx,
                        containerHeight = containerHeightPx,
                        fabricFold = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isGenieBusy && currentOutgoingTab != null) {
                    // Fallback when bitmap capture is unavailable
                    val destinationKey = selectedTab
                    val measuredTarget = tabTargets[destinationKey] ?: tabTargets[currentOutgoingTab]
                    val targetCenterX = measuredTarget?.centerX?.takeIf { it > 0f } ?: when (destinationKey) {
                        "support" -> containerWidthPx * (1f / 6f)
                        "ramazan" -> containerWidthPx * (5f / 6f)
                        else -> containerWidthPx * 0.5f
                    }
                    val targetCenterY = measuredTarget?.centerY?.takeIf { it > 0f } ?: containerHeightPx

                    val p = genieProgress.value
                    val bounceProgress = (p / 0.15f).coerceIn(0f, 1f)
                    val bounceSin = kotlin.math.sin(bounceProgress * kotlin.math.PI.toFloat())

                    val suckProgress = ((p - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    val sCurve = suckProgress * suckProgress * (3f - 2f * suckProgress)

                    val targetPivotX = if (containerWidthPx > 0f) {
                        (targetCenterX / containerWidthPx).coerceIn(0.12f, 0.88f)
                    } else 0.5f

                    val outgoingScaleX = if (p < 0.15f) {
                        1f + 0.025f * bounceSin
                    } else {
                        (1f - 0.94f * sCurve).coerceAtLeast(0.04f)
                    }

                    val outgoingScaleY = if (p < 0.15f) {
                        1f + 0.025f * bounceSin
                    } else {
                        (1f - 0.96f * sCurve).coerceAtLeast(0.02f)
                    }

                    val outgoingTranslationY = if (p < 0.15f) {
                        -with(density) { 12.dp.toPx() } * bounceSin
                    } else {
                        val totalDrop = (targetCenterY - containerHeightPx * 0.45f).coerceAtLeast(containerHeightPx * 0.4f)
                        totalDrop * sCurve
                    }

                    val outgoingAlpha = if (suckProgress > 0.86f) {
                        ((1f - suckProgress) / 0.14f).coerceIn(0f, 1f)
                    } else {
                        1f
                    }

                    val blurX = (sCurve * 8f).dp
                    val blurY = (sCurve * 16f).dp

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .graphicsLayer {
                                this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(targetPivotX, 1.0f)
                                this.scaleX = outgoingScaleX
                                this.scaleY = outgoingScaleY
                                this.translationY = outgoingTranslationY
                                this.alpha = outgoingAlpha
                            }
                            .blur(radiusX = blurX, radiusY = blurY)
                    ) {
                        when (currentOutgoingTab) {
                            "support" -> {
                                com.example.ui.SupportScreen(
                                    uiState = uiState,
                                    onBack = {},
                                    onLangSelect = {},
                                    onMasjidSubmitted = {},
                                    onChangeMasjid = {},
                                    onSelectMasjid = {}
                                )
                            }
                            "ramazan" -> {
                                com.example.ui.RamazanScreenContent(
                                    uiState = uiState,
                                    onLanguageSelect = {}
                                )
                            }
                            else -> {
                                AzanHomeContent(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    onOpenAdminLogin = {},
                                    onChangeMasjid = {},
                                    onShowAdminPanel = {},
                                    onSupportClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            selectedMasjid = uiState.selectedMasjid,
            allMasajid = uiState.allMasajid,
            language = uiState.language,
            onDismiss = { showAdminLoginDialog = false },
            onLoginSuccess = {
                showAdminLoginDialog = false
                showAdminPanel = true
            }
        )
    }

    if (showMasjidSelectorScreen) {
        MasjidSelectionDialog(
            selectedMasjid = uiState.selectedMasjid,
            allMasajid = uiState.allMasajid,
            language = uiState.language,
            onSelectMasjid = { masjid ->
                viewModel.selectMasjid(masjid.id)
                showMasjidSelectorScreen = false
                selectedTab = "home"
            },
            onLanguageSelect = { viewModel.setLanguage(it) },
            onDismiss = { showMasjidSelectorScreen = false }
        )
    }


    }
}

data class HighlightedSlotInfo(
    val index: Int,
    val systemName: String,
    val displayName: String,
    val azanTime: String,
    val jammatTime: String,
    val isOngoing: Boolean,
    val icon: ImageVector
)

fun getHighlightedSlotInfo(
    uiState: com.example.ui.UIState,
    strings: com.example.ui.theme.AppStrings
): HighlightedSlotInfo {
    val s = uiState.selectedMasjid
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val isToday = uiState.selectedDate.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            uiState.selectedDate.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    val isFri = if (isToday) (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) else (uiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
    val timings = uiState.todayTimings

    val fAzan = s.fajrAzanFixed ?: timings?.fajr ?: "05:40"
    val dAzan = if (isFri) (uiState.customJumahAzan ?: s.jumahAzanTime.ifBlank { "12:30" }) else (s.zoharAzanFixed ?: "13:15")
    val aAzan = s.asrAzanFixed ?: timings?.asr ?: "17:17"
    val mAzan = s.maghribAzanFixed ?: timings?.maghrib ?: "18:10"
    val iAzan = s.ishaAzanFixed ?: timings?.isha ?: "19:50"

    val fJammat = getEffectiveJammatTime("Fajr", fAzan, uiState.customJammatTimes, isFri, s)
    val dJammat = getEffectiveJammatTime("Dhuhr", dAzan, uiState.customJammatTimes, isFri, s)
    val aJammat = getEffectiveJammatTime("Asr", aAzan, uiState.customJammatTimes, isFri, s)
    val mJammat = getEffectiveJammatTime("Maghrib", mAzan, uiState.customJammatTimes, isFri, s)
    val iJammat = getEffectiveJammatTime("Isha", iAzan, uiState.customJammatTimes, isFri, s)

    fun toMin(t: String): Int {
        val p = t.split(":")
        return if (p.size >= 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else 0
    }

    val fJammatCutoff = toMin(fJammat) + 15
    val dJammatCutoff = toMin(dJammat) + 15
    val aJammatCutoff = toMin(aJammat) + 15
    val mJammatCutoff = toMin(mJammat) + 15
    val iJammatCutoff = toMin(iJammat) + 15

    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    val activeIdx = when {
        currentMinutes <= fJammatCutoff -> 0
        currentMinutes <= dJammatCutoff -> 1
        currentMinutes <= aJammatCutoff -> 2
        currentMinutes <= mJammatCutoff -> 3
        currentMinutes <= iJammatCutoff -> 4
        else -> 0
    }

    val dhuhrLabel = if (isFri) strings.jumah.uppercase() else strings.dhuhr.uppercase()
    val names = listOf(
        Pair("Fajr", strings.fajr.uppercase()),
        Pair("Dhuhr", dhuhrLabel),
        Pair("Asr", strings.asr.uppercase()),
        Pair("Maghrib", strings.maghrib.uppercase()),
        Pair("Isha", strings.isha.uppercase())
    )
    val azanTimes = listOf(fAzan, dAzan, aAzan, mAzan, iAzan)
    val jammatTimes = listOf(fJammat, dJammat, aJammat, mJammat, iJammat)
    val cutoffs = listOf(fJammatCutoff, dJammatCutoff, aJammatCutoff, mJammatCutoff, iJammatCutoff)
    val icons = listOf(
        Icons.Outlined.WbTwilight,
        Icons.Outlined.WbSunny,
        Icons.Outlined.WbCloudy,
        Icons.Outlined.NightsStay,
        Icons.Outlined.Nightlight
    )

    val activeAzanMin = toMin(azanTimes[activeIdx])
    val activeCutoff = cutoffs[activeIdx]
    val isOngoing = isToday && (currentMinutes in activeAzanMin..activeCutoff)

    return HighlightedSlotInfo(
        index = activeIdx,
        systemName = names[activeIdx].first,
        displayName = names[activeIdx].second,
        azanTime = azanTimes[activeIdx],
        jammatTime = jammatTimes[activeIdx],
        isOngoing = isOngoing,
        icon = icons[activeIdx]
    )
}

@Composable
fun SlidingArrowHandle(
    isPulledDown: Boolean,
    onClick: () -> Unit,
    scale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrowFloat")
    val animatedOffsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOffsetY"
    )

    Box(
        modifier = modifier
            .size((34f * scale).dp)
            .clip(CircleShape)
            .background(Color(0xFFF3DE8E)) // Home button background color
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPulledDown) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isPulledDown) "Slide Up" else "Slide Down",
            tint = Color(0xFF0F2618), // Dark contrast icon tint
            modifier = Modifier
                .size((18f * scale).dp)
                .offset(y = (animatedOffsetY * scale).dp)
        )
    }
}

@Composable
fun AzanHomeContent(
    viewModel: AzanViewModel,
    uiState: com.example.ui.UIState,
    onOpenAdminLogin: () -> Unit = {},
    onChangeMasjid: () -> Unit = {},
    onShowAdminPanel: (Boolean) -> Unit = {},
    onSupportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photoUrl = uiState.selectedMasjid.photoUrl
    val hasQrCode = com.example.data.MasjidPaymentHelper.hasValidQr(uiState.selectedMasjid, context)
    var showSupportPopup by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        val baseTopPadding = 205.dp
        val navGap = 0.dp
        val navGapPx = with(density) { navGap.toPx() }
        val screenHeightPx = with(density) { screenHeight.toPx() }
        val baseTopPaddingPx = with(density) { baseTopPadding.toPx() }

        var sheetContentHeightPx by remember { mutableIntStateOf(0) }
        val effectiveCollapsedSheetHeightPx = if (sheetContentHeightPx > 0) {
            sheetContentHeightPx.toFloat()
        } else {
            with(density) { 260.dp.toPx() }
        }

        // When slid down: highlighted slot and content section collapse down fully to navigation bar (matching Google Studio AI proportional layout)
        val targetCollapsedTopPx = (screenHeightPx - navGapPx - effectiveCollapsedSheetHeightPx).coerceAtLeast(baseTopPaddingPx)
        val maxPullDownPx = (targetCollapsedTopPx - baseTopPaddingPx).coerceAtLeast(0f)
        val maxPullDownDistance = with(density) { maxPullDownPx.toDp() }
        val targetCollapsedTopDp = with(density) { targetCollapsedTopPx.toDp() }

        var isPulledDown by rememberSaveable { mutableStateOf(true) }
        val sheetOffsetY = remember { Animatable(if (isPulledDown) maxPullDownPx else 0f) }
        val isPlayingAzan by com.example.service.AzanForegroundService.isPlayingAzan.collectAsState()
        val isMuted by com.example.service.AzanForegroundService.isMuted.collectAsState()

        LaunchedEffect(isPulledDown, maxPullDownPx) {
            val target = if (isPulledDown) maxPullDownPx else 0f
            // Snap instantly when page opens or when returning from another screen (no slow crawl/drift)
            if (kotlin.math.abs(sheetOffsetY.value - target) > 1f) {
                if (!sheetOffsetY.isRunning) {
                    sheetOffsetY.snapTo(target)
                }
            }
        }

        var currentMinutesTick by remember {
            mutableIntStateOf(
                Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).let {
                    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
                }
            )
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000L)
                val m = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).let {
                    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
                }
                if (m != currentMinutesTick) {
                    currentMinutesTick = m
                }
            }
        }
        val strings = LocalAppStrings.current
        val highlightedInfo = remember(uiState, strings, currentMinutesTick) {
            getHighlightedSlotInfo(uiState, strings)
        }

        val dragGestureModifier = Modifier.pointerInput(maxPullDownPx) {
            detectVerticalDragGestures(
                onDragStart = { },
                onDragEnd = {
                    coroutineScope.launch {
                        val current = sheetOffsetY.value
                        val target = if (current > maxPullDownPx * 0.35f) {
                            isPulledDown = true
                            maxPullDownPx
                        } else {
                            isPulledDown = false
                            0f
                        }
                        sheetOffsetY.animateTo(
                            targetValue = target,
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        sheetOffsetY.animateTo(
                            targetValue = if (isPulledDown) maxPullDownPx else 0f,
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        val newOffset = (sheetOffsetY.value + dragAmount).coerceIn(0f, maxPullDownPx)
                        sheetOffsetY.snapTo(newOffset)
                        isPulledDown = (newOffset > maxPullDownPx * 0.4f)
                    }
                }
            )
        }

        val nestedScrollConnection = remember(maxPullDownPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    if (delta < 0 && sheetOffsetY.value > 0f) {
                        val newOffset = (sheetOffsetY.value + delta).coerceIn(0f, maxPullDownPx)
                        val consumed = sheetOffsetY.value - newOffset
                        coroutineScope.launch {
                            sheetOffsetY.snapTo(newOffset)
                            isPulledDown = (newOffset > maxPullDownPx * 0.4f)
                        }
                        return Offset(0f, -consumed)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    if (delta > 0 && source == NestedScrollSource.UserInput) {
                        val newOffset = (sheetOffsetY.value + delta).coerceIn(0f, maxPullDownPx)
                        val consumedY = newOffset - sheetOffsetY.value
                        coroutineScope.launch {
                            sheetOffsetY.snapTo(newOffset)
                            isPulledDown = (newOffset > maxPullDownPx * 0.4f)
                        }
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val target = if (sheetOffsetY.value > maxPullDownPx * 0.35f || available.y > 400f) {
                        isPulledDown = true
                        maxPullDownPx
                    } else {
                        isPulledDown = false
                        0f
                    }
                    sheetOffsetY.animateTo(
                        targetValue = target,
                        initialVelocity = available.y,
                        animationSpec = tween(
                            durationMillis = 260,
                            easing = FastOutSlowInEasing
                        )
                    )
                    return Velocity.Zero
                }
            }
        }

        // --- BACK SCREEN ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            val isGuestMasjid = uiState.selectedMasjid.id.equals("guest", ignoreCase = true) ||
                    uiState.selectedMasjid.name.contains("guest", ignoreCase = true) ||
                    uiState.selectedMasjid.name.contains("गेस्ट", ignoreCase = true) ||
                    uiState.selectedMasjid.name.contains("گیسٹ", ignoreCase = true)

            val localPhotoFile = remember(uiState.selectedMasjid.id) {
                com.example.data.MasjidLocalCacheManager.getLocalPhotoFile(context, uiState.selectedMasjid.id)
            }
            val localPhotoTimestamp = if (localPhotoFile.exists()) localPhotoFile.lastModified() else 0L
            val hasLocalPhoto = com.example.data.MasjidLocalCacheManager.hasLocalPhoto(context, uiState.selectedMasjid.id)
            val hasMasjidPhoto = (photoUrl.isNotBlank() || hasLocalPhoto) && !isGuestMasjid

            var langRowHeightPx by remember { mutableIntStateOf(0) }
            val langRowHeightDp = with(density) { langRowHeightPx.toDp() }
            val photoTopPadding = if (langRowHeightDp > 0.dp) langRowHeightDp + 16.dp else 60.dp

            val availablePhotoHeight = (targetCollapsedTopDp - photoTopPadding + 20.dp).coerceAtLeast(180.dp)

            // Centered photo container displaying 100% full original image across all screen heights
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = photoTopPadding)
                    .height(availablePhotoHeight),
                contentAlignment = Alignment.Center
            ) {
                var imageLoadError by remember(photoUrl, uiState.selectedMasjid.id, localPhotoTimestamp) { mutableStateOf(false) }

                val localOrRemoteModel = remember(photoUrl, uiState.selectedMasjid.id, localPhotoTimestamp) {
                    com.example.data.MasjidLocalCacheManager.getPhotoModel(context, uiState.selectedMasjid.id, photoUrl)
                }
                val hasValidPhoto = hasMasjidPhoto && localOrRemoteModel != null && !imageLoadError

                if (hasValidPhoto) {
                    val finalPhotoModel = ImageRequest.Builder(context)
                        .data(localOrRemoteModel)
                        .memoryCacheKey("${uiState.selectedMasjid.id}_$localPhotoTimestamp")
                        .diskCacheKey("${uiState.selectedMasjid.id}_$localPhotoTimestamp")
                        .crossfade(true)
                        .listener(onError = { _, _ -> imageLoadError = true })
                        .build()

                    // Edge-to-edge full width mosque photo matching Google Studio AI
                    AsyncImage(
                        model = finalPhotoModel,
                        contentDescription = "Selected Masjid",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // When no photo for masjid given in the request form: add azan time logo and punch line there not the makka masjid
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF14241C),
                                        Color(0xFF0F1A15),
                                        Color(0xFF0A0F14)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Squircle Logo Card with 45-degree Shimmer (matching Support Screen / image_0af060ef.png)
                            com.example.ui.AzanLogoCard(
                                size = 110.dp
                            )

                            com.example.ui.AzanTimeWordmark(
                                height = 46.dp,
                                topPadding = 14.dp,
                                bottomPadding = 6.dp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 0.dp)
                    .onSizeChanged { langRowHeightPx = it.height },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                LanguageCirclesRow(
                    currentLang = uiState.language,
                    onLangSelect = { viewModel.setLanguage(it) }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPlayingAzan && isPulledDown) {
                        Button(
                            onClick = {
                                com.example.service.AzanForegroundService.stopService(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(getStopLabel(uiState.language), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                com.example.service.AzanForegroundService.toggleMuteService(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMuted) Color(0xFFD97706) else Color(0xFF4B5563),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(getMuteLabel(uiState.language, isMuted), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (!isPlayingAzan || !isPulledDown) {
                        Box(modifier = Modifier.padding(end = 24.dp)) {
                            // Animated QR icon button (white-golden color change spin animation, square rounded corner)
                            com.example.ui.AnimatedTopQrIconButton(
                                hasQr = hasQrCode,
                                onClick = {
                                    if (hasQrCode) {
                                        showSupportPopup = true
                                    } else {
                                        val noQrMsg = when (uiState.language) {
                                            "ur" -> "اس مسجد کا QR کوڈ نہیں ہے"
                                            "hi" -> "इस मस्जिद का QR कोड नहीं है"
                                            else -> "QR code not available for this masjid"
                                        }
                                        android.widget.Toast.makeText(context, noQrMsg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- FRONT SCREEN (screen over screen: part from time till the bottom) ---
        val currentOffsetDp = with(density) { sheetOffsetY.value.toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(
                    top = (baseTopPadding + currentOffsetDp - 18.dp).coerceAtLeast(0.dp),
                    bottom = if (isPulledDown) 0.dp else 10.dp
                )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 18.dp)
                    .nestedScroll(nestedScrollConnection),
                shape = if (isPulledDown) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFF06140D),
                border = null,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (isPulledDown) it.wrapContentHeight() else it.fillMaxSize() }
                        .onSizeChanged {
                            if (isPulledDown && it.height > 0) {
                                sheetContentHeightPx = it.height + with(density) { 18.dp.roundToPx() }
                            }
                        }
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            top = if (isPulledDown) 24.dp else 36.dp,
                            bottom = if (isPulledDown) 2.dp else 12.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                // Top header: Part from time till the bottom of the clock header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(dragGestureModifier)
                ) {
                    ClockDisplay(
                        selectedDate = uiState.selectedDate,
                        language = uiState.language,
                        todayTimings = uiState.todayTimings,
                        onPrevDay = { viewModel.previousDay() },
                        onNextDay = { viewModel.nextDay() },
                        onToday = { viewModel.selectToday() },
                        onOpenAdminLogin = { onOpenAdminLogin() },
                        scale = if (isPulledDown) 0.65f else 1.0f,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = true
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (isPulledDown) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            val infiniteTransition = rememberInfiniteTransition(label = "pullDownTrimTransition")
                            val flareProgress by infiniteTransition.animateFloat(
                                initialValue = -1f,
                                targetValue = 2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 7500, easing = androidx.compose.animation.core.LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "pullDownFlare"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.35f,
                                targetValue = 0.8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pullDownPulse"
                            )
                            val islamicGlowColors = listOf(
                                Color(0xFFF59E0B),
                                Color(0xFF10B981),
                                Color(0xFFFBBF24),
                                Color(0xFF34D399),
                                Color(0xFFF59E0B)
                            )
                            
                            // Header for Prayer Name & Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = highlightedInfo.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = highlightedInfo.displayName,
                                    fontWeight = FontWeight(900),
                                    fontFamily = com.example.ui.theme.AppFontFamily,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (highlightedInfo.isOngoing) strings.ongoing else strings.next,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    style = androidx.compose.ui.text.TextStyle(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                            includeFontPadding = false
                                        ),
                                        lineHeight = 11.sp
                                    ),
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }

                            // Card with same animation as AzanSlot when slid up (shining border + glowing background)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.05f * pulseAlpha),
                                                Color.White.copy(alpha = 0.02f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .drawWithContent {
                                        drawContent()
                                        val strokeWidth = 1.5.dp.toPx()
                                        val halfStroke = strokeWidth / 2f
                                        val stroke = Stroke(width = strokeWidth)
                                        val corner = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() - halfStroke)
                                        val topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke)
                                        val borderSize = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
                                        
                                        val center = flareProgress * size.width
                                        val span = size.width * 0.7f
                                        
                                        // Sharp shining animated border (Same as slide up animation)
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    islamicGlowColors[0],
                                                    islamicGlowColors[1],
                                                    islamicGlowColors[2],
                                                    islamicGlowColors[3],
                                                    Color.Transparent
                                                ),
                                                startX = center - span,
                                                endX = center + span
                                            ),
                                            topLeft = topLeft,
                                            size = borderSize,
                                            style = stroke,
                                            cornerRadius = corner
                                        )
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF07140E))
                            ) {
                                HighlightedAzanJammatDisplay(
                                    azanTime24 = highlightedInfo.azanTime,
                                    jammatTime24 = highlightedInfo.jammatTime,
                                    compact = false,
                                    labelFontSize = 18.sp,
                                    timeFontSize = 38.sp,
                                    timeSuffixSize = 18.sp,
                                    useRobotoBlack = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            AiStudioLoadingFooter()
                        }
                    } else {
                        // When pulled up (default): full accordion will show
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AzanList(
                                viewModel = viewModel,
                                uiState = uiState,
                                singleSlotMode = false,
                                isPulledDown = isPulledDown,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            AiStudioLoadingFooter()
                        }
                    }
                }
            }
        }

        // Circular arrow handle on top edge of sheet (half inside, half outside)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        coroutineScope.launch {
                            val willSlideUp = sheetOffsetY.value > 10f
                            val target = if (willSlideUp) 0f else maxPullDownPx
                            isPulledDown = !willSlideUp
                            sheetOffsetY.animateTo(
                                targetValue = target,
                                animationSpec = tween(
                                    durationMillis = 260,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                )
                            )
                        }
                    }
                )
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFED65B),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    SlidingArrowHandle(
                        isPulledDown = isPulledDown,
                        onClick = {
                            coroutineScope.launch {
                                val willSlideUp = sheetOffsetY.value > 10f
                                val target = if (willSlideUp) 0f else maxPullDownPx
                                isPulledDown = !willSlideUp
                                sheetOffsetY.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(
                                        durationMillis = 260,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        scale = 1.15f
                    )
                }
            }
        }
    }


    if (showSupportPopup) {
        com.example.ui.SupportMasjidPopup(
            language = uiState.language,
            qrUrl = uiState.selectedMasjid.qrUrl,
            selectedMasjid = uiState.selectedMasjid,
            onDismiss = { showSupportPopup = false }
        )
    }
}
}

@Composable
fun LanguageCirclesRow(
    currentLang: String,
    onLangSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnglishActive = currentLang == "en"

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // English: morphs between circle (34x34) and rectangle (70x34, corner 8dp) with animation
        LanguageAnimatedButton(
            shortLabel = "E",
            activeLabel = "ENGLISH",
            langCode = "en",
            currentLang = currentLang,
            isOtherLarger = false,
            onLangSelect = onLangSelect
        )

        // Hindi: slightly larger (40x40 circle) when English is active
        LanguageAnimatedButton(
            shortLabel = "ह",
            activeLabel = "हिंदी",
            langCode = "hi",
            currentLang = currentLang,
            isOtherLarger = isEnglishActive,
            onLangSelect = onLangSelect
        )

        // Urdu: slightly larger (40x40 circle) when English is active
        LanguageAnimatedButton(
            shortLabel = "ر",
            activeLabel = "اردو",
            langCode = "ur",
            currentLang = currentLang,
            isOtherLarger = isEnglishActive,
            onLangSelect = onLangSelect
        )
    }
}

@Composable
fun LanguageAnimatedButton(
    shortLabel: String,
    activeLabel: String,
    langCode: String,
    currentLang: String,
    isOtherLarger: Boolean,
    onLangSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = currentLang == langCode

    // Morph target dimensions
    val targetWidth = when {
        isSelected -> 70.dp
        isOtherLarger -> 40.dp
        else -> 34.dp
    }
    val targetHeight = when {
        isSelected -> 34.dp
        isOtherLarger -> 40.dp
        else -> 34.dp
    }
    // Corner radius animation: rectangle = 8dp, circle = 20dp (for 40dp) or 17dp (for 34dp)
    val targetCorner = when {
        isSelected -> 17.dp // Smoothly morphs from circle shape
        isOtherLarger -> 20.dp
        else -> 17.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "lang_btn_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lang_btn_height"
    )
    val animatedCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lang_btn_corner"
    )

    val shape = RoundedCornerShape(animatedCorner)

    Box(
        modifier = modifier
            .size(width = animatedWidth, height = animatedHeight)
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
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFD700) else Color(0xFF26334A),
                shape = shape
            )
            .clickable { onLangSelect(langCode) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isSelected) activeLabel else shortLabel,
            fontSize = when {
                isSelected -> if (langCode == "ur") 13.sp else 12.5.sp
                langCode == "ur" -> if (isOtherLarger) 18.sp else 16.sp
                langCode == "hi" -> if (isOtherLarger) 17.sp else 14.5.sp
                else -> 13.5.sp
            },
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) Color(0xFF0C101B) else Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LanguageCircleButton(
    label: String,
    langCode: String,
    currentLang: String,
    onLangSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LanguageAnimatedButton(
        shortLabel = label,
        activeLabel = label,
        langCode = langCode,
        currentLang = currentLang,
        isOtherLarger = false,
        onLangSelect = onLangSelect,
        modifier = modifier
    )
}

@Composable
fun LanguageToggleRow(currentLang: String, onLangSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1522), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageButton("English", "en", currentLang, onLangSelect, Modifier.weight(1f))
        LanguageButton("हिंदी", "hi", currentLang, onLangSelect, Modifier.weight(1f))
        LanguageButton("اردو", "ur", currentLang, onLangSelect, Modifier.weight(1f))
    }
}

@Composable
fun LanguageButton(
    label: String,
    langCode: String,
    currentLang: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = currentLang == langCode
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(34.dp)
            .background(
                brush = if (isActive) {
                    Brush.verticalGradient(
                        listOf(Color(0xFFF3DE8E), Color(0xFFB37C3C))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF131926), Color(0xFF131926))
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(langCode) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = if (isActive) 14.sp else 12.sp,
            color = if (isActive) Color(0xFF0C101B) else Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

fun parseTimeToCalendar(timeStr: String, baseCal: Calendar): Calendar {
    val parts = timeStr.split(":")
    val targetCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata")).apply {
        timeInMillis = baseCal.timeInMillis
        if (parts.size == 2) {
            set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
            set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    return targetCal
}

@Composable
fun ClockDisplay(
    selectedDate: Calendar,
    language: String,
    todayTimings: com.example.data.AzanTiming?,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onOpenAdminLogin: () -> Unit = {},
    scale: Float = 1.0f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        }
    }

    val strings = LocalAppStrings.current

    val locale = remember(language) {
        when (language) {
            "hi" -> Locale("hi", "IN")
            "ur" -> Locale("ur")
            else -> Locale.ENGLISH
        }
    }

    val timeFormat = remember(locale) { 
        SimpleDateFormat("hh:mm", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    val amPmFormat = remember(locale) { 
        SimpleDateFormat("a", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    val dateFormat = remember(locale) { 
        SimpleDateFormat("EEEE, dd MMM yyyy", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    
    val secondsFormat = remember(locale) { 
        SimpleDateFormat("ss", locale).apply { 
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") 
        } 
    }
    val seconds = secondsFormat.format(currentTime.time)

    val today = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
    val isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    val isAfterMaghrib = if (isToday && todayTimings != null) {
        val mToday = parseTimeToCalendar(todayTimings.maghrib, currentTime)
        currentTime.after(mToday)
    } else {
        false
    }

    val islamicDateText = remember(selectedDate, isAfterMaghrib, language) {
        try {
            val gYear = selectedDate.get(Calendar.YEAR)
            val gMonth = selectedDate.get(Calendar.MONTH) + 1
            val gDay = selectedDate.get(Calendar.DAY_OF_MONTH)
            
            val baseLocalDate = java.time.LocalDate.of(gYear, gMonth, gDay)
            val localDate = if (isAfterMaghrib) baseLocalDate else baseLocalDate.minusDays(1)
            val hijrahDate = java.time.chrono.HijrahDate.from(localDate)
            val hYear = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
            val hMonth = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            val hDay = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            
            val urduDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
            val hDayUrdu = hDay.toString().map { if (it in '0'..'9') urduDigits[it - '0'] else it }.joinToString("")
            val hYearUrdu = hYear.toString().map { if (it in '0'..'9') urduDigits[it - '0'] else it }.joinToString("")

            val monthNameUrdu = when (hMonth) {
                1 -> "محرم"
                2 -> "صفر"
                3 -> "ربیع الاول"
                4 -> "ربیع الثانی"
                5 -> "جمادی الاول"
                6 -> "جمادی الثانی"
                7 -> "رجب"
                8 -> "شعبان"
                9 -> "رمضان"
                10 -> "شوال"
                11 -> "ذوالقعدہ"
                12 -> "ذوالحجہ"
                else -> ""
            }

            val urduDateFormatted = "$hDayUrdu $monthNameUrdu $hYearUrdu ہجری"

            when (language) {
                "ur" -> "اسلامی تاریخ: $urduDateFormatted"
                "hi" -> {
                    val monthNameHi = when (hMonth) {
                        1 -> "मुहर्रम"
                        2 -> "सफ़र"
                        3 -> "रबी अल-अव्वल"
                        4 -> "रबी अउ-थानी"
                        5 -> "जमाद अल-अव्वल"
                        6 -> "जमाद अउ-थानी"
                        7 -> "रजब"
                        8 -> "शबान"
                        9 -> "रमज़ान"
                        10 -> "शव्वाल"
                        11 -> "ज़ु अल-क़ादा"
                        12 -> "ज़ु अल-हिज्जह"
                        else -> ""
                    }
                    "इस्लामिक तारीख: $hDay $monthNameHi $hYear हिजरी"
                }
                else -> {
                    val monthNameEn = when (hMonth) {
                        1 -> "Muharram"
                        2 -> "Safar"
                        3 -> "Rabi-ul-Awwal"
                        4 -> "Rabi-us-Sani"
                        5 -> "Jamadi-ul-Awwal"
                        6 -> "Jamadi-us-Sani"
                        7 -> "Rajab"
                        8 -> "Shaban"
                        9 -> "Ramadan"
                        10 -> "Shawaal"
                        11 -> "Zul-Qadah"
                        12 -> "Zul-Hijjah"
                        else -> ""
                    }
                    "Islamic Date: $hDay $monthNameEn $hYear AH"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 0.dp, bottom = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val timeString = timeFormat.format(currentTime.time)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    timeString.forEachIndexed { index, char ->
                        AnimatedContent(
                            targetState = char,
                            transitionSpec = {
                                (slideInVertically { height -> height } + fadeIn()) togetherWith
                                        (slideOutVertically { height -> -height } + fadeOut())
                            },
                            label = "TimeCharAnimation$index"
                        ) { c ->
                            Text(
                                text = c.toString(),
                                fontSize = (52f * scale).sp,
                                fontWeight = FontWeight.Black,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = com.example.ui.theme.AppFontFamily,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFF3DE8E), Color(0xFFB37C3C))
                                    ),
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.9f),
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1.5f),
                                        blurRadius = 3f
                                    )
                                ),
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width((6f * scale).dp))
                Column(
                    modifier = Modifier.widthIn(min = (36f * scale).dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row {
                        seconds.forEachIndexed { index, char ->
                            AnimatedContent(
                                targetState = char,
                                transitionSpec = {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height } + fadeOut())
                                },
                                label = "SecondCharAnimation$index"
                            ) { c ->
                                Text(
                                    text = c.toString(),
                                    fontSize = (20f * scale).sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = com.example.ui.theme.AppFontFamily,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    Text(
                        text = amPmFormat.format(currentTime.time).uppercase(),
                        fontSize = (14f * scale).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = com.example.ui.theme.AppFontFamily,
                        color = TextColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(0.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isFriday = selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                val dateColor = if (isFriday) Color(0xFF25D366) else Color.White
                val dayName = strings.getDayOfWeekFull(selectedDate.get(Calendar.DAY_OF_WEEK)).uppercase()
                val dayNum = selectedDate.get(Calendar.DAY_OF_MONTH)
                val monthShort = strings.getMonthShortName(selectedDate.get(Calendar.MONTH) + 1).uppercase()
                val yearNum = selectedDate.get(Calendar.YEAR)
                val formattedDate = "$dayName, $dayNum $monthShort $yearNum"
                Text(
                    text = formattedDate,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = com.example.ui.theme.AppFontFamily,
                    color = dateColor,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            if (islamicDateText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(0.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = islamicDateText,
                        fontSize = if (language == "ur") 15.sp else 13.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = com.example.ui.theme.AppFontFamily,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = if (language == "ur") 0.sp else 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (!isToday) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clickable { onToday() }
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = strings.goToToday,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AzanList(
    viewModel: AzanViewModel,
    uiState: com.example.ui.UIState,
    isAccordionClosed: Boolean = false,
    singleSlotMode: Boolean = false,
    isPulledDown: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val strings = LocalAppStrings.current
    val currentUiState by rememberUpdatedState(uiState)
    val context = androidx.compose.ui.platform.LocalContext.current

    val curMasjid = uiState.selectedMasjid
    val todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val isTodayDate = uiState.selectedDate.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            uiState.selectedDate.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    val isFridayToday = if (isTodayDate) {
        todayCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    } else {
        uiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    }
    val dhuhrLabel = if (isFridayToday) strings.jumah else strings.dhuhr.uppercase()

    val fajrAzan = curMasjid.fajrAzanFixed ?: uiState.todayTimings?.fajr ?: "05:40"
    val zoharAzan = if (isFridayToday) {
        uiState.customJumahAzan ?: curMasjid.jumahAzanTime.ifBlank { "12:30" }
    } else {
        curMasjid.zoharAzanFixed ?: "13:15"
    }
    val asrAzan = curMasjid.asrAzanFixed ?: uiState.todayTimings?.asr ?: "17:17"
    val maghribAzan = curMasjid.maghribAzanFixed ?: uiState.todayTimings?.maghrib ?: "18:10"
    val ishaAzan = curMasjid.ishaAzanFixed ?: uiState.todayTimings?.isha ?: "19:50"

    fun computeCurrentNext(): Int {
        val isAudioPlaying = com.example.service.AzanForegroundService.isPlayingAzan.value
        val lastAudioIdx = com.example.service.AzanForegroundService.lastAudioPrayerIndex.value

        if (isAudioPlaying && lastAudioIdx in 0..5) {
            return lastAudioIdx
        }

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val s = currentUiState.selectedMasjid
        val isFri = if (isTodayDate) (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) else (currentUiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
        val f = s.fajrAzanFixed ?: currentUiState.todayTimings?.fajr ?: "05:40"
        val d = if (isFri) (currentUiState.customJumahAzan ?: s.jumahAzanTime.ifBlank { "12:30" }) else (s.zoharAzanFixed ?: "13:15")
        val a = s.asrAzanFixed ?: currentUiState.todayTimings?.asr ?: "17:17"
        val m = s.maghribAzanFixed ?: currentUiState.todayTimings?.maghrib ?: "18:10"
        val i = s.ishaAzanFixed ?: currentUiState.todayTimings?.isha ?: "19:50"

        val fJammat = getEffectiveJammatTime("Fajr", f, currentUiState.customJammatTimes, isFri, s)
        val dJammat = getEffectiveJammatTime("Dhuhr", d, currentUiState.customJammatTimes, isFri, s)
        val aJammat = getEffectiveJammatTime("Asr", a, currentUiState.customJammatTimes, isFri, s)
        val mJammat = getEffectiveJammatTime("Maghrib", m, currentUiState.customJammatTimes, isFri, s)
        val iJammat = getEffectiveJammatTime("Isha", i, currentUiState.customJammatTimes, isFri, s)

        fun toMin(t: String): Int {
            val p = t.split(":")
            return if (p.size >= 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else 0
        }

        val fJammatCutoff = toMin(fJammat) + 15
        val dJammatCutoff = toMin(dJammat) + 15
        val aJammatCutoff = toMin(aJammat) + 15
        val mJammatCutoff = toMin(mJammat) + 15
        val iJammatCutoff = toMin(iJammat) + 15

        return when {
            currentMinutes <= fJammatCutoff -> 0 // Through Fajr Jamaat + 15 min -> Fajr (Index 0)
            currentMinutes <= dJammatCutoff -> 1 // Through Zohr Jamaat + 15 min -> Zohr (Index 1)
            currentMinutes <= aJammatCutoff -> 2 // Through Asr Jamaat + 15 min -> Asr (Index 2)
            currentMinutes <= mJammatCutoff -> 3 // Through Maghrib Jamaat + 15 min -> Maghrib (Index 3)
            currentMinutes <= iJammatCutoff -> 4 // Through Isha Jamaat + 15 min -> Isha (Index 4)
            else -> 0 // After Isha Jamaat + 15 min at night -> Loops to tomorrow's Fajr (Index 0)
        }
    }

    var currentNextIndex by remember(fajrAzan, zoharAzan, asrAzan, maghribAzan, ishaAzan, currentUiState.customJammatTimes, currentUiState.selectedMasjid) {
        mutableIntStateOf(computeCurrentNext())
    }
    val initialMinuteKey = remember {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val day = cal.get(Calendar.DAY_OF_YEAR)
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        String.format(java.util.Locale.US, "INIT-%02d:%02d-%d", h, m, day)
    }
    var lastTriggeredMinute by remember { mutableStateOf(initialMinuteKey) }
    var lastObservedDayOfYear by remember { mutableIntStateOf(Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).get(Calendar.DAY_OF_YEAR)) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            if (todayDayOfYear != lastObservedDayOfYear) {
                lastObservedDayOfYear = todayDayOfYear
                viewModel.refreshDate()
            }

            currentNextIndex = computeCurrentNext()

            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMin = cal.get(Calendar.MINUTE)
            val currentTimeStr = String.format(java.util.Locale.US, "%02d:%02d", currentHour, currentMin)

            // In-app prayer time audio playback trigger (plays user audio once and closes)
            val isNowFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val liveZoharAzan = if (isNowFriday) {
                currentUiState.customJumahAzan ?: curMasjid.jumahAzanTime.ifBlank { "12:30" }
            } else {
                curMasjid.zoharAzanFixed ?: "13:15"
            }
            val prayerPairs = listOf(
                "Fajr" to (fajrAzan to currentUiState.fajrEnabled),
                "Dhuhr" to (liveZoharAzan to currentUiState.dhuhrEnabled),
                "Asr" to (asrAzan to currentUiState.asrEnabled),
                "Maghrib" to (maghribAzan to currentUiState.maghribEnabled),
                "Isha" to (ishaAzan to currentUiState.ishaEnabled)
            )
            for ((name, pair) in prayerPairs) {
                val (timeStr, isEnabled) = pair
                if (isEnabled && timeStr == currentTimeStr) {
                    if (!com.example.service.AzanForegroundService.isPlayingAzan.value &&
                        !com.example.service.AzanForegroundService.isPrayerStoppedOrHandled(context, name)
                    ) {
                        val triggerKey = "$name-$currentTimeStr-$todayDayOfYear"
                        if (lastTriggeredMinute != triggerKey) {
                            lastTriggeredMinute = triggerKey
                            try {
                                val serviceIntent = Intent(context, com.example.service.AzanForegroundService::class.java).apply {
                                    putExtra("AZAN_NAME", name)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            delay(1000L)
        }
    }

    val slots = listOf(
        Triple("Fajr", strings.fajr.uppercase(), fajrAzan),
        Triple("Dhuhr", dhuhrLabel, zoharAzan),
        Triple("Asr", strings.asr.uppercase(), asrAzan),
        Triple("Maghrib", strings.maghrib.uppercase(), maghribAzan),
        Triple("Isha", strings.isha.uppercase(), ishaAzan),
        Triple("Tahajjud", strings.tahajjud.uppercase(), "01:30")
    )
    val toggles = listOf(
        uiState.fajrEnabled, uiState.dhuhrEnabled, uiState.asrEnabled, uiState.maghribEnabled, uiState.ishaEnabled, false // no audio for tahajjud
    )

    // Icons assigned representing the visual path of the day/night
    val icons = listOf(
        Icons.Outlined.WbTwilight, // Fajr
        Icons.Outlined.WbSunny,    // Dhuhr
        Icons.Outlined.WbCloudy,   // Asr
        Icons.Outlined.NightsStay, // Maghrib (moon with cloud)
        Icons.Outlined.Nightlight, // Isha
        Icons.Outlined.StarRate    // Tahajjud
    )

    val today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val isToday = uiState.selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            uiState.selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    val prayedList = listOf(
        uiState.fajrPrayed,
        uiState.dhuhrPrayed,
        uiState.asrPrayed,
        uiState.maghribPrayed,
        uiState.ishaPrayed,
        uiState.tahajjudPrayed
    )

    var expandedIndex by remember(currentNextIndex, isAccordionClosed) {
        mutableIntStateOf(if (isAccordionClosed) -1 else currentNextIndex)
    }

    val isPlayingAzan by com.example.service.AzanForegroundService.isPlayingAzan.collectAsState()
    val isMuted by com.example.service.AzanForegroundService.isMuted.collectAsState()

    LaunchedEffect(currentNextIndex, isToday, isAccordionClosed) {
        if (isAccordionClosed) {
            expandedIndex = -1
        } else if (isToday && currentNextIndex != -1) {
            expandedIndex = currentNextIndex
        }
    }

    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isPlayingAzan && !isPulledDown) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isMuted) Color(0xFFD97706) else Color(0xFFEAB308))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Azan Muted" else "Azan Playing",
                            tint = if (isMuted) Color(0xFFD97706) else Color(0xFFEAB308),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isMuted) "Azan Muted" else "Azan Playing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isMuted) "Audio is muted • Playing silently" else "Playing once • Auto closing",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                com.example.service.AzanForegroundService.stopService(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(getStopLabel(uiState.language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                com.example.service.AzanForegroundService.toggleMuteService(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMuted) Color(0xFFD97706) else Color(0xFF4B5563),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(getMuteLabel(uiState.language, isMuted), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val displayedIndices = if (singleSlotMode) {
            val safeIdx = if (currentNextIndex in slots.indices) currentNextIndex else 0
            listOf(safeIdx)
        } else {
            slots.indices.toList()
        }

        displayedIndices.forEach { index ->
            val triple = slots[index]
            val rawEffectiveJammat = if (triple.first != "Tahajjud") {
                getEffectiveJammatTime(triple.first, triple.third, uiState.customJammatTimes, isFridayToday, uiState.selectedMasjid)
            } else {
                ""
            }
            val jammatTimeStr = if (rawEffectiveJammat.isNotBlank() && rawEffectiveJammat.contains(":")) {
                rawEffectiveJammat
            } else if (triple.first != "Tahajjud") {
                val p = triple.third.split(":")
                val h = p.getOrNull(0)?.toIntOrNull() ?: 5
                val m = p.getOrNull(1)?.toIntOrNull() ?: 0
                val off = when (triple.first.lowercase()) {
                    "fajr" -> 35
                    "maghrib" -> 15
                    "isha" -> 20
                    else -> 25
                }
                val tot = h * 60 + m + off
                String.format(java.util.Locale.US, "%02d:%02d", (tot / 60) % 24, tot % 60)
            } else {
                ""
            }
            
            // Check if prayer is unlocked: active ONLY 10 minutes after Jammat time!
            val (isPrayerUnlocked, unlockTimeFormatted) = if (isToday && triple.first != "Tahajjud") {
                checkPrayerUnlocked(jammatTimeStr, triple.third, now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")), systemName = triple.first)
            } else {
                Pair(true, "")
            }

            // Ongoing if current time is between Azan and Jamaat + 15 min
            val currentMinutes = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
            val azanMin = triple.third.split(":").let { if (it.size >= 2) (it[0].toIntOrNull() ?: 0) * 60 + (it[1].toIntOrNull() ?: 0) else 0 }
            val jammatMin = jammatTimeStr.split(":").let { if (it.size >= 2) (it[0].toIntOrNull() ?: 0) * 60 + (it[1].toIntOrNull() ?: 0) else 0 }
            val jammatCutoff = (if (jammatMin > 0) jammatMin else azanMin + 20) + 15

            val isSlotActive = if (singleSlotMode) true else (isToday && (index == currentNextIndex))
            val isOngoing = isSlotActive && (currentMinutes in azanMin..jammatCutoff)
            val isNext = isSlotActive && !isOngoing

            val isExpanded = if (singleSlotMode) false else (index == expandedIndex)
            
            AzanSlot(
                systemName = triple.first,
                displayName = triple.second,
                time = triple.third,
                jammatTime = jammatTimeStr,
                enabled = toggles[index],
                isNext = isNext,
                isOngoing = isOngoing,
                icon = icons[index],
                prayed = prayedList[index],
                isPrayerUnlocked = isPrayerUnlocked,
                expanded = isExpanded,
                showAudioToggle = if (singleSlotMode) false else (triple.first != "Tahajjud"),
                showExpandIcon = !singleSlotMode,
                onHeaderClick = {
                    if (enabled && !singleSlotMode) {
                        expandedIndex = if (isExpanded) -1 else index
                    }
                },
                modifier = Modifier.animateContentSize(),
                onToggle = { newEnabled -> if (enabled) viewModel.toggleAzan(triple.first, newEnabled) },
                onPrayedToggle = {
                    if (!enabled) return@AzanSlot
                    if (isToday && !prayedList[index]) {
                        if (!isPrayerUnlocked) {
                            val jammatDisplay = formatTo12Hour(if (jammatTimeStr.isNotBlank()) jammatTimeStr else triple.third)
                            val msg = when (uiState.language) {
                                "ur" -> "${triple.second} کی جماعت $jammatDisplay پر ہے۔ حاضری کا نشان جماعت کے 10 منٹ بعد ($unlockTimeFormatted پر) فعال ہوگا۔"
                                "hi" -> "${triple.second} जमात $jammatDisplay बजे है। टिक मार्क जमात के 10 मिनट बाद ($unlockTimeFormatted पर) चालू होगा。"
                                else -> "${triple.second} jammat is at $jammatDisplay. Tick mark will unlock at $unlockTimeFormatted (10 min after jammat)."
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            return@AzanSlot
                        }
                    }
                    viewModel.togglePrayerPrayed(triple.first)
                },
                showPrayerToggle = false
            )
        }
    }
}

fun getRakatSegments(prayerName: String, strings: com.example.ui.theme.AppStrings, isJumuah: Boolean = false): List<Pair<String, Int>> {
    return when (prayerName) {
        "Fajr" -> listOf(
            Pair(strings.sunnat, 2),
            Pair(strings.farz, 2)
        )
        "Dhuhr" -> if (isJumuah) {
            listOf(
                Pair(strings.sunnat, 4),
                Pair(strings.farz, 2),
                Pair(strings.sunnat, 4),
                Pair(strings.sunnat, 2),
                Pair(strings.nafil, 2)
            )
        } else {
            listOf(
                Pair(strings.sunnat, 4),
                Pair(strings.farz, 4),
                Pair(strings.sunnat, 2),
                Pair(strings.nafil, 2)
            )
        }
        "Asr" -> listOf(
            Pair(strings.sunnat, 4),
            Pair(strings.farz, 4)
        )
        "Maghrib" -> listOf(
            Pair(strings.farz, 3),
            Pair(strings.sunnat, 2),
            Pair(strings.nafil, 2)
        )
        "Isha" -> listOf(
            Pair(strings.sunnat, 4),
            Pair(strings.farz, 4),
            Pair(strings.sunnat, 2),
            Pair(strings.nafil, 2),
            Pair(strings.witr, 3),
            Pair(strings.nafil, 2)
        )
        "Tahajjud" -> listOf(
            Pair(strings.nafil, 2)
        )
        else -> emptyList()
    }
}

@Composable
fun AzanSlot(
    systemName: String,
    displayName: String,
    time: String,
    jammatTime: String = "",
    enabled: Boolean,
    isNext: Boolean,
    isOngoing: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    prayed: Boolean,
    isPrayerUnlocked: Boolean = true,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAudioToggle: Boolean = true,
    showExpandIcon: Boolean = true,
    onToggle: (Boolean) -> Unit,
    onPrayedToggle: () -> Unit,
    showPrayerToggle: Boolean = true
) {
    // ...
    val isHighlighted = isNext || isOngoing
    // ...
    // Text("NEXT") logic inside AzanSlot changed to use strings.ongoing if isOngoing is true
    // And AzanJammatDisplay(..., isNext = isNext, isOngoing = isOngoing)
    // ...

    val infiniteTransition = rememberInfiniteTransition(label = "trimTransition")
    
    val flareProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flare"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val tintColor = MaterialTheme.colorScheme.primary
    val strings = LocalAppStrings.current

    val islamicGlowColors = listOf(
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFFFBBF24),
        Color(0xFF34D399),
        Color(0xFFF59E0B)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() }
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isHighlighted) listOf(
                        Color.White.copy(alpha = 0.05f * pulseAlpha),
                        Color.White.copy(alpha = 0.02f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color(0xFF0E131F).copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .drawWithContent {
                drawContent()
                if (isHighlighted) {
                    val strokeWidth = 1.5.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val stroke = Stroke(width = strokeWidth)
                    val corner = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() - halfStroke)
                    val topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke)
                    val borderSize = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
                    
                    val center = flareProgress * size.width
                    val span = size.width * 0.7f
                    
                    // Sharp shining animated border (Celestial Islamic theme)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                islamicGlowColors[0],
                                islamicGlowColors[1],
                                islamicGlowColors[2],
                                islamicGlowColors[3],
                                Color.Transparent
                            ),
                            startX = center - span,
                            endX = center + span
                        ),
                        topLeft = topLeft,
                        size = borderSize,
                        style = stroke,
                        cornerRadius = corner
                    )
                } else {
                    // Glass border for inactive slots
                    val strokeWidth = 1.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() - halfStroke)
                    )
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isHighlighted && !showExpandIcon) 0.dp else 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = if (isHighlighted) (if (!showExpandIcon) 8.dp else 10.dp) else 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = if (!showExpandIcon) Modifier else Modifier.weight(1f).padding(end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Glow icon wrapper
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isHighlighted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color(0xFF1A2234),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = displayName,
                            tint = if (isHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = displayName,
                            fontSize = if (isHighlighted) {
                                if (systemName == "Maghrib") 18.sp else if (!showExpandIcon) 20.sp else 22.sp
                            } else 16.sp,
                            fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Bold,
                            color = if (isHighlighted) MaterialTheme.colorScheme.secondary else TextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isHighlighted) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isOngoing) strings.ongoing else strings.next,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    ),
                                    lineHeight = 10.sp
                                ),
                                modifier = Modifier
                                    .background(Color(0xFFFFD700), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isHighlighted && !showExpandIcon && systemName != "Tahajjud") {
                        // Image 2 exact single-row layout
                        HighlightedAzanJammatDisplay(
                            azanTime24 = time,
                            jammatTime24 = jammatTime,
                            compact = true
                        )
                    } else if (!isHighlighted) {
                        if (systemName == "Tahajjud") {
                            PrayerTimeDisplay(time24 = time, isNext = false)
                        } else {
                            AzanJammatDisplay(
                                azanTime24 = time,
                                jammatTime24 = jammatTime,
                                isNext = false,
                                isOngoing = false
                            )
                        }
                    } else if (systemName == "Tahajjud") {
                        PrayerTimeDisplay(time24 = time, isNext = true)
                    }
                    
                    if (showPrayerToggle) {
                        IconButton(onClick = onPrayedToggle, modifier = Modifier.size(32.dp)) {
                            if (prayed) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Marked as Prayed",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else if (!isPrayerUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = "Locked until 10 min after jammat",
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Mark as Prayed",
                                    tint = TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    
                    if (showAudioToggle) {
                        IconButton(onClick = { onToggle(!enabled) }, modifier = Modifier.size(32.dp)) {
                            if (enabled) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "Toggle Audio",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .drawWithContent {
                                             drawCircle(
                                                 color = Color.Red,
                                                 radius = size.minDimension / 2,
                                                 style = Stroke(width = 2.dp.toPx())
                                             )
                                             drawLine(
                                                 color = Color.Red,
                                                 start = Offset(4.dp.toPx(), 4.dp.toPx()),
                                                 end = Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                                 strokeWidth = 2.dp.toPx()
                                             )
                                             drawContent()
                                         },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Toggle Audio",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (showExpandIcon) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle Rak'at Accordion",
                            tint = if (isHighlighted) MaterialTheme.colorScheme.secondary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            if (isHighlighted && showExpandIcon && systemName != "Tahajjud") {
                HighlightedAzanJammatDisplay(
                    azanTime24 = time,
                    jammatTime24 = jammatTime,
                    compact = false,
                    labelFontSize = 18.sp,
                    timeFontSize = 30.sp,
                    timeSuffixSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 14.dp)
                )
            }
            
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                val isJumuah = systemName == "Dhuhr" && (displayName.equals("JUMAH", ignoreCase = true) || displayName.equals(strings.jumah, ignoreCase = true))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .background(Color(0xFF0F1522), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    val segments = getRakatSegments(systemName, strings, isJumuah)
                    val totalRakat = segments.sumOf { it.second }
                    
                    val typeHeader = strings.prayerType
                    val countHeader = strings.rakats

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Table Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF162035), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = typeHeader,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = countHeader,
                                modifier = Modifier.width(80.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.End,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Data rows below
                        segments.forEachIndexed { idx, segment ->
                            val color = Color(0xFFF3DE8E)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) Color(0xFF0C1220) else Color(0xFF0F1522))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = segment.first,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextColor
                                    )
                                }
                                Text(
                                    text = "${segment.second}",
                                    modifier = Modifier.width(80.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextColor,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        // Total Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF162035), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.total,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "$totalRakat",
                                modifier = Modifier.width(80.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.appBackground(): Modifier {
    return this.background(Color.Black)
}

// Clean solid background without decorative overlays as requested
fun Modifier.islamicStarBackground(primaryColor: Color): Modifier = this



fun formatTo12Hour(time24: String): String {
    if (time24 == "--:--" || !time24.contains(":")) return time24
    val parts = time24.split(":")
    if (parts.size < 2) return time24
    val hour = parts[0].toIntOrNull() ?: return time24
    val minute = parts[1].toIntOrNull() ?: return time24
    
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(java.util.Locale.US, "%02d:%02d %s", displayHour, minute, suffix)
}

fun calculateJammatTime(systemName: String, azanTime24: String, isFriday: Boolean = false): String {
    if (azanTime24 == "--:--" || !azanTime24.contains(":")) return "--:--"
    val parts = azanTime24.split(":")
    if (parts.size != 2) return azanTime24
    val h = parts[0].toIntOrNull() ?: return azanTime24
    val m = parts[1].toIntOrNull() ?: return azanTime24

    if (systemName == "Dhuhr" && isFriday) {
        return "13:30" // 01:30 PM Jum'ah Jamaat
    }

    val offsetMinutes = when (systemName) {
        "Fajr" -> 25
        "Dhuhr" -> 20
        "Asr" -> 20
        "Maghrib" -> 10
        "Isha" -> 20
        else -> 0
    }
    val totalMins = (h * 60 + m + offsetMinutes) % (24 * 60)
    val jammatH = totalMins / 60
    val jammatM = totalMins % 60
    return String.format(java.util.Locale.US, "%02d:%02d", jammatH, jammatM)
}

fun checkPrayerUnlocked(
    jammatTime24: String,
    azanTime24: String,
    now: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")),
    systemName: String = ""
): Pair<Boolean, String> {
    val timeToUse = if (jammatTime24.isNotBlank() && jammatTime24.contains(":")) {
        jammatTime24.trim()
    } else {
        azanTime24.trim()
    }
    if (!timeToUse.contains(":")) return Pair(true, "")
    val parts = timeToUse.split(":")
    val rawH = parts.getOrNull(0)?.toIntOrNull() ?: return Pair(true, "")
    val rawM = parts.getOrNull(1)?.toIntOrNull() ?: return Pair(true, "")
    
    val h = when (systemName.lowercase()) {
        "fajr" -> rawH // Fajr is ALWAYS in the morning (AM), never add 12
        "dhuhr", "zohar", "jumah", "jum'ah" -> if (rawH in 1..11) rawH + 12 else rawH
        "asr" -> if (rawH in 1..11) rawH + 12 else rawH
        "maghrib" -> if (rawH in 1..11) rawH + 12 else rawH
        "isha" -> if (rawH in 1..11) rawH + 12 else rawH
        else -> {
            if (rawH in 1..3) rawH + 12 else rawH
        }
    }
    val jammatTotalMinutes = h * 60 + rawM
    val unlockTotalMinutes = jammatTotalMinutes + 10 // Active ONLY 10 min after jammat!

    val currentTotalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    val unlockH = (unlockTotalMinutes / 60) % 24
    val unlockM = unlockTotalMinutes % 60
    val unlockFormatted = formatTo12Hour(String.format(java.util.Locale.US, "%02d:%02d", unlockH, unlockM))

    val isUnlocked = currentTotalMinutes >= unlockTotalMinutes
    return Pair(isUnlocked, unlockFormatted)
}

fun getEffectiveJammatTime(
    systemName: String,
    azanTime24: String,
    customJammatMap: Map<String, String>,
    isFriday: Boolean = false,
    masjid: MasjidItem? = null
): String {
    // 1. Check masjid's fixed Jammat time FIRST (Primary Source of Truth)
    if (masjid != null) {
        if (systemName.equals("Jumah", ignoreCase = true) || systemName.equals("Jum'ah", ignoreCase = true) || (systemName.equals("Dhuhr", ignoreCase = true) && isFriday)) {
            if (masjid.jumahJammatTime.isNotBlank()) {
                return masjid.jumahJammatTime
            }
        }
        val fixed = when (systemName.lowercase()) {
            "fajr" -> masjid.fajrJammatFixed
            "dhuhr", "zohar" -> masjid.zoharJammatFixed
            "asr" -> masjid.asrJammatFixed
            "maghrib" -> masjid.maghribJammatFixed
            "isha" -> masjid.ishaJammatFixed
            else -> null
        }
        if (!fixed.isNullOrBlank()) {
            return fixed
        }
    }

    // 2. Custom local override if set
    if (systemName.equals("Jumah", ignoreCase = true) || systemName.equals("Jum'ah", ignoreCase = true) || (systemName.equals("Dhuhr", ignoreCase = true) && isFriday)) {
        val customJumah = customJammatMap["jumah"]
        if (!customJumah.isNullOrEmpty()) {
            return customJumah
        }
        return masjid?.jumahJammatTime ?: "13:30"
    }
    val custom = customJammatMap[systemName.lowercase()]
    if (!custom.isNullOrEmpty()) {
        return custom
    }
    if (masjid != null && azanTime24.contains(":")) {
        val offset = when (systemName.lowercase()) {
            "fajr" -> masjid.fajrJammatOffset
            "dhuhr" -> masjid.dhuhrJammatOffset
            "asr" -> masjid.asrJammatOffset
            "maghrib" -> masjid.maghribJammatOffset
            "isha" -> masjid.ishaJammatOffset
            else -> 20
        }
        val parts = azanTime24.split(":")
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        val totalMins = (h * 60 + m + offset) % (24 * 60)
        return String.format(java.util.Locale.US, "%02d:%02d", totalMins / 60, totalMins % 60)
    }
    return calculateJammatTime(systemName, azanTime24, isFriday)
}

@Composable
fun AzanJammatDisplay(
    azanTime24: String,
    jammatTime24: String,
    isNext: Boolean,
    isOngoing: Boolean = false
) {
    val strings = LocalAppStrings.current
    val rawAzan = if (azanTime24.isNotBlank() && azanTime24 != "--:--" && azanTime24.contains(":")) azanTime24 else "05:40"
    val rawJammat = if (jammatTime24.isNotBlank() && jammatTime24 != "--:--" && jammatTime24.contains(":")) {
        jammatTime24
    } else {
        val parts = rawAzan.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 5
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 40
        val tot = h * 60 + m + 25
        String.format(java.util.Locale.US, "%02d:%02d", (tot / 60) % 24, tot % 60)
    }

    val azanFormatted = formatTo12Hour(rawAzan)
    val jammatFormatted = formatTo12Hour(rawJammat)

    val labelColor = if (isNext || isOngoing) Color(0xFFF3DE8E) else TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // Azan column: Name on top (ALL CAPS), time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.azanLabel.uppercase(),
                fontSize = if (isNext) 10.sp else 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = azanFormatted,
                fontSize = if (isNext) 12.sp else 10.5.sp,
                fontWeight = FontWeight(900),
                fontFamily = com.example.ui.theme.AppFontFamily,
                color = if (isNext || isOngoing) Color(0xFFFFD700) else Color.White
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(1.dp)
                .height(if (isNext) 24.dp else 18.dp)
                .background(Color.White.copy(alpha = if (isNext) 0.35f else 0.15f))
        )

        // Jammat column: Name on top (ALL CAPS), time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.jammatLabel.uppercase(),
                fontSize = if (isNext) 10.sp else 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNext) Color(0xFF86EFAC) else TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = jammatFormatted,
                fontSize = if (isNext) 12.sp else 10.5.sp,
                fontWeight = FontWeight(900),
                fontFamily = com.example.ui.theme.AppFontFamily,
                color = if (isNext || isOngoing) Color(0xFF4ADE80) else Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun HighlightedAzanJammatDisplay(
    azanTime24: String,
    jammatTime24: String,
    compact: Boolean = false,
    textScale: Float = 1.0f,
    timeFontSize: androidx.compose.ui.unit.TextUnit? = null,
    timeSuffixSize: androidx.compose.ui.unit.TextUnit? = null,
    labelFontSize: androidx.compose.ui.unit.TextUnit? = null,
    useRobotoBlack: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val rawAzan = if (azanTime24.isNotBlank() && azanTime24 != "--:--" && azanTime24.contains(":")) azanTime24 else "05:40"
    val rawJammat = if (jammatTime24.isNotBlank() && jammatTime24 != "--:--" && jammatTime24.contains(":")) {
        jammatTime24
    } else {
        val parts = rawAzan.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 5
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 40
        val tot = h * 60 + m + 25
        String.format(java.util.Locale.US, "%02d:%02d", (tot / 60) % 24, tot % 60)
    }

    val azanFormatted = formatTo12Hour(rawAzan)
    val jammatFormatted = formatTo12Hour(rawJammat)

    val azanParts = azanFormatted.split(" ")
    val azanTimeStr = azanParts.getOrNull(0) ?: azanFormatted
    val azanSuffix = azanParts.getOrNull(1) ?: ""

    val jammatParts = jammatFormatted.split(" ")
    val jammatTimeStr = jammatParts.getOrNull(0) ?: jammatFormatted
    val jammatSuffix = jammatParts.getOrNull(1) ?: ""

    val effectiveTimeSize = timeFontSize ?: (if (compact) 16.5.sp else (20f * textScale).sp)
    val effectiveSuffixSize = timeSuffixSize ?: (if (compact) 9.5.sp else (12f * textScale).sp)
    val effectiveLabelSize = labelFontSize ?: (if (compact) 9.5.sp else (11f * textScale).sp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.SpaceEvenly
    ) {
        // Azan column: Label on top (ALL CAPS), prominent time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (compact) Modifier else Modifier.weight(1f)
        ) {
            Text(
                text = strings.azanLabel.uppercase(),
                fontSize = effectiveLabelSize,
                fontWeight = FontWeight(900),
                fontFamily = com.example.ui.theme.AppFontFamily,
                color = Color(0xFFF3DE8E),
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height((2f * textScale).dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = azanTimeStr,
                    fontSize = effectiveTimeSize,
                    fontWeight = FontWeight(900),
                    fontFamily = com.example.ui.theme.AppFontFamily,
                    letterSpacing = (-0.5).sp,
                    color = Color(0xFFFFD700),
                    maxLines = 1,
                    softWrap = false
                )
                if (azanSuffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = azanSuffix,
                        fontSize = effectiveSuffixSize,
                        fontWeight = FontWeight(900),
                        fontFamily = com.example.ui.theme.AppFontFamily,
                        color = Color(0xFFFFD700).copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = if (timeFontSize != null) 3.dp else (1.5f * textScale).dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = if (compact) 8.dp else (12f * textScale).dp)
                .width(if (compact) 1.dp else (1.6f * textScale).dp)
                .height(if (compact) 24.dp else (if (timeFontSize != null) 38.dp else (32f * textScale).dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF10B981).copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Jammat column: Label on top (ALL CAPS), prominent time below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (compact) Modifier else Modifier.weight(1f)
        ) {
            Text(
                text = strings.jammatLabel.uppercase(),
                fontSize = effectiveLabelSize,
                fontWeight = FontWeight(900),
                fontFamily = com.example.ui.theme.AppFontFamily,
                color = Color(0xFF86EFAC),
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height((2f * textScale).dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = jammatTimeStr,
                    fontSize = effectiveTimeSize,
                    fontWeight = FontWeight(900),
                    fontFamily = com.example.ui.theme.AppFontFamily,
                    letterSpacing = (-0.5).sp,
                    color = Color(0xFF4ADE80),
                    maxLines = 1,
                    softWrap = false
                )
                if (jammatSuffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = jammatSuffix,
                        fontSize = effectiveSuffixSize,
                        fontWeight = FontWeight(900),
                        fontFamily = com.example.ui.theme.AppFontFamily,
                        color = Color(0xFF4ADE80).copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = if (timeFontSize != null) 3.dp else (1.5f * textScale).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerTimeDisplay(time24: String, isNext: Boolean) {
    val formatted = formatTo12Hour(time24)
    val parts = formatted.split(" ")
    val timeStr = parts.getOrNull(0) ?: time24
    val suffixStr = parts.getOrNull(1) ?: ""
    
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = timeStr,
            fontSize = if (isNext) 22.sp else 16.sp,
            fontWeight = FontWeight(900),
            fontFamily = com.example.ui.theme.AppFontFamily,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF3DE8E), Color(0xFFB37C3C))
                )
            )
        )
        if (suffixStr.isNotEmpty()) {
            Text(
                text = suffixStr,
                fontSize = if (isNext) 14.sp else 10.sp,
                fontWeight = FontWeight(900),
                fontFamily = com.example.ui.theme.AppFontFamily,
                color = if (isNext) MaterialTheme.colorScheme.secondary else TextMuted
            )
        }
    }
}

fun Modifier.animatedWavingLines(primaryColor: Color) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    this.drawBehind {
        val width = size.width
        val height = size.height
        val centerY = height * 0.70f
        
        for (i in 0 until 3) {
            val amplitude = 25f + (i * 20f)
            val frequency = 1.5f
            
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, centerY)
            
            val steps = 50
            for (step in 0..steps) {
                val x = (step.toFloat() / steps) * width
                val angle = (x / width) * (2f * Math.PI.toFloat() * frequency) + phase + (i * 1.5f)
                val y = centerY + kotlin.math.sin(angle).toFloat() * amplitude
                path.lineTo(x, y)
            }
            
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.03f + (i * 0.015f)),
                style = Stroke(width = 3f)
            )
        }
    }
}

data class AdminPrayerItem(
    val systemName: String,
    val displayName: String,
    val icon: ImageVector,
    val azanTime: String,
    val jammatTime: String
)

@Composable
fun AdminLoginDialog(
    selectedMasjid: com.example.data.MasjidItem? = null,
    allMasajid: List<com.example.data.MasjidItem> = emptyList(),
    language: String = "en",
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val curMasjid = selectedMasjid ?: allMasajid.firstOrNull()
    val defaultAdminId = curMasjid?.adminId?.trim().takeUnless { it.isNullOrBlank() } ?: "admin"
    val defaultPassword = curMasjid?.adminPassword?.trim().takeUnless { it.isNullOrBlank() } ?: "admin"

    var adminId by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)),
            border = BorderStroke(1.5.dp, Color(0xFFE5B842).copy(alpha = 0.70f)),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo emblem without wordmark or punchline
                com.example.ui.AzanLogoCard(
                    size = 64.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (language) {
                        "ur" -> "ایڈمن لاگ ان"
                        "hi" -> "एडमिन लॉगिन"
                        else -> "ADMIN LOGIN"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF3DE8E),
                    letterSpacing = 1.2.sp
                )

                val localizedMasjidName = selectedMasjid?.getLocalizedName(language)
                val subText = when (language) {
                    "ur" -> if (localizedMasjidName != null) "$localizedMasjidName کے لیے ID اور پاس ورڈ درج کریں" else "نماز کے اوقات میں ترمیم کے لیے ID اور پاس ورڈ درج کریں"
                    "hi" -> if (localizedMasjidName != null) "$localizedMasjidName के लिए ID और पासवर्ड दर्ज करें" else "नमाज़ के समय में बदलाव के लिए ID और पासवर्ड दर्ज करें"
                    else -> if (selectedMasjid != null) "Enter ID & Password for ${selectedMasjid.name}" else "Enter ID & Password to edit prayer timings"
                }
                Text(
                    text = subText,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                val adminIdLabel = when (language) {
                    "ur" -> "ایڈمن آئی ڈی - admin"
                    "hi" -> "एडमिन आईडी - admin"
                    else -> "Admin ID - admin"
                }
                OutlinedTextField(
                    value = "admin",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(adminIdLabel) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White.copy(alpha = 0.8f),
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedBorderColor = Color(0xFFF3DE8E).copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0xFFE5B842).copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val passLabel = when (language) {
                    "ur" -> "پاس ورڈ"
                    "hi" -> "पासवर्ड"
                    else -> "Password"
                }
                val passPlaceholder = when (language) {
                    "ur" -> "پاس ورڈ درج کریں"
                    "hi" -> "पासवर्ड दर्ज करें"
                    else -> "Enter Password"
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = { Text(passLabel) },
                    placeholder = { Text(passPlaceholder) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFFF3DE8E)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color(0xFFE5B842).copy(alpha = 0.4f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        cursorColor = Color(0xFFF3DE8E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val errText = when (language) {
                        "ur" -> if (localizedMasjidName != null) "$localizedMasjidName کے لیے غلط آئی ڈی یا پاس ورڈ" else "غلط آئی ڈی یا پاس ورڈ"
                        "hi" -> if (localizedMasjidName != null) "$localizedMasjidName के लिए गलत आईडी या पासवर्ड" else "गलत आईडी या पासवर्ड"
                        else -> if (selectedMasjid != null) "Incorrect ID or Password for ${selectedMasjid.name}" else "Incorrect ID or Password"
                    }
                    Text(
                        text = errText,
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFE5B842).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF3DE8E))
                    ) {
                        Text(
                            text = when (language) {
                                "ur" -> "منسوخ"
                                "hi" -> "रद्द करें"
                                else -> "Cancel"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                    }

                    Button(
                        onClick = {
                            val id = adminId.trim()
                            val pass = password.trim()

                            val curMasjid = selectedMasjid ?: allMasajid.firstOrNull()
                            val targetAdminId = curMasjid?.adminId?.trim().takeUnless { it.isNullOrBlank() } ?: "admin"
                            val targetPassword = curMasjid?.adminPassword?.trim() ?: ""

                            val isMatch = (id.equals(targetAdminId, ignoreCase = true) || id == "admin" || targetAdminId == "admin") &&
                                (pass == targetPassword || (targetPassword.isBlank() && (pass == "admin" || pass == "1234")))

                            if (isMatch) {
                                isError = false
                                onLoginSuccess()
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF3DE8E),
                            contentColor = Color(0xFF0F1713)
                        )
                    ) {
                        Text(
                            text = when (language) {
                                "ur" -> "لاگ ان"
                                "hi" -> "लॉगिन"
                                else -> "Login"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    viewModel: AzanViewModel,
    uiState: com.example.ui.UIState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val curMasjid = uiState.selectedMasjid
    val localizedCurMasjidName = curMasjid.getLocalizedName(uiState.language)
    val localizedCurMasjidLoc = curMasjid.getLocalizedFullLocation(uiState.language)
    val isFridayToday = uiState.selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    val dhuhrLabel = if (isFridayToday) strings.jumah else strings.dhuhr.uppercase()

    val timings = uiState.todayTimings
    val fajrAzan = curMasjid.fajrAzanFixed ?: timings?.fajr ?: "05:40"
    val zoharAzan = curMasjid.zoharAzanFixed ?: "13:15"
    val jumahAzan = curMasjid.jumahAzanTime.ifBlank { "12:30" }
    val asrAzan = curMasjid.asrAzanFixed ?: timings?.asr ?: "17:17"
    val maghribAzan = curMasjid.maghribAzanFixed ?: timings?.maghrib ?: "18:10"
    val ishaAzan = curMasjid.ishaAzanFixed ?: timings?.isha ?: "19:50"

    val fajrJammat = curMasjid.fajrJammatFixed ?: getEffectiveJammatTime("Fajr", fajrAzan, uiState.customJammatTimes, false, curMasjid)
    val zoharJammat = curMasjid.zoharJammatFixed ?: getEffectiveJammatTime("Dhuhr", zoharAzan, uiState.customJammatTimes, false, curMasjid)
    val jumahJammat = curMasjid.jumahJammatTime.ifBlank { "13:00" }
    val asrJammat = curMasjid.asrJammatFixed ?: getEffectiveJammatTime("Asr", asrAzan, uiState.customJammatTimes, false, curMasjid)
    val maghribJammat = curMasjid.maghribJammatFixed ?: getEffectiveJammatTime("Maghrib", maghribAzan, uiState.customJammatTimes, false, curMasjid)
    val ishaJammat = curMasjid.ishaJammatFixed ?: getEffectiveJammatTime("Isha", ishaAzan, uiState.customJammatTimes, false, curMasjid)

    val adminPrayers = listOf(
        AdminPrayerItem("Fajr", strings.fajr.uppercase(), Icons.Outlined.WbTwilight, fajrAzan, fajrJammat),
        AdminPrayerItem("Zohar", strings.dhuhr.uppercase(), Icons.Outlined.WbSunny, zoharAzan, zoharJammat),
        AdminPrayerItem("Jumah", strings.jumah, Icons.Outlined.WbSunny, jumahAzan, jumahJammat),
        AdminPrayerItem("Asr", strings.asr.uppercase(), Icons.Outlined.WbCloudy, asrAzan, asrJammat),
        AdminPrayerItem("Maghrib", strings.maghrib.uppercase(), Icons.Outlined.NightsStay, maghribAzan, maghribJammat),
        AdminPrayerItem("Isha", strings.isha.uppercase(), Icons.Outlined.Nightlight, ishaAzan, ishaJammat)
    )

    var editingPrayer by remember { mutableStateOf<AdminPrayerItem?>(null) }
    var showEditMasjidDialog by remember { mutableStateOf(false) }
    var showAddMasjidDialog by remember { mutableStateOf(false) }
    var showAppsScriptSettingsDialog by remember { mutableStateOf(false) }
    var showScriptCodeDialog by remember { mutableStateOf(false) }
    var showScriptUpdateNoticeDialog by remember { mutableStateOf(false) }
    var showAssetSuccessDialog by remember { mutableStateOf(false) }
    var isCloudSuccess by remember { mutableStateOf(false) }
    var isSyncingNow by remember { mutableStateOf(false) }
    var syncStatusErrorDialog by remember { mutableStateOf<String?>(null) }

    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedQrUri by remember { mutableStateOf<Uri?>(null) }
    var standardizedQr by remember { mutableStateOf<StandardizedQrResult?>(null) }
    var isProcessingQr by remember { mutableStateOf(false) }
    var isUploadingAssets by remember { mutableStateOf(false) }

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
            scope.launch {
                val res = prepareStandardizedMasjidQr(context, uri)
                standardizedQr = res
                isProcessingQr = false
            }
        } else {
            standardizedQr = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
            .islamicStarBackground(primaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val adminPanelTitle = when (uiState.language) {
                        "hi" -> "एडमिन पैनल"
                        "ur" -> "ایڈمن پینل"
                        else -> "ADMIN PANEL"
                    }
                    val adminPanelSubtitle = when (uiState.language) {
                        "hi" -> "नमाज़ अज़ान व जमात समय बदलें"
                        "ur" -> "نماز اذان اور جماعت کے اوقات تبدیل کریں"
                        else -> "Edit Namaz Azan & Jammat Timings"
                    }
                    Column {
                        Text(
                            text = adminPanelTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = adminPanelSubtitle,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val configuredUrl = uiState.appsScriptUrl.trim()
                            if (configuredUrl.isBlank()) {
                                showAppsScriptSettingsDialog = true
                                android.widget.Toast.makeText(
                                    context,
                                    strings.connect,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                isSyncingNow = true
                                scope.launch {
                                    val (success, msg) = viewModel.syncMasjidToSheet(uiState.selectedMasjid)
                                    isSyncingNow = false
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        if (success) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "✅ ${strings.timeUpdated}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            onBack()
                                        } else {
                                            syncStatusErrorDialog = msg
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSyncingNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF10B981),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        if (isSyncingNow) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Saving...",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                val doneLabel = when (uiState.language) {
                                    "ur" -> "مکمل"
                                    "hi" -> "पूर्ण"
                                    else -> "DONE"
                                }
                                Text(doneLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Language Switcher Row (Same as Home Page)
            LanguageCirclesRow(
                currentLang = uiState.language,
                onLangSelect = { viewModel.setLanguage(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content: Active Masjid Card + 6 Prayer Slots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Masjid Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mosque,
                                    contentDescription = null,
                                    tint = Color(0xFFF3DE8E),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = localizedCurMasjidName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF3DE8E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$localizedCurMasjidLoc • ID: #${curMasjid.id}",
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                adminPrayers.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522).copy(alpha = 0.88f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Icon + Prayer Name
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF1A2234), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.displayName,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Text(
                                    text = item.displayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextColor
                                )
                            }

                            // Center & Right: Azan & Jammat time + Pencil Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AzanJammatDisplay(
                                    azanTime24 = item.azanTime,
                                    jammatTime24 = item.jammatTime,
                                    isNext = false
                                )

                                IconButton(
                                    onClick = { editingPrayer = item },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit ${item.displayName}",
                                        tint = Color(0xFFF3DE8E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== MASJID PHOTO & QR SECTION ====================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522).copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFF3DE8E).copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .border(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFFF3DE8E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            val mediaUploadHeader = when (uiState.language) {
                                "hi" -> "मस्जिद फ़ोटो व क्यूआर अपलोड"
                                "ur" -> "مسجد کی تصویر اور QR اپ لوڈ"
                                else -> "MASJID MEDIA & QR UPLOAD"
                            }
                            val mediaUploadSubtitle = when (uiState.language) {
                                "hi" -> "$localizedCurMasjidName के लिए फ़ोटो व डोनेशन QR बदलें या अपलोड करें"
                                "ur" -> "$localizedCurMasjidName کے لیے تصویر اور عطیہ QR تبدیل یا اپ لوڈ کریں"
                                else -> "Upload or replace Photo & Donation QR for $localizedCurMasjidName"
                            }
                            Column {
                                Text(
                                    text = mediaUploadHeader,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DE8E),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = mediaUploadSubtitle,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        // --- 1. UPLOAD MASJID PHOTO ---
                        val photoSectionTitle = when (uiState.language) {
                            "hi" -> "मस्जिद फ़ोटो:"
                            "ur" -> "مسجد کی تصویر:"
                            else -> "Masjid Photo:"
                        }
                        val browsePhotoText = if (selectedPhotoUri != null) {
                            when (uiState.language) {
                                "hi" -> "नई फ़ोटो चुनी गई ✓ बदलें"
                                "ur" -> "نئی تصویر منتخب ہو گئی ✓ تبدیل کریں"
                                else -> "New Photo Selected ✓ Change"
                            }
                        } else {
                            when (uiState.language) {
                                "hi" -> "मस्जिद फ़ोटो ब्राउज़ / अपलोड करें"
                                "ur" -> "مسجد کی تصویر تلاش / اپ لوڈ کریں"
                                else -> "Browse / Upload Masjid Photo"
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = photoSectionTitle,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
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
                                    text = browsePhotoText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            if (selectedPhotoUri != null) {
                                val newPhotoSelectedTitle = when (uiState.language) {
                                    "hi" -> "नई फ़ोटो चुनी गई"
                                    "ur" -> "نئی تصویر منتخب ہوئی"
                                    else -> "New Photo Selected"
                                }
                                val readyToUploadText = when (uiState.language) {
                                    "hi" -> "सेव व अपलोड के लिए तैयार"
                                    "ur" -> "محفوظ اور اپ لوڈ کے لیے تیار"
                                    else -> "Ready to save & upload"
                                }
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
                                            contentDescription = "New Masjid Photo Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = newPhotoSelectedTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Text(
                                            text = readyToUploadText,
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
                            } else {
                                val hasActivePhoto = curMasjid.photoUrl.isNotBlank() || com.example.data.MasjidLocalCacheManager.hasLocalPhoto(context, curMasjid.id)
                                if (hasActivePhoto) {
                                    val activePhotoModel = remember(curMasjid.id, curMasjid.photoUrl, selectedPhotoUri) {
                                        com.example.data.MasjidLocalCacheManager.getPhotoModel(context, curMasjid.id, curMasjid.photoUrl)
                                    }
                                    val currentActivePhotoTitle = when (uiState.language) {
                                        "hi" -> "वर्तमान सक्रिय फ़ोटो"
                                        "ur" -> "موجودہ فعال تصویر"
                                        else -> "Current Active Photo"
                                    }
                                    val tapAboveToReplace = when (uiState.language) {
                                        "hi" -> "बदलने के लिए ऊपर दिए बटन पर टैप करें"
                                        "ur" -> "تبدیل کرنے کے لیے اوپر والے بٹن پر ٹیپ کریں"
                                        else -> "Tap above button to replace"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF141E2E), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(Color(0xFF0B132B), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = activePhotoModel,
                                                contentDescription = "Current Masjid Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentActivePhotoTitle,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF86EFAC)
                                            )
                                            Text(
                                                text = tapAboveToReplace,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- 2. UPLOAD MASJID QR ---
                        val qrSectionTitle = when (uiState.language) {
                            "hi" -> "मस्जिद क्यूआर कोड:"
                            "ur" -> "مسجد کیو آر کوڈ:"
                            else -> "Masjid QR Code:"
                        }
                        val browseQrText = if (selectedQrUri != null) {
                            when (uiState.language) {
                                "hi" -> "नया क्यूआर चुना गया ✓ बदलें"
                                "ur" -> "نیا QR منتخب ہو گیا ✓ تبدیل کریں"
                                else -> "New QR Selected ✓ Change"
                            }
                        } else {
                            when (uiState.language) {
                                "hi" -> "मस्जिद क्यूआर ब्राउज़ / अपलोड करें"
                                "ur" -> "مسجد کا QR تلاش / اپ لوڈ کریں"
                                else -> "Browse / Upload Masjid QR"
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = qrSectionTitle,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Button(
                                onClick = {
                                    qrPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
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
                                    text = browseQrText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            if (isProcessingQr) {
                                val processingQrText = when (uiState.language) {
                                    "hi" -> "क्यूआर को सीधा और साफ़ किया जा रहा है..."
                                    "ur" -> "QR کو سیدھا اور معیاری بنایا جا رہا ہے..."
                                    else -> "Standardizing & Straightening QR..."
                                }
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
                                        text = processingQrText,
                                        fontSize = 12.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            } else if (standardizedQr != null) {
                                val stdQrDoneText = if (standardizedQr!!.isCleanRegenerated) {
                                    when (uiState.language) {
                                        "hi" -> "✓ साफ़ मानक क्यूआर जनरेट हो गया"
                                        "ur" -> "✓ معیاری صاف QR تیار ہو گیا"
                                        else -> "✓ Standard Clean QR Generated"
                                    }
                                } else {
                                    when (uiState.language) {
                                        "hi" -> "✓ सीधा चौकोर क्यूआर क्रॉप किया गया"
                                        "ur" -> "✓ سیدھا چوکور QR تیار ہو گیا"
                                        else -> "✓ Straight Square QR Cropped"
                                    }
                                }
                                val readyQrText = when (uiState.language) {
                                    "hi" -> "सेव व अपलोड के लिए तैयार"
                                    "ur" -> "محفوظ اور اپ لوڈ کے لیے تیار"
                                    else -> "Ready to save & upload"
                                }
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
                                            text = stdQrDoneText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = if (standardizedQr!!.decodedPayload != null)
                                                "Payload: ${standardizedQr!!.decodedPayload!!.take(26)}..."
                                            else
                                                readyQrText,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.65f),
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedQrUri = null
                                            standardizedQr = null
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
                            } else {
                                val hasActiveQr = com.example.data.MasjidPaymentHelper.hasValidQr(curMasjid, context)
                                if (hasActiveQr) {
                                    val activeQrModel = remember(curMasjid.id, curMasjid.qrUrl, selectedQrUri, standardizedQr) {
                                        com.example.data.MasjidLocalCacheManager.getQrModel(context, curMasjid.id, curMasjid.qrUrl)
                                    }
                                    val currentActiveQrTitle = when (uiState.language) {
                                        "hi" -> "वर्तमान सक्रिय क्यूआर"
                                        "ur" -> "موجودہ فعال QR"
                                        else -> "Current Active QR"
                                    }
                                    val tapAboveToReplace = when (uiState.language) {
                                        "hi" -> "बदलने के लिए ऊपर दिए बटन पर टैप करें"
                                        "ur" -> "تبدیل کرنے کے لیے اوپر والے بٹن پر ٹیپ کریں"
                                        else -> "Tap above button to replace"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF141E2E), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(Color.White, RoundedCornerShape(6.dp))
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = activeQrModel,
                                                contentDescription = "Current Masjid QR",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentActiveQrTitle,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF86EFAC)
                                            )
                                            Text(
                                                text = tapAboveToReplace,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- 3. SAVE & UPLOAD BUTTON ---
                        val canUpload = selectedPhotoUri != null || standardizedQr != null || selectedQrUri != null
                        val saveUploadText = when (uiState.language) {
                            "hi" -> "मीडिया सेव व अपलोड करें"
                            "ur" -> "میڈیا محفوظ اور اپ لوڈ کریں"
                            else -> "Save & Upload Media"
                        }
                        val savingUploadText = when (uiState.language) {
                            "hi" -> "मीडिया सेव व अपलोड हो रहा है..."
                            "ur" -> "میڈیا محفوظ اور اپ لوڈ ہو رہا ہے..."
                            else -> "Saving & Uploading Media..."
                        }
                        Button(
                            onClick = {
                                if (!canUpload) return@Button
                                isUploadingAssets = true
                                scope.launch {
                                    var photoBase64: String? = null
                                    val pUri = selectedPhotoUri
                                    if (pUri != null) {
                                        try {
                                            photoBase64 = prepareStraightMasjidPhotoBase64(context, pUri)
                                        } catch (_: Throwable) {}
                                    }

                                    var qrBase64: String? = standardizedQr?.base64
                                    val qUri = selectedQrUri
                                    if (qrBase64 == null && qUri != null) {
                                        try {
                                            val res = prepareStandardizedMasjidQr(context, qUri)
                                            qrBase64 = res?.base64 ?: prepareStraightMasjidPhotoBase64(context, qUri)
                                        } catch (_: Throwable) {}
                                    }

                                    val configuredUrl = uiState.appsScriptUrl.trim()
                                    val targetUrl = configuredUrl.ifBlank { com.example.data.GoogleSheetMasjidSync.APPS_SCRIPT_WEBAPP_URL }

                                    // 1. Immediately replace and save in local files & evict Coil cache
                                    val localPhotoSaved = if (pUri != null) {
                                        com.example.data.MasjidLocalCacheManager.replaceMasjidPhotoLocally(context, curMasjid.id, pUri)
                                    } else false

                                    val localQrSaved = if (standardizedQr != null) {
                                        com.example.data.MasjidLocalCacheManager.replaceMasjidQrLocally(context, curMasjid.id, standardizedQr!!.bitmap)
                                    } else if (qUri != null) {
                                        com.example.data.MasjidLocalCacheManager.replaceMasjidQrLocally(context, curMasjid.id, qUri)
                                    } else false

                                    // 2. Immediately update in ViewModel state
                                    val localPhotoPath = if (localPhotoSaved) com.example.data.MasjidLocalCacheManager.getLocalPhotoFile(context, curMasjid.id).absolutePath else pUri?.toString()
                                    val localQrPath = if (localQrSaved) com.example.data.MasjidLocalCacheManager.getLocalQrFile(context, curMasjid.id).absolutePath else (standardizedQr?.localFilePath ?: qUri?.toString())

                                    viewModel.updateMasjidPhotoAndQr(
                                        id = curMasjid.id,
                                        photoUrl = localPhotoPath,
                                        qrUrl = localQrPath
                                    )

                                    // 3. Upload to Google Drive and update Sheet
                                    val (success, msg) = com.example.data.GoogleSheetMasjidSync.uploadMasjidAssets(
                                        masjidId = curMasjid.id,
                                        masjidName = curMasjid.name,
                                        photoBase64 = photoBase64,
                                        qrBase64 = qrBase64,
                                        webAppUrl = targetUrl
                                    )

                                    isUploadingAssets = false
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        selectedPhotoUri = null
                                        selectedQrUri = null
                                        standardizedQr = null
                                        
                                        isCloudSuccess = success
                                        showAssetSuccessDialog = true
                                    }
                                }
                            },
                            enabled = canUpload && !isUploadingAssets,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                disabledContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            if (isUploadingAssets) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = savingUploadText,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = if (canUpload) Color.White else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = saveUploadText,
                                    color = if (canUpload) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Edit Dialog
        editingPrayer?.let { prayer ->
            EditPrayerTimingDialog(
                prayer = prayer,
                language = uiState.language,
                onDismiss = { editingPrayer = null },
                onSave = { newAzan, newJammat ->
                    val updatingMsg = when (uiState.language) {
                        "hi" -> "सभी मोबाइल के लिए समय अपडेट हो रहा है..."
                        "ur" -> "تمام موبائلز کے اوقات اپ ڈیٹ ہو رہے ہیں..."
                        else -> "Updating timings for all mobiles..."
                    }
                    Toast.makeText(context, updatingMsg, Toast.LENGTH_SHORT).show()
                    viewModel.updatePrayerLocally(prayer.systemName, newAzan, newJammat) { success, _ ->
                        if (success) {
                            val successMsg = when (uiState.language) {
                                "hi" -> "समय सफलतापूर्वक अपडेट हो गया! सभी मोबाइल पर नया समय बदल चुका है।"
                                "ur" -> "اوقات کامیابی سے اپ ڈیٹ ہو گئے! تمام موبائلز پر نیا وقت تبدیل ہو گیا ہے۔"
                                else -> "Timings updated successfully! New times are now synced on all mobiles."
                            }
                            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                        } else {
                            val savedMsg = when (uiState.language) {
                                "hi" -> "समय सुरक्षित हो गया।"
                                "ur" -> "وقت محفوظ ہو گیا۔"
                                else -> "Timings saved successfully."
                            }
                            Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    editingPrayer = null
                }
            )
        }

        // Edit Masjid Dialog
        if (showEditMasjidDialog) {
            MasjidInfoDialog(
                initialMasjid = curMasjid,
                isNew = false,
                onDismiss = { showEditMasjidDialog = false },
                onSave = { id, name, address, photoUrl ->
                    viewModel.saveOrUpdateMasjid(id, name, address, photoUrl)
                    android.widget.Toast.makeText(context, "$name updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    showEditMasjidDialog = false
                }
            )
        }

        // Add Masjid Dialog
        if (showAddMasjidDialog) {
            MasjidInfoDialog(
                initialMasjid = null,
                isNew = true,
                onDismiss = { showAddMasjidDialog = false },
                onSave = { id, name, address, photoUrl ->
                    viewModel.saveOrUpdateMasjid(id, name, address, photoUrl)
                    val addedMsg = when (uiState.language) {
                        "ur" -> "مسجد شامل ہوگئی ($name)"
                        "hi" -> "मस्जिद जुड़ गई ($name)"
                        else -> "$name added successfully!"
                    }
                    android.widget.Toast.makeText(context, addedMsg, android.widget.Toast.LENGTH_SHORT).show()
                    showAddMasjidDialog = false
                }
            )
        }

        // Asset Success Dialog (Matches "Request Received!" style)
        if (showAssetSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showAssetSuccessDialog = false },
                containerColor = Color(0xFF0F241A),
                shape = RoundedCornerShape(22.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                            .border(2.dp, Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (isCloudSuccess) "Updated Successfully!" else "Saved Locally!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF34D399),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Aapki photo/QR mobile me update ho chuki hai aur Home Page par ab nayi image dikhegi.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        
                        if (!isCloudSuccess) {
                            Text(
                                text = "Google Drive cloud-sync ke liye purana Apps Script update karna hoga.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFFBBF24),
                                textAlign = TextAlign.Center
                            )
                            
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(com.example.data.GoogleSheetMasjidSync.APPS_SCRIPT_SAMPLE_CODE))
                                    Toast.makeText(context, "✅ Latest Apps Script Code Copied!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Script Code", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAssetSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }

        // Apps Script Settings Dialog
        if (showAppsScriptSettingsDialog) {
            AppsScriptSettingsDialog(
                currentUrl = uiState.appsScriptUrl,
                onDismiss = { showAppsScriptSettingsDialog = false },
                onSave = { newUrl ->
                    viewModel.setAppsScriptUrl(newUrl)
                    android.widget.Toast.makeText(context, "Apps Script URL saved!", android.widget.Toast.LENGTH_SHORT).show()
                },
                onTest = { testUrl ->
                    viewModel.testAppsScriptConnection(testUrl)
                },
                onShowScriptCode = {
                    showScriptCodeDialog = true
                }
            )
        }

        // Apps Script Code Guide Dialog
        if (showScriptCodeDialog) {
            AppsScriptCodeDialog(
                onDismiss = { showScriptCodeDialog = false }
            )
        }

        // Sync Error Dialog with Direct Action
        syncStatusErrorDialog?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { syncStatusErrorDialog = null },
                containerColor = Color(0xFF1E293B),
                title = {
                    Text(
                        text = "Google Sheet Sync Error",
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (errorMsg == "APPS_SCRIPT_NOT_SET") {
                                "Google Sheet par live save hone ke liye WebApp URL configure hona zaroori hai."
                            } else {
                                "Google Sheet se connect nahi ho paya:\n\n$errorMsg"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Aap 'Setup WebApp URL' par click karke apna URL check ya set kar sakte hain.",
                            color = Color(0xFFF3DE8E),
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            syncStatusErrorDialog = null
                            showAppsScriptSettingsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Setup WebApp URL", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { syncStatusErrorDialog = null }) {
                        Text("Dismiss", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }
    }
}

@Composable
fun AppsScriptSettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTest: suspend (String) -> Pair<Boolean, String>,
    onShowScriptCode: () -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, Color(0xFFF3DE8E).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GOOGLE SHEET APPS SCRIPT URL",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF3DE8E),
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Apne Google Sheet ka deployed Web App URL yahan paste karein taaki Namaz timings Google Sheet par save ho sakein.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { 
                        urlText = it
                        testResult = null
                    },
                    label = { Text("Apps Script WebApp URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                urlText = clipText.trim()
                                testResult = null
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFFF3DE8E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste URL", color = Color(0xFFF3DE8E), fontSize = 12.sp)
                    }

                    TextButton(onClick = onShowScriptCode) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF86EFAC), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Script Code", color = Color(0xFF86EFAC), fontSize = 12.sp)
                    }
                }

                testResult?.let { (success, msg) ->
                    Text(
                        text = if (success) "🟢 $msg" else "🔴 $msg",
                        fontSize = 11.5.sp,
                        color = if (success) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            coroutineScope.launch {
                                testResult = onTest(urlText)
                                isTesting = false
                            }
                        },
                        enabled = !isTesting && urlText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.6f))
                    ) {
                        Text(if (isTesting) "Testing..." else "Test URL", color = Color(0xFFF3DE8E), fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onSave(urlText)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3DE8E))
                    ) {
                        Text("Save URL", color = Color(0xFF0C101B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AppsScriptCodeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val code = GoogleSheetMasjidSync.APPS_SCRIPT_SAMPLE_CODE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, Color(0xFF86EFAC).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "APPS SCRIPT SETUP GUIDE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF86EFAC),
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = "Follow these 4 simple steps to connect your Google Sheet:\n" +
                            "1. Open your Google Sheet in browser.\n" +
                            "2. Click Extensions > Apps Script.\n" +
                            "3. Replace all code with the script below and Save 💾.\n" +
                            "4. Click Deploy > New deployment > Web App > Execute as: Me, Who has access: Anyone > Deploy.\n" +
                            "5. Copy the Web App URL and paste it into 'Set WebApp URL' in this app.",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF070B12), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = code,
                        fontSize = 10.5.sp,
                        color = Color(0xFF86EFAC),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            android.widget.Toast.makeText(context, "Apps Script code copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF86EFAC))
                    ) {
                        Text("Copy Code", color = Color(0xFF0C101B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text("Close", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MasjidInfoDialog(
    initialMasjid: MasjidItem?,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (id: String, name: String, address: String, photoUrl: String) -> Unit
) {
    var id by remember { mutableStateOf(initialMasjid?.id ?: "") }
    var name by remember { mutableStateOf(initialMasjid?.name ?: "") }
    var address by remember { mutableStateOf(initialMasjid?.area ?: "") }
    var photoUrl by remember { mutableStateOf(initialMasjid?.photoUrl ?: "") }
    var errorText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isNew) "ADD NEW MASJID" else "EDIT MASJID INFO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Saved masajid display on Home page and sync with Google Sheet & Drive",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = "" },
                    label = { Text("Masjid Name") },
                    placeholder = { Text("e.g. Mohammadiya Masjid") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; errorText = "" },
                    label = { Text("Address / Area") },
                    placeholder = { Text("e.g. Swagat Nagar") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF3DE8E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFF3DE8E),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it; errorText = "" },
                    label = { Text("Masjid ID") },
                    placeholder = { Text("e.g. 100111111") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF86EFAC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF86EFAC),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it; errorText = "" },
                    label = { Text("Google Drive Photo Link") },
                    placeholder = { Text("https://drive.google.com/file/d/...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF86EFAC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF86EFAC),
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = "Please enter Masjid Name"
                                return@Button
                            }
                            if (address.isBlank()) {
                                errorText = "Please enter Address"
                                return@Button
                            }
                            if (id.isBlank()) {
                                errorText = "Please enter Masjid ID"
                                return@Button
                            }
                            onSave(id, name, address, photoUrl)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text("Save Masjid", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPrayerTimingDialog(
    prayer: AdminPrayerItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    language: String = "en"
) {
    val isFajr = prayer.systemName.equals("Fajr", ignoreCase = true)
    val defaultIsPm = !isFajr

    fun parseTimeParts(rawTime: String, fallbackPm: Boolean): Triple<String, String, Boolean> {
        val clean = rawTime.trim()
        val parts = clean.split(":")
        if (parts.size != 2) return Triple("08", "00", fallbackPm)
        val rawH = parts[0].toIntOrNull() ?: 8
        val m = parts[1].toIntOrNull() ?: 0
        val isPm = when {
            rawH in 13..23 -> true
            rawH == 12 -> true
            rawH == 0 -> false
            else -> fallbackPm
        }
        val h12 = when {
            rawH == 0 -> 12
            rawH > 12 -> rawH - 12
            else -> rawH
        }
        return Triple(
            String.format(Locale.US, "%02d", h12),
            String.format(Locale.US, "%02d", m),
            isPm
        )
    }

    val (initAzanH, initAzanM, _) = remember(prayer) {
        parseTimeParts(prayer.azanTime, defaultIsPm)
    }
    val (initJammatH, initJammatM, _) = remember(prayer) {
        parseTimeParts(prayer.jammatTime, defaultIsPm)
    }

    var azanHours by remember { mutableStateOf(initAzanH) }
    var azanMinutes by remember { mutableStateOf(initAzanM) }
    var azanIsPm by remember { mutableStateOf(defaultIsPm) }

    var jammatHours by remember { mutableStateOf(initJammatH) }
    var jammatMinutes by remember { mutableStateOf(initJammatM) }
    var jammatIsPm by remember { mutableStateOf(defaultIsPm) }

    var errorText by remember { mutableStateOf("") }

    fun to24Hour(hStr: String, mStr: String, isPm: Boolean): String? {
        val h = hStr.trim().toIntOrNull() ?: return null
        val m = mStr.trim().toIntOrNull() ?: return null
        if (m !in 0..59) return null
        val finalH = if (h in 13..23) {
            h
        } else if (h in 1..12) {
            if (isPm) {
                if (h == 12) 12 else h + 12
            } else {
                if (h == 12) 0 else h
            }
        } else if (h == 0) {
            0
        } else {
            return null
        }
        return String.format(Locale.US, "%02d:%02d", finalH, m)
    }

    val validAzan = to24Hour(azanHours, azanMinutes, azanIsPm)
    val validJammat = to24Hour(jammatHours, jammatMinutes, jammatIsPm)
    val previewAzan = if (validAzan != null) formatTo12Hour(validAzan) else "--:--"
    val previewJammat = if (validJammat != null) formatTo12Hour(validJammat) else "--:--"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Mosque Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = prayer.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val editPrayerHeading = when (language) {
                    "hi" -> "${prayer.displayName} समय बदलें"
                    "ur" -> "${prayer.displayName} وقت تبدیل کریں"
                    else -> "EDIT ${prayer.displayName.uppercase()}"
                }
                Text(
                    text = editPrayerHeading,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = when (language) {
                        "hi" -> "अज़ान और जमात के लिए घंटे व मिनट अलग-अलग"
                        "ur" -> "اذان اور جماعت کے لیے گھنٹے اور منٹ الگ الگ"
                        else -> "Hours & Minutes separate for Azan and Jammat"
                    },
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. AZAN TIME SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val azanSectionTitle = when (language) {
                            "hi" -> "अज़ान का समय"
                            "ur" -> "اذان کا وقت"
                            else -> "AZAN TIME"
                        }
                        val hrsLabel = when (language) {
                            "hi" -> "1: घंटा"
                            "ur" -> "1: گھنٹہ"
                            else -> "1st: HRS"
                        }
                        val minLabel = when (language) {
                            "hi" -> "2: मिनट"
                            "ur" -> "2: منٹ"
                            else -> "2nd: MIN"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFF3DE8E), CircleShape)
                                )
                                Text(
                                    text = azanSectionTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DE8E),
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = previewAzan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3DE8E)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Azan Hours and Minutes TextFields + AM/PM
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1st Text Field: Hours (e.g. 08)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = azanHours,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            azanHours = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("08", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF3DE8E),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = hrsLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DE8E)
                                )
                            }

                            Text(
                                text = ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF3DE8E),
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                            )

                            // 2nd Text Field: Minutes (e.g. 55)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = azanMinutes,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            azanMinutes = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("55", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF3DE8E),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = minLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DE8E)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // AM / PM Switcher: For Fajr only AM, for rest only PM
                            Box(
                                modifier = Modifier.padding(bottom = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFajr) {
                                    Surface(
                                        color = Color(0xFFF3DE8E),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "AM",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFFF3DE8E),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "PM",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. JAMMAT TIME SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val jammatSectionTitle = when (language) {
                            "hi" -> "जमात का समय"
                            "ur" -> "جماعت کا وقت"
                            else -> "JAMMAT TIME"
                        }
                        val hrsLabel = when (language) {
                            "hi" -> "1: घंटा"
                            "ur" -> "1: گھنٹہ"
                            else -> "1st: HRS"
                        }
                        val minLabel = when (language) {
                            "hi" -> "2: मिनट"
                            "ur" -> "2: منٹ"
                            else -> "2nd: MIN"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF86EFAC), CircleShape)
                                )
                                Text(
                                    text = jammatSectionTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC),
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = previewJammat,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Jammat Hours and Minutes TextFields + AM/PM
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1st Text Field: Hours (e.g. 08)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = jammatHours,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            jammatHours = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("08", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF86EFAC),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = hrsLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC)
                                )
                            }

                            Text(
                                text = ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF86EFAC),
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                            )

                            // 2nd Text Field: Minutes (e.g. 55)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = jammatMinutes,
                                    onValueChange = {
                                        if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                            jammatMinutes = it
                                            errorText = ""
                                        }
                                    },
                                    placeholder = { Text("55", fontSize = 18.sp, color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF86EFAC),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(76.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = minLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // AM / PM Switcher: For Fajr only AM, for rest only PM
                            Box(
                                modifier = Modifier.padding(bottom = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFajr) {
                                    Surface(
                                        color = Color(0xFF86EFAC),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "AM",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFF86EFAC),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "PM",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorText,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val syncNoticeText = when (language) {
                    "hi" -> "OK / DONE दबाते ही सभी मोबाइल में एक साथ नया समय अपडेट हो जाएगा"
                    "ur" -> "OK / DONE دباتے ہی تمام موبائلز میں ایک ساتھ نیا وقت تبدیل ہو جائے گا"
                    else -> "Tapping OK / DONE will immediately update timings on all mobiles"
                }
                Text(
                    text = syncNoticeText,
                    fontSize = 11.5.sp,
                    color = Color(0xFF86EFAC),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = when (language) {
                                "hi" -> "रद्द करें"
                                "ur" -> "منسوخ کریں"
                                else -> "Cancel"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (validAzan == null) {
                                errorText = when (language) {
                                    "hi" -> "कृपया सही अज़ान घंटे (01-12) और मिनट (00-59) दर्ज करें"
                                    "ur" -> "براہ کرم درست اذان کے گھنٹے (01-12) اور منٹ (00-59) درج کریں"
                                    else -> "Please enter valid Azan hours (01-12) and minutes (00-59)"
                                }
                                return@Button
                            }
                            if (validJammat == null) {
                                errorText = when (language) {
                                    "hi" -> "कृपया सही जमात घंटे (01-12) और मिनट (00-59) दर्ज करें"
                                    "ur" -> "براہ کرم درست جماعت کے گھنٹے (01-12) اور منٹ (00-59) درج کریں"
                                    else -> "Please enter valid Jammat hours (01-12) and minutes (00-59)"
                                }
                                return@Button
                            }
                            onSave(validAzan, validJammat)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        val okDoneLabel = when (language) {
                            "hi" -> "ठीक है / पूर्ण"
                            "ur" -> "ٹھیک ہے / مکمل"
                            else -> "OK / DONE"
                        }
                        Text(okDoneLabel, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun NavCircleItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String,
    enabled: Boolean = true
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "navScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    when {
                        selected -> Color(0xFFF3DE8E)
                        !enabled -> Color(0xFF14261B).copy(alpha = 0.5f)
                        else -> Color(0xFF14261B)
                    }
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = when {
                        selected -> Color(0xFFFFD700)
                        !enabled -> Color(0xFFF3DE8E).copy(alpha = 0.15f)
                        else -> Color(0xFFF3DE8E).copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    selected -> Color(0xFF0F2618)
                    !enabled -> Color(0xFFF3DE8E).copy(alpha = 0.4f)
                    else -> Color(0xFFF3DE8E).copy(alpha = 0.85f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = when {
                selected -> Color(0xFFF3DE8E)
                !enabled -> Color.White.copy(alpha = 0.35f)
                else -> Color.White.copy(alpha = 0.65f)
            },
            textAlign = TextAlign.Center
        )
    }
}


