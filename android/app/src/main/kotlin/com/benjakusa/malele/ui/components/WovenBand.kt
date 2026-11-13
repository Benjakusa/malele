package com.benjakusa.malele.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.benjakusa.malele.ui.theme.PureBlack
import com.benjakusa.malele.ui.theme.PureRed
import com.benjakusa.malele.ui.theme.PureWhite
import com.benjakusa.malele.ui.theme.WovenYellow

/**
 * The signature Malele woven band — African textile zigzag pattern.
 * Yellow + white interlocking zigzags on black, red dots at valleys,
 * separated by red stripes. Repeats 3 rows vertically.
 *
 * Width is full-screen, height defaults to 60dp (matches mockup).
 */
@Composable
fun WovenBand(
    modifier: Modifier = Modifier,
    height: Dp = 60.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        drawWovenBand(size.width, size.height)
    }
}

private fun DrawScope.drawWovenBand(width: Float, height: Float) {
    val bandsPerRow = 3
    val rowHeight = height / bandsPerRow
    val zigHeight = rowHeight * 0.55f
    val stripeHeight = rowHeight * 0.12f
    val dotRadius = rowHeight * 0.08f

    // Horizontal period — how wide each zigzag tooth is
    val periodPx = 48.dp.toPx()
    val teethPerWidth = (width / periodPx).toInt() + 2
    val actualPeriod = width / (teethPerWidth - 1)

    repeat(bandsPerRow) { rowIndex ->
        val topOfRow = rowIndex * rowHeight

        // Black background for this row
        drawRect(
            color = PureBlack,
            topLeft = Offset(0f, topOfRow),
            size = Size(width, rowHeight),
        )

        // Top red stripe
        drawRect(
            color = PureRed,
            topLeft = Offset(0f, topOfRow),
            size = Size(width, stripeHeight),
        )

        // Bottom red stripe
        drawRect(
            color = PureRed,
            topLeft = Offset(0f, topOfRow + rowHeight - stripeHeight),
            size = Size(width, stripeHeight),
        )

        // Zigzags area
        val zigTop = topOfRow + stripeHeight
        val zigBottom = zigTop + zigHeight

        // Yellow zigzag (upper)
        drawZigzag(
            color = WovenYellow,
            startX = -actualPeriod / 2,
            endX = width + actualPeriod / 2,
            topY = zigTop,
            bottomY = zigTop + zigHeight / 2,
            periodPx = actualPeriod,
            strokeWidth = 4.dp.toPx(),
            offsetPhases = 0,
        )

        // White zigzag (lower) — offset by half a period
        drawZigzag(
            color = PureWhite,
            startX = -actualPeriod,
            endX = width + actualPeriod,
            topY = zigTop + zigHeight / 2,
            bottomY = zigBottom,
            periodPx = actualPeriod,
            strokeWidth = 4.dp.toPx(),
            offsetPhases = 0,
        )

        // Red dots at valleys of yellow zigzag (top zigzag)
        val dotY = zigTop + zigHeight / 4
        var x = 0f
        while (x < width) {
            drawCircle(
                color = PureRed,
                radius = dotRadius,
                center = Offset(x, dotY),
            )
            x += actualPeriod
        }

        // Red dots at peaks of white zigzag (bottom zigzag)
        val dot2Y = zigTop + (zigHeight * 3) / 4
        x = actualPeriod / 2
        while (x < width) {
            drawCircle(
                color = PureRed,
                radius = dotRadius,
                center = Offset(x, dot2Y),
            )
            x += actualPeriod
        }
    }
}

private fun DrawScope.drawZigzag(
    color: Color,
    startX: Float,
    endX: Float,
    topY: Float,
    bottomY: Float,
    periodPx: Float,
    strokeWidth: Float,
    offsetPhases: Int,
) {
    val path = Path()
    val halfPeriod = periodPx / 2
    var x = startX
    var goUp = true
    var first = true
    while (x <= endX) {
        val y = if (goUp) topY else bottomY
        if (first) {
            path.moveTo(x, y)
            first = false
        } else {
            path.lineTo(x, y)
        }
        x += halfPeriod
        goUp = !goUp
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
        ),
    )
}
