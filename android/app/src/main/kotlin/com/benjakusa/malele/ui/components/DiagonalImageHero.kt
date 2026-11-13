package com.benjakusa.malele.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.benjakusa.malele.R

/**
 * African image occupying the top of the auth screen, with a diagonal cut
 * at the bottom sloping UP toward the RIGHT (as per redesign v2 mockups).
 *
 * @param height fixed height (46% of screen computed by caller)
 * @param diagonalFraction how much the diagonal rises from left to right
 *                         (0.0 = flat, 1.0 = full height). Default 0.18f.
 */
@Composable
fun DiagonalImageHero(
    height: Dp,
    modifier: Modifier = Modifier,
    diagonalFraction: Float = 0.18f,
) {
    Image(
        painter = painterResource(id = R.drawable.african),
        contentDescription = "African pattern",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(DiagonalBottomShape(diagonalFraction)),
    )
}

/**
 * Shape that clips the bottom edge into a diagonal going UP toward the right.
 * The bottom-left corner stays at full height; the bottom-right corner is
 * raised by `diagonalFraction * totalHeight`.
 */
class DiagonalBottomShape(private val diagonalFraction: Float = 0.18f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val rise = h * diagonalFraction
        val path = Path().apply {
            moveTo(0f, 0f)           // top-left
            lineTo(w, 0f)            // top-right
            lineTo(w, h - rise)      // right side, raised by `rise`
            lineTo(0f, h)            // bottom-left, at full height
            close()
        }
        return Outline.Generic(path)
    }
}
