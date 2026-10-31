package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.delay

val GraenMetalFont = FontFamily(Font(R.font.graen_metal))

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scaleAnim"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(3500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F382E), Color(0xFF07241E), Color(0xFF00221A)),
                    radius = 1200f
                )
            )
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .scale(scaleAnim)
        ) {
            AzanLogoCard(
                size = 140.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            AzanTimeWordmark(
                height = 54.dp,
                topPadding = 12.dp,
                bottomPadding = 8.dp
            )
        }
    }
}
