package com.please.stop.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Earth-tone brand tokens ──
internal val Sage = Color(0xFF6B8F71)        // primary — muted sage green
internal val Clay = Color(0xFFA07855)         // secondary — warm clay
internal val Wheat = Color(0xFFC4A265)        // tertiary — soft gold

// ── Light Color Scheme ──
internal val LightColorScheme = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EADA),
    onPrimaryContainer = Color(0xFF1B3620),
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEDDD0),
    onSecondaryContainer = Color(0xFF3A2512),
    tertiary = Color(0xFFA08845),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E4C4),
    onTertiaryContainer = Color(0xFF3A2E0E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFFAF8F5),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFE8E2DA),
    onSurfaceVariant = Color(0xFF4A4640),
    surfaceContainerLowest = Color(0xFFFFFEFC),
    surfaceContainerLow = Color(0xFFF5F2ED),
    surfaceContainer = Color(0xFFEFECE7),
    surfaceContainerHigh = Color(0xFFE9E6E1),
    surfaceContainerHighest = Color(0xFFE3E0DB),
    outline = Color(0xFF7D7770),
    outlineVariant = Color(0xFFCDC6BE),
    inverseSurface = Color(0xFF31302D),
    inverseOnSurface = Color(0xFFF3F0EB),
    inversePrimary = Color(0xFF9CC6A1),
)

// ── Dark Color Scheme ──
internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CC6A1),              // lighter sage for dark bg
    onPrimary = Color(0xFF1B3620),
    primaryContainer = Color(0xFF3A5C3F),
    onPrimaryContainer = Color(0xFFD6EADA),
    secondary = Color(0xFFD4A87A),            // lighter clay for dark bg
    onSecondary = Color(0xFF3A2512),
    secondaryContainer = Color(0xFF6B5138),
    onSecondaryContainer = Color(0xFFEEDDD0),
    tertiary = Wheat,
    onTertiary = Color(0xFF3A2E0E),
    tertiaryContainer = Color(0xFF6B5A2C),
    onTertiaryContainer = Color(0xFFF0E4C4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF1C1B18),
    onSurface = Color(0xFFE8E2DA),
    surfaceVariant = Color(0xFF31302D),
    onSurfaceVariant = Color(0xFFCDC6BE),
    surfaceContainerLowest = Color(0xFF111110),
    surfaceContainerLow = Color(0xFF1C1B18),
    surfaceContainer = Color(0xFF252420),
    surfaceContainerHigh = Color(0xFF302F2B),
    surfaceContainerHighest = Color(0xFF3B3935),
    outline = Color(0xFF979089),
    outlineVariant = Color(0xFF4A4640),
    inverseSurface = Color(0xFFE8E2DA),
    inverseOnSurface = Color(0xFF31302D),
    inversePrimary = Color(0xFF4A7A50),
)
