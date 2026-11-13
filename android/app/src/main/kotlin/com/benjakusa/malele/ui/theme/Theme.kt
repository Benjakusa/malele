package com.benjakusa.malele.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = PureRed,
    onPrimary = PureWhite,
    secondary = WovenYellow,
    onSecondary = PureBlack,
    tertiary = PureRed,
    onTertiary = PureWhite,
    background = LightBackground,
    onBackground = OnLight,
    surface = LightSurface,
    onSurface = OnLight,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = OnLight,
    error = PureRed,
    onError = PureWhite,
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFEEEEEE),
)

private val DarkColors = darkColorScheme(
    primary = PureRed,
    onPrimary = PureWhite,
    secondary = WovenYellow,
    onSecondary = PureBlack,
    tertiary = PureRed,
    onTertiary = PureWhite,
    background = DarkBackground,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = OnDark,
    error = PureRed,
    onError = PureWhite,
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF2A2A2A),
)

@Composable
fun MaleleTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MaleleTypography,
        content = content,
    )
}
