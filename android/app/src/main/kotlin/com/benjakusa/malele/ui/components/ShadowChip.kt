package com.benjakusa.malele.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.ui.theme.AfricanRed

/**
 * A rounded pill chip that uses a soft shadow instead of a border line.
 * Selected = AfricanRed fill + white content, elevated shadow.
 * Idle = surfaceVariant fill, subtle shadow.
 */
@Composable
fun ShadowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50.dp)
    val surfaceColor = if (selected) AfricanRed else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val elevation = if (selected) 8.dp else 4.dp
    val shadowColor = if (selected) AfricanRed.copy(alpha = 0.35f)
                      else Color.Black.copy(alpha = 0.15f)

    Surface(
        onClick = onClick,
        shape = shape,
        color = surfaceColor,
        contentColor = contentColor,
        modifier = modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(text = label, color = contentColor, fontSize = 14.sp)
        }
    }
}
