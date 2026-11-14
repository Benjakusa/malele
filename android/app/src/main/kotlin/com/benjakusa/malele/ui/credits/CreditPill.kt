package com.benjakusa.malele.ui.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.ui.theme.AfricanRed
import com.benjakusa.malele.ui.theme.WovenYellow

/**
 * Compact credit balance pill with a + button.
 * Shows available credits; tapping anywhere opens the top-up sheet.
 */
@Composable
fun CreditPill(
    available: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isLoading) "…" else available.toString()
    val lowCredit = !isLoading && available <= 0
    val bg = if (lowCredit) AfricanRed else WovenYellow
    val fg = if (lowCredit) Color.White else Color.Black

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = bg,
        contentColor = fg,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                ),
            )
            Text(
                text = if (lowCredit) "Top up" else "credits",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    color = fg.copy(alpha = 0.85f),
                ),
            )
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = if (lowCredit) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f),
                modifier = Modifier.size(22.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Top up credits",
                    tint = fg,
                    modifier = Modifier.padding(3.dp),
                )
            }
        }
    }
}
