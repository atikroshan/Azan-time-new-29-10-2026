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
    logoPadding: Dp = (size.value * 0.18f).dp,
    animateShimmer: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo45Shimmer")

    val shimmerPos by if (animateShimmer) {
        infiniteTransition.animateFloat(
            initialValue = -350f,
            targetValue = 950f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerPos"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val shimmer45Brush = if (animateShimmer) {
        Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x0FB38938),
                Color(0x40F3DE8E),
                Color(0x0FB38938),
                Color.Transparent
            ),
            start = Offset(shimmerPos - 180f, shimmerPos - 180f),
            end = Offset(shimmerPos + 180f, shimmerPos + 180f)
        )
    } else null

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
            .then(
                if (shimmer45Brush != null) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(brush = shimmer45Brush)
                    }
                } else Modifier
            ),
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
