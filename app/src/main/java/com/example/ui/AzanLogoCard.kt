package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Standardized Squircle App Logo Card matching Support Screen (`image_0af060ef.png`)
 * Features:
 * 1. Squircle container (RoundedCornerShape)
 * 2. Deep emerald green gradient background (Color(0xFF0E2F23) to Color(0xFF061A13))
 * 3. Muted gold border (Color(0xFFE5B842))
 * 4. Animated 45-degree sweeping shimmer / shiny effect across the card
 * 5. Perfectly centered `R.drawable.foreground` logo with consistent padding
 */
@Composable
fun AzanLogoCard(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    cornerRadius: Dp = (size.value * 0.28f).dp,
    logoPadding: Dp = (size.value * 0.18f).dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo45Shimmer")

    // Continuous slow 45-degree shiny shimmer sweep (8000ms cycle for a gentle, luxurious glide)
    val shimmerPos by infiniteTransition.animateFloat(
        initialValue = -350f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPos"
    )

    // 45-degree diagonal warm golden sheen brush (darker/warmer, no stark white glow)
    val shimmer45Brush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x0FB38938), // Muted dark gold ambient
            Color(0x40F3DE8E), // Soft warm gold sheen center
            Color(0x0FB38938), // Muted dark gold ambient
            Color.Transparent
        ),
        start = Offset(shimmerPos - 180f, shimmerPos - 180f),
        end = Offset(shimmerPos + 180f, shimmerPos + 180f)
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E2F23), Color(0xFF061A13))
                )
            )
            .border(
                BorderStroke(1.2.dp, Color(0xFFE5B842).copy(alpha = 0.65f)),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawWithContent {
                drawContent()
                // Overlay 45-degree shiny shimmer beam
                drawRect(brush = shimmer45Brush)
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.foreground),
            contentDescription = "Azan App Logo",
            modifier = Modifier
                .fillMaxSize()
                .padding(logoPadding),
            contentScale = ContentScale.Fit
        )
    }
}
