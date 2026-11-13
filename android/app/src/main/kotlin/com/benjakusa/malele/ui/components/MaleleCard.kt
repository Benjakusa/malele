package com.benjakusa.malele.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MaleleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val shape = RoundedCornerShape(20.dp)

    // Prominent, highly visible shadows in light mode (dark shadow) and dark mode (soft white glow)
    val shadowColor = if (isDark) Color.White else Color.Black
    val shadowAlpha = if (isDark) 0.25f else 0.20f
    val elevation = if (isDark) 12.dp else 10.dp

    val shadowModifier = modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = shadowColor.copy(alpha = shadowAlpha),
        spotColor = shadowColor.copy(alpha = shadowAlpha * 1.5f),
    )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = shadowModifier,
            shape = shape,
            color = surfaceColor,
            content = content,
        )
    } else {
        Surface(
            modifier = shadowModifier,
            shape = shape,
            color = surfaceColor,
            content = content,
        )
    }
}
