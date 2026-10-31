package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.R
import java.util.Locale

/**
 * Custom Notification Bubble Shape matching the user's attachment (notification_64982e0c.jpg):
 * Rounded rectangle body with a triangular pointer/tail at the bottom center.
 */
class NotificationBubbleShape(
    private val cornerRadius: Dp = 9.dp,
    private val pointerWidth: Dp = 10.dp,
    private val pointerHeight: Dp = 5.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val pw = with(density) { pointerWidth.toPx() }
        val ph = with(density) { pointerHeight.toPx() }
        val w = size.width
        val h = size.height
        val bodyBottom = (h - ph).coerceAtLeast(0f)

        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            quadraticTo(w, 0f, w, r)
            lineTo(w, bodyBottom - r)
            quadraticTo(w, bodyBottom, w - r, bodyBottom)
            lineTo(w / 2f + pw / 2f, bodyBottom)
            lineTo(w / 2f, h)
            lineTo(w / 2f - pw / 2f, bodyBottom)
            lineTo(r, bodyBottom)
            quadraticTo(0f, bodyBottom, 0f, bodyBottom - r)
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Floating Support Masjid Button:
 * - Shape: Notification bubble with bottom pointer (matching notification_64982e0c.jpg)
 * - Icon: Still hand at bottom + pulsing heart hovering above with clear air gap (refined slightly smaller)
 * - Aligned on right side with Admin button
 * - Moves with the sliding card
 */
@Composable
fun SupportMasjidFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "supportHeartbeat")

    val heartPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1300
                1.0f at 0 using FastOutSlowInEasing
                1.25f at 160 using FastOutSlowInEasing
                1.06f at 300 using FastOutSlowInEasing
                1.20f at 440 using FastOutSlowInEasing
                1.0f at 600 using FastOutSlowInEasing
                1.0f at 1300
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartPulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val bubbleShape = remember { NotificationBubbleShape(cornerRadius = 9.dp, pointerWidth = 10.dp, pointerHeight = 5.dp) }

    // Notification Bubble Button (matching notification_64982e0c.jpg)
    Box(
        modifier = modifier
            .width(42.dp)
            .height(38.dp)
            .shadow(
                elevation = 8.dp,
                shape = bubbleShape,
                spotColor = Color(0xFFF3DE8E),
                ambientColor = Color(0xFFF3DE8E).copy(alpha = glowAlpha)
            )
            .clip(bubbleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF13281C),
                        Color(0xFF09160F)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFF2BD),
                        Color(0xFFF3DE8E),
                        Color(0xFFC99B2D)
                    )
                ),
                shape = bubbleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color(0xFFF3DE8E)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Icon placed cleanly inside the upper body of the bubble (above the bottom pointer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Container for Hand and Heart - refined slightly smaller ("thoda chota")
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // 1. Still Hand at the bottom
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = "Support Masjid",
                    tint = Color(0xFFF3DE8E),
                    modifier = Modifier
                        .size(20.dp)
                        .offset(y = 2.dp)
                        .drawWithContent {
                            // Clip out top half so only the still hand shows
                            clipRect(
                                left = 0f,
                                top = size.height * 0.48f,
                                right = size.width,
                                bottom = size.height
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        }
                )

                // 2. Beating Heart floating above the hand with a clear visible air gap
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier
                        .size(8.5.dp)
                        .offset(y = (-4.5).dp)
                        .scale(heartPulseScale)
                )
            }
        }
    }
}

/**
 * Animated Support Masjid Popup:
 * - Animated origin expands out from the button on open, and shrinks back into the button on close
 * - Background scrim with lighter opacity (as requested: "Background opacity kam hogi")
 * - Top: Scan QR code to donate
 * - Bottom: Same function and design as attached image (₹50, ₹100, ₹250, ₹500, Other + Pay button via UPI)
 */
@Composable
fun SupportMasjidPopup(
    language: String,
    qrUrl: String? = null,
    selectedMasjid: com.example.data.MasjidItem? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasValidQr = remember(selectedMasjid) {
        com.example.data.MasjidPaymentHelper.hasValidQr(selectedMasjid)
    }
    val resolvedQrModel: Any? = remember(selectedMasjid, qrUrl) {
        com.example.data.MasjidPaymentHelper.getMasjidQrModel(selectedMasjid)
    }

    // Background decoding if a remote QR image needs to be parsed
    LaunchedEffect(selectedMasjid) {
        val remoteUrl = selectedMasjid?.qrUrl?.trim() ?: ""
        if (remoteUrl.startsWith("http") && selectedMasjid?.id != "100111111") {
            com.example.data.MasjidPaymentHelper.decodeAndCacheFromUrl(remoteUrl)
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    var selectedAmount by remember { mutableIntStateOf(250) }
    var showOtherDialog by remember { mutableStateOf(false) }
    var showUpiAppChooser by remember { mutableStateOf(false) }

    // Trigger open animation on launch
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Dismiss with exit animation back into the button
    fun requestClose() {
        isVisible = false
    }

    val animProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        finishedListener = { progress ->
            if (progress == 0f) {
                onDismiss()
            }
        },
        label = "popupAnimation"
    )

    BackHandler {
        requestClose()
    }

    fun launchUpiForApp(packageName: String?, amount: Int) {
        val upiUri = com.example.data.MasjidPaymentHelper.buildMasjidUpiUri(selectedMasjid, amount)
        if (upiUri == null) {
            Toast.makeText(context, "QR code not available for this masjid", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, upiUri)
        if (!packageName.isNullOrBlank()) {
            intent.setPackage(packageName)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, upiUri)
                context.startActivity(Intent.createChooser(fallbackIntent, "Pay with UPI"))
            } catch (e2: Exception) {
                Toast.makeText(context, "No UPI app found. Please scan the QR code.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Scrim with reduced opacity: "Background opacity kam hogi"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.36f * animProgress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { requestClose() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Popup Card with origin expanding from and shrinking back into the button (TopEnd ~ x: 0.90, y: 0.40)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071711)),
                border = BorderStroke(1.3.dp, Color(0xFFE5B842).copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume clicks inside card */ }
                    )
                    .graphicsLayer {
                        // Origin positioned towards top-right QR button location (x: 0.86, y: 0.05)
                        transformOrigin = TransformOrigin(pivotFractionX = 0.86f, pivotFractionY = 0.05f)
                        scaleX = lerp(0.06f, 1f, animProgress)
                        scaleY = lerp(0.06f, 1f, animProgress)
                        alpha = animProgress.coerceIn(0f, 1f)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: SUPPORT MASJID (Center Aligned & Bold) + Close Button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val masjidTitle = selectedMasjid?.name?.uppercase(Locale.ROOT)?.ifBlank { "SUPPORT MASJID" } ?: "SUPPORT MASJID"
                        val masjidLoc = selectedMasjid?.address ?: ""
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp)
                        ) {
                            Text(
                                text = masjidTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3DE8E),
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.8.sp,
                                maxLines = 1
                            )
                            if (masjidLoc.isNotBlank()) {
                                Text(
                                    text = masjidLoc,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { requestClose() },
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                    // Upper Section: QR Code (Double Size: 310.dp) or Azan Time Logo & Punchline if no QR
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(310.dp)
                                .background(if (hasValidQr) Color.White else Color(0xFF0F1A15), RoundedCornerShape(14.dp))
                                .border(1.5.dp, Color(0xFFE5B842).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasValidQr && resolvedQrModel != null) {
                                if (resolvedQrModel is android.graphics.Bitmap) {
                                    Image(
                                        bitmap = resolvedQrModel.asImageBitmap(),
                                        contentDescription = "Masjid Donation QR Code",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    AsyncImage(
                                        model = resolvedQrModel,
                                        contentDescription = "Masjid Donation QR Code",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF1E3328), Color(0xFF0D1712))
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(
                                                BorderStroke(
                                                    1.5.dp,
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFFF3DE8E), Color(0xFFB37C3C))
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.foreground),
                                            contentDescription = "Azan Time Logo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    com.example.ui.AzanTimeWordmark(
                                         height = 30.dp,
                                         topPadding = 4.dp,
                                         bottomPadding = 4.dp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (hasValidQr) "Scan QR using any UPI app (GPay, PhonePe, Paytm)" else "QR code not available for this masjid",
                            fontSize = 11.sp,
                            color = if (hasValidQr) Color.White.copy(alpha = 0.85f) else Color(0xFFEF4444).copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Lower Section: Contribution Amount Buttons (Enlarged)
                    val amounts = listOf(50, 100, 250, 500)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { if (!hasValidQr) it.alpha(0.4f) else it },
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        amounts.forEach { amt ->
                            val isSelected = selectedAmount == amt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected && hasValidQr) Color(0xFFE5B842) else Color(0xFF0F261C))
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isSelected && hasValidQr) Color(0xFFFFE082) else Color(0xFF224836),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .let { if (hasValidQr) it.clickable { selectedAmount = amt } else it },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹$amt",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected && hasValidQr) Color(0xFF0F1713) else Color(0xFFF3DE8E)
                                )
                            }
                        }

                        val isOtherSelected = !amounts.contains(selectedAmount)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isOtherSelected && hasValidQr) Color(0xFFE5B842) else Color(0xFF0F261C))
                                .border(
                                    width = 1.2.dp,
                                    color = if (isOtherSelected && hasValidQr) Color(0xFFFFE082) else Color(0xFF224836),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .let { if (hasValidQr) it.clickable { showOtherDialog = true } else it },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOtherSelected) "₹$selectedAmount" else "Other",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOtherSelected && hasValidQr) Color(0xFF0F1713) else Color(0xFFF3DE8E)
                            )
                        }
                    }

                    // Pay UPI Button matching attached image style (Enlarged)
                    Button(
                        onClick = {
                            if (hasValidQr) {
                                showUpiAppChooser = true
                            } else {
                                Toast.makeText(context, "QR code not available for this masjid", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasValidQr) Color(0xFF123425) else Color(0xFF141F1A),
                            disabledContainerColor = Color(0xFF141F1A)
                        ),
                        enabled = hasValidQr,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, if (hasValidQr) Color(0xFFE5B842) else Color(0xFF1E3328)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (hasValidQr) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_upi),
                                    contentDescription = "UPI Logo",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (hasValidQr) "Pay ₹$selectedAmount via UPI (GPay, PhonePe, Paytm)" else "QR Code Not Available",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasValidQr) Color(0xFFF3DE8E) else Color.White.copy(alpha = 0.4f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // Other Amount Input Dialog
    if (showOtherDialog) {
        var customAmountText by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { showOtherDialog = false },
            properties = DialogProperties(decorFitsSystemWindows = false)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E17)),
                border = BorderStroke(1.dp, Color(0xFFF3DE8E).copy(alpha = 0.5f)),
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
                        color = Color(0xFFF3DE8E)
                    )
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (₹)", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("e.g. 1000", color = Color.White.copy(alpha = 0.3f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF3DE8E),
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
                            onClick = { showOtherDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val entered = customAmountText.toIntOrNull()
                                if (entered != null && entered > 0) {
                                    selectedAmount = entered
                                    showOtherDialog = false
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B842)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set Amount", color = Color(0xFF0F1713), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // UPI App Chooser Dialog
    if (showUpiAppChooser) {
        UpiAppChooserDialog(
            amount = selectedAmount,
            onDismiss = { showUpiAppChooser = false },
            onAppSelected = { pkg ->
                showUpiAppChooser = false
                launchUpiForApp(pkg, selectedAmount)
            }
        )
    }
}
