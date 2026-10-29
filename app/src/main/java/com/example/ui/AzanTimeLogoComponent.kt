package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Reusable Azan Time Vector Logo with:
 * 1. "AZAN TIME" wordmark in metallic gold gradient (CorelDRAW exact vector path)
 * 2. "AAO ALLAH KI RAAH MEIN CHALE" punchline in pure crisp white (#FFFFFF)
 * Includes built-in top and bottom padding so it never mixes up or touches surrounding elements.
 */
@Composable
fun AzanTimeWordmark(
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    topPadding: Dp = 8.dp,
    bottomPadding: Dp = 6.dp
) {
    Box(
        modifier = modifier.padding(top = topPadding, bottom = bottomPadding),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_azan_time_logo),
            contentDescription = "Azan Time - AAO ALLAH KI RAAH MEIN CHALE",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(height)
        )
    }
}
