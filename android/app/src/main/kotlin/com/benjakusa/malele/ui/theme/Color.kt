package com.benjakusa.malele.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core palette (light mode default, from redesign v2) ──
val PureBlack = Color(0xFF0A0A0A)
val PureWhite = Color(0xFFFFFFFF)
val PureRed = Color(0xFFFF0000)
val WovenYellow = Color(0xFFFFD000)

// ── Dark mode ──
val DarkBackground = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF161616)
val DarkSurfaceHigh = Color(0xFF232323)
val OnDark = Color(0xFFFFFFFF)
val OnDarkMuted = Color(0xB3FFFFFF)  // 70% white

// ── Light mode ──
val LightBackground = Color(0xFFF4F4F4)  // Pale grey page for visible card shadows in light mode
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFEFEFEF)
val OnLight = Color(0xFF0A0A0A)
val OnLightMuted = Color(0xB3000000)  // 70% black

// ── Status badge colors ──
val BadgeMockBg = Color(0xFF2A2A2A)
val BadgeProcessingBg = Color(0xFFFF0000)
val BadgeCompletedBg = Color(0xFFFFD000)
val BadgeTextOnYellow = Color(0xFF0A0A0A)
val BadgeTextOnDark = Color(0xFFFFFFFF)

// ── Legacy aliases ──
val AfricanRed = PureRed
val BaseBlack = PureBlack
val BaseBackground = LightBackground

val OnDarkAlias = OnDark
val OnLightAlias = OnLight
