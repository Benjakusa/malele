package com.benjakusa.malele.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class MaleleThemeState(initialDark: Boolean = false) {
    var isDark by mutableStateOf(initialDark)
        private set

    fun toggle() {
        isDark = !isDark
    }

    fun set(dark: Boolean) {
        isDark = dark
    }
}

@Composable
fun rememberMaleleThemeState(): MaleleThemeState {
    return remember { MaleleThemeState(initialDark = false) }
}

val LocalThemeState = staticCompositionLocalOf<MaleleThemeState> {
    error("No ThemeState provided")
}
