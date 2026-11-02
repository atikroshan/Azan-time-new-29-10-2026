package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.AppUpdateManager
import com.example.util.UpdateInfo
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val primaryGold = Color(0xFFF3DE8E)

    var availableVersions by remember { mutableStateOf(listOf(updateInfo)) }
    var selectedVersion by remember { mutableStateOf(updateInfo) }
    var isLoadingVersions by remember { mutableStateOf(true) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val fetched = AppUpdateManager.fetchAllAvailableVersions()
        if (fetched.isNotEmpty()) {
            availableVersions = fetched
            selectedVersion = fetched.firstOrNull { it.latestVersionName == updateInfo.latestVersionName } ?: fetched.first()
        }
        isLoadingVersions = false
    }

    Dialog(onDismissRequest = {
        if (!isDownloading) onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, primaryGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(primaryGold.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, primaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Version Manager & Restore",
                        tint = primaryGold,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title
                Text(
                    text = when (language) {
                        "ur" -> "ورژن مینیجر اور ریستور (v${selectedVersion.latestVersionName})"
                        "hi" -> "वर्जन मैनेजर और रीस्टोर (v${selectedVersion.latestVersionName})"
                        else -> "Version Manager & Restore (v${selectedVersion.latestVersionName})"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryGold,
                    textAlign = TextAlign.Center
                )

                // Version Selection Selector
                if (isLoadingVersions) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = primaryGold
                    )
                } else if (availableVersions.size > 1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (language) {
                                "ur" -> "ورژن منتخب کریں (Select Version):"
                                "hi" -> "वर्जन चुनें (Select Version):"
                                else -> "Select Version to Preview & Restore:"
                            },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableVersions, key = { it.latestVersionName }) { ver ->
                                val isSelected = ver.latestVersionName == selectedVersion.latestVersionName
                                Surface(
                                    onClick = { selectedVersion = ver },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) primaryGold else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSelected) primaryGold else Color.White.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = "v${ver.latestVersionName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Preview Box (Changelog / Release Notes as per selected version)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162238)),
                    border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (language) {
                                "ur" -> "پیش نظارہ (Preview & Changelog):"
                                "hi" -> "प्रीव्यू (Preview & Changelog):"
                                else -> "Preview & Release Notes:"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold
                        )
                        Text(
                            text = if (selectedVersion.changelog.isNotBlank()) {
                                selectedVersion.changelog
                            } else {
                                "No release notes available for version v${selectedVersion.latestVersionName}."
                            },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF10B981),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "Restoring / Downloading v${selectedVersion.latestVersionName}... $downloadProgress%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                if (downloadError != null) {
                    Text(
                        text = downloadError ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Actions: Restore / Update and Cancel
                if (!isDownloading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = when (language) {
                                    "ur" -> "منسوخ"
                                    "hi" -> "रद्द करें"
                                    else -> "Cancel"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedVersion.downloadUrl.isBlank()) {
                                    downloadError = "This is the current active version or no download URL is available."
                                    return@Button
                                }
                                isDownloading = true
                                downloadError = null
                                coroutineScope.launch {
                                    val destination = File(context.cacheDir, "AzanTime_v${selectedVersion.latestVersionName}.apk")
                                    val success = AppUpdateManager.downloadApk(
                                        downloadUrl = selectedVersion.downloadUrl,
                                        destinationFile = destination,
                                        onProgress = { percent ->
                                            downloadProgress = percent
                                        }
                                    )
                                    isDownloading = false
                                    if (success) {
                                        onDismiss()
                                        AppUpdateManager.installApk(context, destination)
                                    } else {
                                        downloadError = "Restore / Download failed for v${selectedVersion.latestVersionName}."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            enabled = selectedVersion.downloadUrl.isNotBlank()
                        ) {
                            Text(
                                text = when (language) {
                                    "ur" -> "بحال / اپ ڈیٹ کریں"
                                    "hi" -> "रीस्टोर / अपडेट करें"
                                    else -> "Restore v${selectedVersion.latestVersionName}"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
