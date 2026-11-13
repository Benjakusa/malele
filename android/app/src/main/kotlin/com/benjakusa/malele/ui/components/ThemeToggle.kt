package com.benjakusa.malele.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.benjakusa.malele.ui.theme.LocalThemeState

@Composable
fun ThemeToggleButton(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground,
) {
    val themeState = LocalThemeState.current
    IconButton(
        onClick = { themeState.toggle() },
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (themeState.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = "Toggle theme",
            tint = tint,
        )
    }
}
