package com.example.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.hypot

/**
 * Tap-Origin Liquid Portal Reveal
 * Balanced 1150ms cubic-bezier(0.32, 0.08, 0.22, 1) transition:
 * - Originates from the outer circle (54.dp) of the tapped navigation item (e.g., Home, Setting, Ramazan)
 * - Expands continuously from the tab's circle button radius right across the farthest screen corner (103%)
 * - Incoming screen: scale from 1.04 down to 1.0, clipped to the expanding circle
 * - Outgoing screen: scale from 1.0 down to 0.95, blurred 4px, dimmed to 0.7 brightness
 */
const val PORTAL_REVEAL_DURATION_MS = 1150L
val PortalRevealEasing = CubicBezierEasing(0.32f, 0.08f, 0.22f, 1.0f)

class CircularPortalShape(
    private val progress: Float,
    private val originX: Float,
    private val originY: Float,
    private val startRadius: Float,
    private val maxExpansion: Float = 1.03f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        if (p >= 0.999f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }

        val cx = originX
        val cy = originY

        val d1 = hypot(cx, cy)
        val d2 = hypot(size.width - cx, cy)
        val d3 = hypot(cx, size.height - cy)
        val d4 = hypot(size.width - cx, size.height - cy)
        val maxDist = maxOf(d1, d2, d3, d4).coerceAtLeast(100f)
        val endRadius = maxDist * maxExpansion

        val currentRadius = startRadius + (endRadius - startRadius) * p

        val path = Path().apply {
            addOval(
                Rect(
                    left = cx - currentRadius,
                    top = cy - currentRadius,
                    right = cx + currentRadius,
                    bottom = cy + currentRadius
                )
            )
        }
        return Outline.Generic(path)
    }
}
