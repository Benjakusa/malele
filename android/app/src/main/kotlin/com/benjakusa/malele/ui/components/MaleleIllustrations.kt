package com.benjakusa.malele.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.benjakusa.malele.ui.theme.PureBlack
import com.benjakusa.malele.ui.theme.PureRed
import com.benjakusa.malele.ui.theme.PureWhite
import com.benjakusa.malele.ui.theme.WovenYellow

/**
 * Vinyl record illustration — concentric rings with dotted middle bands.
 * Used on Library screen empty state and song cards.
 */
@Composable
fun VinylRecord(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val r = this.size.minDimension / 2

        // Black disc
        drawCircle(color = PureBlack, radius = r, center = Offset(cx, cy))

        // Outer red ring
        drawCircle(
            color = PureRed,
            radius = r * 0.92f,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.08f),
        )

        // Yellow dashed ring
        drawDottedRing(center = Offset(cx, cy), radius = r * 0.72f, color = WovenYellow, dotSize = r * 0.10f, count = 20)

        // Red inner ring
        drawCircle(
            color = PureRed,
            radius = r * 0.52f,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.06f),
        )

        // Yellow center dot
        drawCircle(color = WovenYellow, radius = r * 0.18f, center = Offset(cx, cy))

        // Center hole
        drawCircle(color = PureBlack, radius = r * 0.08f, center = Offset(cx, cy))
    }
}

/**
 * Explore radar — concentric rings with dots, red/yellow/white on black.
 */
@Composable
fun ExploreRadar(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val r = this.size.minDimension / 2

        // Outer white ring
        drawCircle(
            color = PureWhite,
            radius = r * 0.98f,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.03f),
        )

        // Red ring
        drawCircle(
            color = PureRed,
            radius = r * 0.82f,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.06f),
        )

        // Yellow dashed ring
        drawDottedRing(center = Offset(cx, cy), radius = r * 0.60f, color = WovenYellow, dotSize = r * 0.07f, count = 24)

        // White dotted ring
        drawDottedRing(center = Offset(cx, cy), radius = r * 0.40f, color = PureWhite, dotSize = r * 0.05f, count = 20)

        // Yellow center
        drawCircle(color = WovenYellow, radius = r * 0.12f, center = Offset(cx, cy))

        // Floating dots (like radar pings)
        drawCircle(color = PureRed, radius = r * 0.06f, center = Offset(cx + r * 0.75f, cy - r * 0.55f))
        drawCircle(color = WovenYellow, radius = r * 0.05f, center = Offset(cx - r * 0.60f, cy + r * 0.60f))
    }
}

/**
 * Woven ring avatar — circular avatar frame with woven band pattern.
 */
@Composable
fun WovenRingAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp,
) {
    val size = sizeDp
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val outerR = this.size.minDimension / 2
        val bandThickness = outerR * 0.25f
        val innerR = outerR - bandThickness

        // Outer red circle
        drawCircle(color = PureRed, radius = outerR, center = Offset(cx, cy))

        // Black ring
        drawCircle(color = PureBlack, radius = outerR - bandThickness * 0.15f, center = Offset(cx, cy))

        // Yellow zigzags around the ring
        val steps = 30
        val path = Path()
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * 2 * Math.PI.toFloat() - Math.PI.toFloat() / 2
            val radiusAtPoint = innerR + bandThickness * (if (i % 2 == 0) 0.85f else 0.15f)
            val px = cx + radiusAtPoint * kotlin.math.cos(angle)
            val py = cy + radiusAtPoint * kotlin.math.sin(angle)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path = path, color = WovenYellow, style = Stroke(width = bandThickness * 0.35f))

        // White zigzag overlay (offset)
        val path2 = Path()
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * 2 * Math.PI.toFloat() - Math.PI.toFloat() / 2
            val radiusAtPoint = innerR + bandThickness * (if (i % 2 == 0) 0.15f else 0.85f)
            val px = cx + radiusAtPoint * kotlin.math.cos(angle)
            val py = cy + radiusAtPoint * kotlin.math.sin(angle)
            if (i == 0) path2.moveTo(px, py) else path2.lineTo(px, py)
        }
        drawPath(path = path2, color = PureWhite, style = Stroke(width = bandThickness * 0.20f))

        // Inner white/black fill
        drawCircle(color = PureWhite, radius = innerR * 0.85f, center = Offset(cx, cy))
        // Simple person silhouette
        drawCircle(color = PureBlack, radius = innerR * 0.18f, center = Offset(cx, cy - innerR * 0.15f))
        drawArc(
            color = PureBlack,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - innerR * 0.35f, cy + innerR * 0.05f),
            size = Size(innerR * 0.70f, innerR * 0.55f),
            style = Stroke(width = innerR * 0.10f),
        )
    }
}

/**
 * Thin woven separator strip — 8dp tall repeating zigzag pattern.
 */
@Composable
fun ThinWovenStrip(
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val w = size.width
        val h = size.height
        drawRect(color = PureBlack, size = Size(w, h))

        val path = Path()
        val period = 24.dp.toPx()
        val half = period / 2
        var x = 0f
        var up = true
        var first = true
        while (x <= w) {
            val y = if (up) 0f else h
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            x += half
            up = !up
        }
        drawPath(path = path, color = WovenYellow, style = Stroke(width = 2.dp.toPx()))
    }
}

// ── Internal helpers ──

private fun DrawScope.drawDottedRing(
    center: Offset,
    radius: Float,
    color: Color,
    dotSize: Float,
    count: Int,
) {
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 2 * Math.PI.toFloat()
        val x = center.x + radius * kotlin.math.cos(angle)
        val y = center.y + radius * kotlin.math.sin(angle)
        drawCircle(color = color, radius = dotSize, center = Offset(x, y))
    }
}

/**
 * Slanted woven hero — the big slanted band at the top of auth screens,
 * like the login mockup. Band is skewed at the bottom with a wavy edge.
 */
@Composable
fun SlantedWovenHero(
    modifier: Modifier = Modifier,
    height: Dp = 280.dp,
    isLogin: Boolean = true,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val w = size.width
        val h = size.height
        val bottomEdge = h * 0.85f
        val slantHeight = h * 0.15f

        // 6 woven rows (yellow zig / black / white zig / black / yellow zig / red stripe)
        val rowH = bottomEdge / 6f
        repeat(6) { i ->
            val top = i * rowH
            when (i % 3) {
                0 -> drawRect(color = PureBlack, topLeft = Offset(0f, top), size = Size(w, rowH))
                1 -> drawRect(color = PureBlack, topLeft = Offset(0f, top), size = Size(w, rowH))
                2 -> drawRect(color = PureRed, topLeft = Offset(0f, top), size = Size(w, rowH))
            }
        }

        // Yellow zigzag over the middle rows
        drawZigzagBand(top = 0f, bottom = rowH * 2, width = w, color = WovenYellow)
        // White zigzag over the next band
        drawZigzagBand(top = rowH * 2, bottom = rowH * 4, width = w, color = PureWhite)
        // Yellow zigzag again
        drawZigzagBand(top = rowH * 4, bottom = rowH * 6, width = w, color = WovenYellow)

        // Wavy bottom edge (slanted)
        val wavePath = Path()
        wavePath.moveTo(0f, bottomEdge)
        var x = 0f
        val period = 80.dp.toPx()
        while (x <= w) {
            val amp = if ((x / period).toInt() % 2 == 0) slantHeight else 0f
            wavePath.lineTo(x, bottomEdge + amp)
            x += period / 2
        }
        wavePath.lineTo(w, bottomEdge)
        wavePath.lineTo(w, h)
        wavePath.lineTo(0f, h)
        wavePath.close()
        drawPath(wavePath, color = PureBlack)
    }
}

private fun DrawScope.drawZigzagBand(top: Float, bottom: Float, width: Float, color: Color) {
    val path = Path()
    val period = 32.dp.toPx()
    val half = period / 2
    var x = 0f
    var up = true
    var first = true
    while (x <= width) {
        val y = if (up) top else bottom
        if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        x += half
        up = !up
    }
    drawPath(path = path, color = color, style = Stroke(width = 6.dp.toPx()))
}

/**
 * Woven progress bar — animated zigzag stripe inside a rounded track.
 */
@Composable
fun WovenProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val w = size.width
        val h = size.height
        val corner = h / 2

        // Track background
        drawRoundRect(
            color = PureBlack.copy(alpha = 0.15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
        )

        // Progress fill
        val fillW = w * progress.coerceIn(0f, 1f)
        if (fillW > 0f) {
            drawRoundRect(
                color = PureRed,
                size = Size(fillW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            )
            // Zigzag overlay on filled portion
            val path = Path()
            val period = 12.dp.toPx()
            val half = period / 2
            var x = 0f
            var up = true
            var first = true
            while (x <= fillW) {
                val y = if (up) h * 0.15f else h * 0.85f
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                x += half
                up = !up
            }
            clipRect(right = fillW) {
                drawPath(path = path, color = WovenYellow, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}
