package com.benjakusa.malele.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.ui.theme.PureRed
import com.benjakusa.malele.ui.theme.WovenYellow
import kotlinx.coroutines.delay

private val PlatterDark = Color(0xFF101010)
private val PlatterMid = Color(0xFF292929)
private val PlatterInner = Color(0xFF111111)
private val PlatterCore = Color(0xFF252525)
private val SpindleColor = Color(0xFFE8E1D2)
private val TonearmLight = Color(0xFFFFF54D)
private val TonearmDark = Color(0xFFFFD700)
private val ArmBaseRing = Color(0xFF29231C)
private val ArmBaseInner = Color(0xFFC40000)
private val LabelInnerBg = Color(0xFF222222)
private val PatternGreen = Color(0xFF315C42)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val progress = remember { Animatable(0f) }

    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
        ),
        label = "spinAngle",
    )

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 5000, easing = LinearEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VinylScene(spinAngle = spin)

            Spacer(Modifier.height(18.dp))
            Text(
                text = "MALELE",
                color = WovenYellow,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 10.sp,
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "SETTING THE RHYTHM",
                color = onBg,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Preparing your musical experience...",
                color = onBg.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            AnimatedBars(color = PureRed)

            Spacer(Modifier.height(16.dp))
            ProgressBar(
                progress = progress.value,
                trackColor = onBg.copy(alpha = 0.15f),
                fillColor = PureRed,
            )
        }
    }
}


@Composable
private fun VinylScene(spinAngle: Float) {
    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(264.dp)) {
            rotate(degrees = spinAngle, pivot = center) {
                drawPlatter()
                drawGrooves()
                drawLabel()
                drawLabelInner()
            }
        }
        Canvas(modifier = Modifier.size(300.dp)) {
            drawTonearm()
        }
    }
}

private fun DrawScope.drawPlatter() {
    val r = size.minDimension / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to PlatterCore,
                0.57f to PlatterCore,
                0.58f to PlatterInner,
                0.63f to PlatterInner,
                0.64f to PlatterMid,
                0.66f to PlatterMid,
                0.67f to PlatterDark,
                1.00f to PlatterDark,
            ),
            center = center,
            radius = r,
        ),
        radius = r,
    )
    drawCircle(
        color = Color(0xFF3A1A1A),
        radius = r + 3.5.dp.toPx(),
        style = Stroke(width = 7.dp.toPx()),
    )
    drawCircle(
        color = Color(0xFF171717),
        radius = r + 5.dp.toPx(),
        style = Stroke(width = 3.dp.toPx()),
    )
}

private fun DrawScope.drawGrooves() {
    val maxR = size.minDimension / 2f
    val innerR = 45.5.dp.toPx()
    val spacing = 6.5.dp.toPx()
    var r = innerR
    while (r < maxR - 2.dp.toPx()) {
        drawCircle(
            color = Color(0x44777777),
            radius = r,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = Color(0xFF080808),
            radius = r + 1.dp.toPx(),
            style = Stroke(width = 1.dp.toPx()),
        )
        r += spacing
    }
}

private fun DrawScope.drawLabel() {
    val labelR = 45.5.dp.toPx()

    // Outer red circle
    drawCircle(color = PureRed, radius = labelR)

    // Conic-style wedges inside (approximated with arcs)
    val wedgeCount = 30
    val wedgeAngle = 360f / wedgeCount
    val innerR = labelR - 5.dp.toPx()
    for (i in 0 until wedgeCount) {
        val startAngle = i * wedgeAngle
        val color = when (i % 6) {
            0, 1, 2, 3 -> PureRed
            4 -> WovenYellow
            else -> PatternGreen
        }
        drawArc(
            color = color,
            startAngle = startAngle - 90f,
            sweepAngle = wedgeAngle - 0.5f,
            useCenter = true,
            topLeft = Offset(center.x - innerR, center.y - innerR),
            size = Size(innerR * 2f, innerR * 2f),
        )
    }

    // Yellow outer border
    drawCircle(
        color = WovenYellow,
        radius = labelR,
        style = Stroke(width = 3.dp.toPx()),
    )

    // Dashed yellow inner ring
    drawCircle(
        color = TonearmLight,
        radius = labelR - 3.dp.toPx(),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f)),
        ),
    )
}

private fun DrawScope.drawLabelInner() {
    val innerR = 24.5.dp.toPx()

    // Dark inner circle (label-inner)
    drawCircle(color = LabelInnerBg, radius = innerR)

    // Yellow-light border
    drawCircle(
        color = TonearmLight,
        radius = innerR,
        style = Stroke(width = 2.dp.toPx()),
    )

    // Spindle dot
    drawCircle(color = SpindleColor, radius = 3.5.dp.toPx())
}

private fun DrawScope.drawTonearm() {
    val pivotX = size.width - 18.dp.toPx()
    val pivotY = 24.5.dp.toPx()

    // Tonearm bar (110dp long, 8dp thick, rotated -44deg)
    rotate(degrees = -44f, pivot = Offset(pivotX, pivotY)) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(TonearmLight, TonearmDark, WovenYellow),
                start = Offset(pivotX - 110.dp.toPx(), pivotY),
                end = Offset(pivotX, pivotY),
            ),
            topLeft = Offset(pivotX - 110.dp.toPx(), pivotY - 4.dp.toPx()),
            size = Size(110.dp.toPx(), 8.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )

        // Needle head at far end
        drawRoundRect(
            color = TonearmLight,
            topLeft = Offset(pivotX - 113.dp.toPx(), pivotY - 5.dp.toPx()),
            size = Size(15.dp.toPx(), 10.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )

        // Red needle tip
        drawCircle(
            color = PureRed,
            radius = 2.dp.toPx(),
            center = Offset(pivotX - 111.dp.toPx(), pivotY),
        )
    }

    // Pivot base
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(TonearmLight, TonearmDark),
            start = Offset(pivotX - 16.dp.toPx(), pivotY - 16.dp.toPx()),
            end = Offset(pivotX + 16.dp.toPx(), pivotY + 16.dp.toPx()),
        ),
        radius = 16.5.dp.toPx(),
        center = Offset(pivotX, pivotY),
    )

    // Dark ring around pivot
    drawCircle(
        color = ArmBaseRing,
        radius = 16.5.dp.toPx(),
        center = Offset(pivotX, pivotY),
        style = Stroke(width = 4.dp.toPx()),
    )

    // Red center dot on pivot
    drawCircle(
        color = ArmBaseInner,
        radius = 9.5.dp.toPx(),
        center = Offset(pivotX, pivotY),
    )
}

@Composable
private fun AnimatedBars(color: Color) {
    val infinite = rememberInfiniteTransition(label = "bars")
    val heights = (0 until 10).map { i ->
        val delayMs = (i % 3) * 200
        infinite.animateFloat(
            initialValue = 6f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, delayMillis = delayMs),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "bar$i",
        )
    }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier.height(22.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.value.dp)
                    .background(color, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    trackColor: Color,
    fillColor: Color,
) {
    Canvas(
        modifier = Modifier
            .width(190.dp)
            .height(3.dp),
    ) {
        val h = size.height
        val w = size.width
        val r = h / 2f
        drawRoundRect(
            color = trackColor,
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
        drawRoundRect(
            color = fillColor,
            size = Size(w * progress.coerceIn(0f, 1f), h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
    }
}
