package com.please.stop.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Cool blue brand tokens ──
internal val Azure = Color(0xFF3B82F6)
internal val Cyan = Color(0xFF14B8A6)
internal val Indigo = Color(0xFF6366F1)

// ── Light Color Scheme ──
internal val LightColorScheme = lightColorScheme(
    primary = Azure,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF0F2A52),
    secondary = Cyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F6F1),
    onSecondaryContainer = Color(0xFF073A34),
    tertiary = Indigo,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3E3FF),
    onTertiaryContainer = Color(0xFF221E63),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFF6F9FF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE7EEF8),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FBFF),
    surfaceContainer = Color(0xFFF0F5FD),
    surfaceContainerHigh = Color(0xFFEAF0FA),
    surfaceContainerHighest = Color(0xFFE2EAF6),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFE2E8F0),
    inversePrimary = Color(0xFF93C5FD),
)

// ── Dark Color Scheme ──
internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF0F2A52),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDCEBFF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF073A34),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFD3F6F1),
    tertiary = Color(0xFFA5B4FC),
    onTertiary = Color(0xFF221E63),
    tertiaryContainer = Color(0xFF4338CA),
    onTertiaryContainer = Color(0xFFE3E3FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF172033),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF060B14),
    surfaceContainerLow = Color(0xFF0B1220),
    surfaceContainer = Color(0xFF111827),
    surfaceContainerHigh = Color(0xFF172033),
    surfaceContainerHighest = Color(0xFF1E293B),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF1E293B),
    inversePrimary = Azure,
)
