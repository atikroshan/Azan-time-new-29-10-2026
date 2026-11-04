package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig

@Composable
fun AiStudioLoadingFooter(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiStudioShimmerSwipe")
    
    // Slow, elegant linear shimmer swipe across the text (7500ms cycle)
    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslateX"
    )

    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFB38938), // Muted gold
            Color(0xFFF3DE8E), // Bright golden shimmer highlight
            Color(0xFFFFFFFF), // Pure white shine center
            Color(0xFFF3DE8E), // Bright golden shimmer highlight
            Color(0xFFB38938)  // Muted gold
        ),
        startX = shimmerTranslateX - 250f,
        endX = shimmerTranslateX + 250f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "v${BuildConfig.VERSION_NAME} • Powered by @tek",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(brush = shimmerBrush),
            textAlign = TextAlign.Center,
            letterSpacing = 1.0.sp
        )
    }
}
